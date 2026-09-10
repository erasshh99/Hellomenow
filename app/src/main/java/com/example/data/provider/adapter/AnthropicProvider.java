package com.example.data.provider.adapter;

import androidx.annotation.NonNull;
import com.example.data.provider.sse.ServerSentEventParser;
import com.example.domain.model.ContentPart;
import com.example.domain.model.ContentPartType;
import com.example.domain.model.ErrorDescriptor;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.model.Role;
import com.example.domain.model.TokenUsage;
import com.example.domain.model.UnifiedMessage;
import com.example.domain.provider.CapabilityProbeResult;
import com.example.domain.provider.ChatRequest;
import com.example.domain.provider.ChatResult;
import com.example.domain.provider.ChatStream;
import com.example.domain.provider.ImageRequest;
import com.example.domain.provider.LlmProvider;
import com.example.domain.provider.MediaTask;
import com.example.domain.provider.StreamListener;
import com.example.domain.provider.VideoRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AnthropicProvider implements LlmProvider {
    private final ProviderConfig config;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson = new Gson();

    public AnthropicProvider(ProviderConfig config, OkHttpClient baseClient, ExecutorService executor) {
        this.config = config;
        this.executor = executor;
        this.httpClient = baseClient.newBuilder()
                .connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String providerId() {
        return config.id;
    }

    private Request.Builder applyHeaders(Request.Builder builder) {
        if (config.apiKey != null && !config.apiKey.isEmpty()) {
            builder.header("x-api-key", config.apiKey);
        }
        builder.header("anthropic-version", "2023-06-01");
        builder.header("content-type", "application/json");
        return builder;
    }

    @Override
    public List<ModelDescriptor> fetchModels() throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/models";
        Request request = applyHeaders(new Request.Builder().url(url).get()).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Return static list if Anthropic models endpoint is restricted
                return getDefaultClaudeModels();
            }
            String body = response.body() != null ? response.body().string() : "{}";
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            List<ModelDescriptor> list = new ArrayList<>();
            if (data != null) {
                for (JsonElement el : data) {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String name = obj.has("display_name") ? obj.get("display_name").getAsString() : id;
                    ModelDescriptor desc = new ModelDescriptor(id, name, config.id);
                    desc.rawJson = obj.toString();
                    list.add(desc);
                }
            }
            return list.isEmpty() ? getDefaultClaudeModels() : list;
        } catch (Exception e) {
            return getDefaultClaudeModels();
        }
    }

    private List<ModelDescriptor> getDefaultClaudeModels() {
        List<ModelDescriptor> list = new ArrayList<>();
        list.add(new ModelDescriptor("claude-3-7-sonnet", "Claude 3.7 Sonnet (Hybrid)", config.id));
        list.add(new ModelDescriptor("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", config.id));
        list.add(new ModelDescriptor("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", config.id));
        list.add(new ModelDescriptor("claude-3-opus-20240229", "Claude 3 Opus", config.id));
        return list;
    }

    @Override
    public ChatStream openChatStream(ChatRequest req) {
        return new ChatStream() {
            private volatile Call call;
            private volatile boolean cancelled = false;
            private final ServerSentEventParser parser = new ServerSentEventParser();

            @Override
            public void start(StreamListener listener) {
                executor.execute(() -> {
                    try {
                        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/messages";
                        JsonObject payload = buildPayload(req, true);
                        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                        Request request = applyHeaders(new Request.Builder().url(url).post(body))
                                .header("Accept", "text/event-stream")
                                .build();

                        call = httpClient.newCall(request);
                        Response response = call.execute();

                        if (!response.isSuccessful()) {
                            String errBody = response.body() != null ? response.body().string() : "";
                            listener.onError(parseError(response.code(), errBody));
                            return;
                        }

                        InputStream is = response.body() != null ? response.body().byteStream() : null;
                        if (is == null) {
                            listener.onError(new ErrorDescriptor(500, ErrorDescriptor.Kind.SERVER_ERROR, "Empty stream", true, 0, ""));
                            return;
                        }

                        final TokenUsage[] capturedUsage = new TokenUsage[]{new TokenUsage()};

                        parser.parse(is, new ServerSentEventParser.Callback() {
                            @Override
                            public void onEvent(String eventType, @NonNull String data) {
                                try {
                                    JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                                    String type = obj.has("type") ? obj.get("type").getAsString() : eventType;

                                    if ("content_block_delta".equals(type)) {
                                        JsonObject delta = obj.getAsJsonObject("delta");
                                        if (delta != null) {
                                            String deltaType = delta.has("type") ? delta.get("type").getAsString() : "text_delta";
                                            if ("text_delta".equals(deltaType) && delta.has("text")) {
                                                listener.onDelta(delta.get("text").getAsString());
                                            } else if ("thinking_delta".equals(deltaType) && delta.has("thinking")) {
                                                listener.onReasoningDelta(delta.get("thinking").getAsString());
                                            }
                                        }
                                    } else if ("message_delta".equals(type)) {
                                        if (obj.has("usage")) {
                                            JsonObject u = obj.getAsJsonObject("usage");
                                            int c = u.has("output_tokens") ? u.get("output_tokens").getAsInt() : 0;
                                            capturedUsage[0].completionTokens = c;
                                            capturedUsage[0].totalTokens = capturedUsage[0].promptTokens + c;
                                        }
                                    } else if ("message_start".equals(type)) {
                                        JsonObject message = obj.getAsJsonObject("message");
                                        if (message != null && message.has("usage")) {
                                            JsonObject u = message.getAsJsonObject("usage");
                                            int p = u.has("input_tokens") ? u.get("input_tokens").getAsInt() : 0;
                                            capturedUsage[0].promptTokens = p;
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }

                            @Override
                            public void onComment(@NonNull String comment) {}

                            @Override
                            public void onError(@NonNull Throwable throwable) {
                                if (!cancelled) listener.onError(normalize(throwable));
                            }

                            @Override
                            public void onClosed() {
                                if (!cancelled) listener.onComplete(capturedUsage[0]);
                            }
                        });

                    } catch (Throwable t) {
                        if (!cancelled) listener.onError(normalize(t));
                    }
                });
            }

            @Override
            public void cancel() {
                cancelled = true;
                parser.cancel();
                if (call != null) call.cancel();
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        };
    }

    @Override
    public ChatResult chatOnce(ChatRequest req) throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/messages";
        JsonObject payload = buildPayload(req, false);
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            JsonObject obj = JsonParser.parseString(respBody).getAsJsonObject();
            JsonArray contentArr = obj.getAsJsonArray("content");
            StringBuilder textBuilder = new StringBuilder();
            StringBuilder reasoningBuilder = new StringBuilder();

            if (contentArr != null) {
                for (JsonElement el : contentArr) {
                    JsonObject block = el.getAsJsonObject();
                    String blockType = block.has("type") ? block.get("type").getAsString() : "text";
                    if ("text".equals(blockType) && block.has("text")) {
                        textBuilder.append(block.get("text").getAsString());
                    } else if ("thinking".equals(blockType) && block.has("thinking")) {
                        reasoningBuilder.append(block.get("thinking").getAsString());
                    }
                }
            }

            TokenUsage usage = new TokenUsage();
            if (obj.has("usage")) {
                JsonObject u = obj.getAsJsonObject("usage");
                int p = u.has("input_tokens") ? u.get("input_tokens").getAsInt() : 0;
                int c = u.has("output_tokens") ? u.get("output_tokens").getAsInt() : 0;
                usage = new TokenUsage(p, c, p + c);
            }

            return new ChatResult(textBuilder.toString(), reasoningBuilder.length() > 0 ? reasoningBuilder.toString() : null, usage);
        }
    }

    @Override
    public MediaTask generateImage(ImageRequest req) throws Exception {
        throw new UnsupportedOperationException("Anthropic Claude does not support image generation natively.");
    }

    @Override
    public MediaTask generateVideo(VideoRequest req) throws Exception {
        throw new UnsupportedOperationException("Anthropic Claude does not support video generation natively.");
    }

    @Override
    public CapabilityProbeResult probe(ModelDescriptor m) {
        ChatRequest req = new ChatRequest();
        req.modelId = m.id;
        req.maxTokens = 2;
        req.messages = new ArrayList<>();
        req.messages.add(new UnifiedMessage(null, Role.USER, "Hello"));
        try {
            ChatResult res = chatOnce(req);
            return new CapabilityProbeResult(m.id, true, m.capabilities, 95, "Claude answered: " + res.content);
        } catch (Exception e) {
            return new CapabilityProbeResult(m.id, false, m.capabilities, 30, e.getMessage());
        }
    }

    @Override
    public ErrorDescriptor normalize(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return new ErrorDescriptor(408, ErrorDescriptor.Kind.NETWORK_TIMEOUT, "Anthropic request timed out.", true, 2000, t.getMessage());
        }
        if (t instanceof SSLException) {
            return new ErrorDescriptor(0, ErrorDescriptor.Kind.TLS_FAILURE, "TLS connection error to Anthropic.", false, 0, t.getMessage());
        }
        return new ErrorDescriptor(0, ErrorDescriptor.Kind.UNKNOWN, t.getMessage(), true, 1000, t.getMessage());
    }

    private ErrorDescriptor parseError(int code, String body) {
        ErrorDescriptor.Kind kind = ErrorDescriptor.Kind.UNKNOWN;
        if (code == 401) kind = ErrorDescriptor.Kind.AUTH_FAILURE;
        else if (code == 429) kind = ErrorDescriptor.Kind.RATE_LIMIT;
        else if (code >= 500) kind = ErrorDescriptor.Kind.SERVER_ERROR;
        return new ErrorDescriptor(code, kind, "Anthropic HTTP " + code + ": " + body, code == 429 || code >= 500, 0, body);
    }

    private JsonObject buildPayload(ChatRequest req, boolean stream) {
        JsonObject json = new JsonObject();
        json.addProperty("model", req.modelId);
        json.addProperty("max_tokens", req.maxTokens > 0 ? req.maxTokens : 4096);
        json.addProperty("stream", stream);

        if (req.systemPrompt != null && !req.systemPrompt.isEmpty()) {
            json.addProperty("system", req.systemPrompt);
        }

        JsonArray messages = new JsonArray();
        if (req.messages != null) {
            for (UnifiedMessage m : req.messages) {
                if (m.role == Role.SYSTEM) continue; // Anthropic top-levels system
                JsonObject msg = new JsonObject();
                msg.addProperty("role", m.role == Role.ASSISTANT ? "assistant" : "user");

                JsonArray content = new JsonArray();
                for (ContentPart p : m.parts) {
                    if (p.type == ContentPartType.TEXT) {
                        JsonObject block = new JsonObject();
                        block.addProperty("type", "text");
                        block.addProperty("text", p.text);
                        content.add(block);
                    }
                }
                msg.add("content", content);
                messages.add(msg);
            }
        }
        json.add("messages", messages);
        return json;
    }
}
