package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.data.transcoder.ContextTranscoder;
import com.example.di.AppContainer;
import com.example.domain.model.ContentPart;
import com.example.domain.model.Conversation;
import com.example.domain.model.ErrorDescriptor;
import com.example.domain.model.MediaAsset;
import com.example.domain.model.MediaJob;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.model.Role;
import com.example.domain.model.TokenUsage;
import com.example.domain.model.UnifiedMessage;
import com.example.domain.provider.ChatRequest;
import com.example.domain.provider.ChatStream;
import com.example.domain.provider.ImageRequest;
import com.example.domain.provider.LlmProvider;
import com.example.domain.provider.MediaTask;
import com.example.domain.provider.StreamListener;
import com.example.domain.provider.VideoRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatViewModel extends ViewModel {
    private final AppContainer container;

    private final MutableLiveData<Conversation> activeConversation = new MutableLiveData<>();
    private final MutableLiveData<ModelDescriptor> activeModel = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isStreaming = new MutableLiveData<>(false);
    private final MutableLiveData<String> streamingDelta = new MutableLiveData<>("");
    private final MutableLiveData<String> reasoningDelta = new MutableLiveData<>("");
    private final MutableLiveData<ErrorDescriptor> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<List<String>> degradationWarnings = new MutableLiveData<>(new ArrayList<>());

    private LiveData<List<UnifiedMessage>> currentMessages;
    private ChatStream activeStream;
    private long streamStartTime = 0;

    public ChatViewModel(@NonNull AppContainer container) {
        this.container = container;
    }

    public LiveData<Conversation> getActiveConversation() { return activeConversation; }
    public LiveData<ModelDescriptor> getActiveModel() { return activeModel; }
    public LiveData<Boolean> getIsStreaming() { return isStreaming; }
    public LiveData<String> getStreamingDelta() { return streamingDelta; }
    public LiveData<String> getReasoningDelta() { return reasoningDelta; }
    public LiveData<ErrorDescriptor> getErrorEvent() { return errorEvent; }
    public LiveData<List<String>> getDegradationWarnings() { return degradationWarnings; }

    public LiveData<List<UnifiedMessage>> loadConversation(String conversationId) {
        container.getExecutors().diskIO().execute(() -> {
            Conversation c = container.getConversationRepository().getConversationByIdSync(conversationId);
            if (c != null) {
                container.getExecutors().mainThread().execute(() -> {
                    activeConversation.setValue(c);
                    loadModel(c.currentModelId, c.currentProviderId);
                });
            }
        });
        currentMessages = container.getMessageRepository().getMessagesForConversation(conversationId);
        return currentMessages;
    }

    private void loadModel(String modelId, String providerId) {
        container.getExecutors().diskIO().execute(() -> {
            ModelDescriptor m = container.getModelRepository().getModelSync(modelId);
            if (m == null) {
                m = container.getDiscoveryEngine().discoverCapabilities(modelId, providerId, null);
            }
            final ModelDescriptor finalModel = m;
            container.getExecutors().mainThread().execute(() -> activeModel.setValue(finalModel));
        });
    }

    public void switchModel(ModelDescriptor newModel) {
        Conversation c = activeConversation.getValue();
        if (c == null || newModel == null) return;

        c.currentModelId = newModel.id;
        c.currentProviderId = newModel.providerId;
        container.getConversationRepository().updateConversation(c);
        activeConversation.setValue(c);
        activeModel.setValue(newModel);

        // Insert a subtle system divider message noting model switch
        UnifiedMessage sysMsg = new UnifiedMessage(c.id, Role.SYSTEM, "Switched model to " + newModel.getFriendlyName() + " (" + newModel.providerId + ")");
        sysMsg.sourceModelId = newModel.id;
        sysMsg.sourceProviderId = newModel.providerId;
        container.getMessageRepository().insertMessage(sysMsg);

        // Check for capability warnings
        validateModelCapabilities(newModel);
    }

    private void validateModelCapabilities(ModelDescriptor model) {
        List<String> warnings = new ArrayList<>();
        if (model.capabilities != null) {
            if (model.capabilities.visionInput.isFalse()) {
                warnings.add("Active model lacks vision. Images in context will be transcoded to text descriptions.");
            }
            if (model.capabilities.streaming.isFalse()) {
                warnings.add("Active model does not support streaming. Output will appear when finished.");
            }
        }
        degradationWarnings.postValue(warnings);
    }

    public void sendMessage(String text, List<ContentPart> attachments) {
        Conversation c = activeConversation.getValue();
        ModelDescriptor m = activeModel.getValue();
        if (c == null || m == null || isStreaming.getValue() == Boolean.TRUE) return;

        // Command detection (e.g. /image prompt or /video prompt)
        if (text.startsWith("/image ")) {
            String prompt = text.substring(7).trim();
            handleImageGeneration(prompt, c, m);
            return;
        } else if (text.startsWith("/video ")) {
            String prompt = text.substring(7).trim();
            handleVideoGeneration(prompt, c, m);
            return;
        }

        // Standard Chat Turn
        UnifiedMessage userMsg = new UnifiedMessage(c.id, Role.USER, text);
        if (attachments != null && !attachments.isEmpty()) {
            userMsg.parts.addAll(attachments);
        }
        userMsg.sourceModelId = m.id;
        userMsg.sourceProviderId = m.providerId;
        container.getMessageRepository().insertMessage(userMsg);

        // Prepare placeholder assistant message
        UnifiedMessage assistantMsg = new UnifiedMessage(c.id, Role.ASSISTANT, "");
        assistantMsg.sourceModelId = m.id;
        assistantMsg.sourceProviderId = m.providerId;
        assistantMsg.status = UnifiedMessage.Status.STREAMING;
        container.getMessageRepository().insertMessage(assistantMsg);

        executeStreamRequest(c, m, assistantMsg);
    }

    private void executeStreamRequest(Conversation c, ModelDescriptor m, UnifiedMessage assistantMsg) {
        container.getExecutors().diskIO().execute(() -> {
            ProviderConfig pConfig = container.getProviderRepository().getProviderSync(m.providerId);
            if (pConfig == null) {
                reportError(new ErrorDescriptor(400, ErrorDescriptor.Kind.INVALID_MODEL, "Provider '" + m.providerId + "' not configured.", false, 0, ""));
                return;
            }

            LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(pConfig);
            if (provider == null) {
                reportError(new ErrorDescriptor(400, ErrorDescriptor.Kind.INVALID_MODEL, "Could not initialize provider.", false, 0, ""));
                return;
            }

            List<UnifiedMessage> history = container.getMessageRepository().getMessagesForConversationSync(c.id);
            // Context Transcoder: adapts context to target model grammar and token budget
            ContextTranscoder.TranscodeResult transcoded = ContextTranscoder.transcodeForModel(history, c.systemPrompt, m);
            if (transcoded.warnings != null && !transcoded.warnings.isEmpty()) {
                degradationWarnings.postValue(transcoded.warnings);
            }

            ChatRequest req = new ChatRequest();
            req.modelId = m.id;
            req.messages = transcoded.messages;
            req.systemPrompt = transcoded.effectiveSystemPrompt;
            req.temperature = c.temperature;
            req.topP = c.topP;
            req.maxTokens = m.maxOutputTokens > 0 ? m.maxOutputTokens : 4096;

            container.getExecutors().mainThread().execute(() -> {
                isStreaming.setValue(true);
                streamingDelta.setValue("");
                reasoningDelta.setValue("");
            });

            streamStartTime = System.currentTimeMillis();
            StringBuilder contentAccumulator = new StringBuilder();
            StringBuilder reasoningAccumulator = new StringBuilder();

            activeStream = provider.openChatStream(req);
            activeStream.start(new StreamListener() {
                @Override
                public void onDelta(String textDelta) {
                    contentAccumulator.append(textDelta);
                    container.getExecutors().mainThread().execute(() -> {
                        streamingDelta.setValue(contentAccumulator.toString());
                    });
                }

                @Override
                public void onReasoningDelta(String reasoningChunk) {
                    reasoningAccumulator.append(reasoningChunk);
                    container.getExecutors().mainThread().execute(() -> {
                        reasoningDelta.setValue(reasoningAccumulator.toString());
                    });
                }

                @Override
                public void onComplete(TokenUsage usage) {
                    long latency = System.currentTimeMillis() - streamStartTime;
                    assistantMsg.setTextContent(contentAccumulator.toString());
                    if (reasoningAccumulator.length() > 0) {
                        assistantMsg.setReasoningContent(reasoningAccumulator.toString());
                    }
                    assistantMsg.status = UnifiedMessage.Status.SUCCESS;
                    assistantMsg.tokenUsage = usage;
                    assistantMsg.latencyMs = latency;
                    container.getMessageRepository().updateMessage(assistantMsg);

                    container.getExecutors().mainThread().execute(() -> {
                        isStreaming.setValue(false);
                    });
                }

                @Override
                public void onError(ErrorDescriptor error) {
                    assistantMsg.status = UnifiedMessage.Status.FAILED;
                    assistantMsg.error = error;
                    container.getMessageRepository().updateMessage(assistantMsg);

                    container.getExecutors().mainThread().execute(() -> {
                        isStreaming.setValue(false);
                        errorEvent.setValue(error);
                    });
                }
            });
        });
    }

    public void stopStreaming() {
        if (activeStream != null && !activeStream.isCancelled()) {
            activeStream.cancel();
        }
        isStreaming.setValue(false);
    }

    private void handleImageGeneration(String prompt, Conversation c, ModelDescriptor m) {
        UnifiedMessage userMsg = new UnifiedMessage(c.id, Role.USER, "/image " + prompt);
        container.getMessageRepository().insertMessage(userMsg);

        UnifiedMessage assistantMsg = new UnifiedMessage(c.id, Role.ASSISTANT, "Generating image for: \"" + prompt + "\"...");
        assistantMsg.status = UnifiedMessage.Status.STREAMING;
        container.getMessageRepository().insertMessage(assistantMsg);

        container.getExecutors().networkIO().execute(() -> {
            try {
                ProviderConfig pConfig = container.getProviderRepository().getProviderSync(m.providerId);
                LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(pConfig);
                ImageRequest imgReq = new ImageRequest(prompt);
                imgReq.modelId = m.capabilities != null && m.capabilities.imageGeneration.isTrue() ? m.id : "dall-e-3";
                MediaTask task = provider.generateImage(imgReq);

                if (task.isDirectUrl()) {
                    assistantMsg.status = UnifiedMessage.Status.SUCCESS;
                    assistantMsg.parts.clear();
                    assistantMsg.parts.add(ContentPart.image(task.directUrl, null));
                    container.getMessageRepository().updateMessage(assistantMsg);

                    MediaAsset asset = new MediaAsset();
                    asset.id = UUID.randomUUID().toString();
                    asset.conversationId = c.id;
                    asset.messageId = assistantMsg.id;
                    asset.type = "IMAGE";
                    asset.prompt = prompt;
                    asset.modelId = m.id;
                    asset.providerId = m.providerId;
                    asset.remoteUrl = task.directUrl;
                    container.getMediaRepository().insertMediaAsset(asset);
                }
            } catch (Exception e) {
                assistantMsg.status = UnifiedMessage.Status.FAILED;
                assistantMsg.setTextContent("Image generation failed: " + e.getMessage());
                container.getMessageRepository().updateMessage(assistantMsg);
            }
        });
    }

    private void handleVideoGeneration(String prompt, Conversation c, ModelDescriptor m) {
        UnifiedMessage userMsg = new UnifiedMessage(c.id, Role.USER, "/video " + prompt);
        container.getMessageRepository().insertMessage(userMsg);

        UnifiedMessage assistantMsg = new UnifiedMessage(c.id, Role.ASSISTANT, "Dispatching video generation job for: \"" + prompt + "\"...");
        assistantMsg.status = UnifiedMessage.Status.STREAMING;
        container.getMessageRepository().insertMessage(assistantMsg);

        container.getExecutors().networkIO().execute(() -> {
            try {
                ProviderConfig pConfig = container.getProviderRepository().getProviderSync(m.providerId);
                LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(pConfig);
                VideoRequest vidReq = new VideoRequest(prompt);
                vidReq.modelId = m.id;
                MediaTask task = provider.generateVideo(vidReq);

                if (task.isAsyncJob()) {
                    MediaJob job = new MediaJob(task.jobId, c.id, assistantMsg.id, m.id, m.providerId, prompt);
                    job.pollUrl = task.pollUrl;
                    container.getMediaRepository().insertMediaJob(job);
                    assistantMsg.status = UnifiedMessage.Status.SUCCESS;
                    assistantMsg.setTextContent("Video generation job submitted! (Job ID: " + task.jobId + "). Processing in background...");
                    container.getMessageRepository().updateMessage(assistantMsg);
                }
            } catch (Exception e) {
                assistantMsg.status = UnifiedMessage.Status.FAILED;
                assistantMsg.setTextContent("Video generation failed: " + e.getMessage());
                container.getMessageRepository().updateMessage(assistantMsg);
            }
        });
    }

    private void reportError(ErrorDescriptor err) {
        container.getExecutors().mainThread().execute(() -> {
            isStreaming.setValue(false);
            errorEvent.setValue(err);
        });
    }
}
