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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiProvider implements LlmProvider {
    private final ProviderConfig config;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson = new Gson();

    public GeminiProvider(ProviderConfig config, OkHttpClient baseClient, ExecutorService executor) {
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
            builder.header("x-goog-api-key", config.apiKey);
        }
        return builder;
    }

    @Override
    public List<ModelDescriptor> fetchModels() throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1beta/models";
        Request request = applyHeaders(new Request.Builder().url(url).get()).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return getDefaultGeminiModels();
            }
            String body = response.body() != null ? response.body().string() : "{}";
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray models = json.getAsJsonArray("models");
            List<ModelDescriptor> list = new ArrayList<>();
            if (models != null) {
                for (JsonElement el : models) {
                    JsonObject obj = el.getAsJsonObject();
                    String name = obj.get("name").getAsString(); // e.g. "models/gemini-2.0-flash"
                    String id = name.replace("models/", "");
                    String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : id;
                    ModelDescriptor desc = new ModelDescriptor(id, displayName, config.id);
                    desc.rawJson = obj.toString();
                    list.add(desc);
                }
            }
            return list.isEmpty() ? getDefaultGeminiModels() : list;
        } catch (Exception e) {
            return getDefaultGeminiModels();
        }
    }

    private List<ModelDescriptor> getDefaultGeminiModels() {
        List<ModelDescriptor> list = new ArrayList<>();
        list.add(new ModelDescriptor("gemini-2.0-flash", "Gemini 2.0 Flash", config.id));
        list.add(new ModelDescriptor("gemini-2.0-flash-thinking-exp-01-21", "Gemini 2.0 Flash Thinking", config.id));
        list.add(new ModelDescriptor("gemini-1.5-pro", "Gemini 1.5 Pro", config.id));
        list.add(new ModelDescriptor("gemini-1.5-flash", "Gemini 1.5 Flash", config.id));
        list.add(new ModelDescriptor("imagen-3.0-generate-002", "Imagen 3", config.id));
        list.add(new ModelDescriptor("veo-2.0-generate-001", "Veo 2 Video", config.id));
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
                        String cleanModel = req.modelId.startsWith("models/") ? req.modelId.replace("models/", "") : req.modelId;
                        String url = config.baseUrl.replaceAll("/+$", "") + "/v1beta/models/" + cleanModel + ":streamGenerateContent?alt=sse";
                        JsonObject payload = buildPayload(req);
                        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                        Request request = applyHeaders(new Request.Builder().url(url).post(body))
                                .header("Accept", "text/event-stream")
                                .build();

                        call = httpClient.newCall(request);
                        Response response = call.execute();

                        if (!response.isSuccessful()) {
                            String err = response.body() != null ? response.body().string() : "";
                            listener.onError(new ErrorDescriptor(response.code(), ErrorDescriptor.Kind.SERVER_ERROR, "Gemini error " + response.code() + ": " + err, true, 0, err));
                            return;
                        }

                        InputStream is = response.body() != null ? response.body().byteStream() : null;
                        if (is == null) {
                            listener.onError(new ErrorDescriptor(500, ErrorDescriptor.Kind.SERVER_ERROR, "Empty response", true, 0, ""));
                            return;
                        }

                        final TokenUsage[] tokenUsage = new TokenUsage[]{new TokenUsage()};

                        parser.parse(is, new ServerSentEventParser.Callback() {
                            @Override
                            public void onEvent(String eventType, @NonNull String data) {
                                try {
                                    JsonObject root = JsonParser.parseString(data).getAsJsonObject();
                                    if (root.has("usageMetadata")) {
                                        JsonObject u = root.getAsJsonObject("usageMetadata");
                                        int p = u.has("promptTokenCount") ? u.get("promptTokenCount").getAsInt() : 0;
                                        int c = u.has("candidatesTokenCount") ? u.get("candidatesTokenCount").getAsInt() : 0;
                                        int t = u.has("totalTokenCount") ? u.get("totalTokenCount").getAsInt() : (p + c);
                                        tokenUsage[0] = new TokenUsage(p, c, t);
                                    }

                                    JsonArray candidates = root.getAsJsonArray("candidates");
                                    if (candidates != null && candidates.size() > 0) {
                                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                                        JsonObject content = candidate.getAsJsonObject("content");
                                        if (content != null && content.has("parts")) {
                                            JsonArray parts = content.getAsJsonArray("parts");
                                            for (JsonElement p : parts) {
                                                JsonObject partObj = p.getAsJsonObject();
                                                if (partObj.has("text")) {
                                                    listener.onDelta(partObj.get("text").getAsString());
                                                }
                                            }
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
                                if (!cancelled) listener.onComplete(tokenUsage[0]);
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
        String cleanModel = req.modelId.startsWith("models/") ? req.modelId.replace("models/", "") : req.modelId;
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1beta/models/" + cleanModel + ":generateContent";
        JsonObject payload = buildPayload(req);
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            JsonObject root = JsonParser.parseString(respBody).getAsJsonObject();
            StringBuilder text = new StringBuilder();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    for (JsonElement p : content.getAsJsonArray("parts")) {
                        if (p.getAsJsonObject().has("text")) {
                            text.append(p.getAsJsonObject().get("text").getAsString());
                        }
                    }
                }
            }
            TokenUsage usage = new TokenUsage();
            if (root.has("usageMetadata")) {
                JsonObject u = root.getAsJsonObject("usageMetadata");
                int p = u.has("promptTokenCount") ? u.get("promptTokenCount").getAsInt() : 0;
                int c = u.has("candidatesTokenCount") ? u.get("candidatesTokenCount").getAsInt() : 0;
                usage = new TokenUsage(p, c, p + c);
            }
            return new ChatResult(text.toString(), usage);
        }
    }

    @Override
    public MediaTask generateImage(ImageRequest req) throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1beta/models/" + (req.modelId != null ? req.modelId : "imagen-3.0-generate-002") + ":predict";
        JsonObject payload = new JsonObject();
        JsonArray instances = new JsonArray();
        JsonObject inst = new JsonObject();
        inst.addProperty("prompt", req.prompt);
        instances.add(inst);
        payload.add("instances", instances);

        JsonObject params = new JsonObject();
        params.addProperty("sampleCount", req.numImages);
        params.addProperty("aspectRatio", "1:1");
        payload.add("parameters", params);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("Imagen call failed: " + respStr);
            }
            JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
            if (obj.has("predictions")) {
                JsonArray preds = obj.getAsJsonArray("predictions");
                if (preds.size() > 0) {
                    JsonObject p = preds.get(0).getAsJsonObject();
                    if (p.has("bytesBase64Encoded")) {
                        MediaTask t = MediaTask.direct("data:image/jpeg;base64," + p.get("bytesBase64Encoded").getAsString());
                        t.format = "b64_json";
                        return t;
                    }
                }
            }
            throw new IOException("No image data in Imagen response");
        }
    }

    @Override
    public MediaTask generateVideo(VideoRequest req) throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1beta/models/" + (req.modelId != null ? req.modelId : "veo-2.0-generate-001") + ":predictLongRunning";
        JsonObject payload = new JsonObject();
        JsonArray instances = new JsonArray();
        JsonObject inst = new JsonObject();
        inst.addProperty("prompt", req.prompt);
        instances.add(inst);
        payload.add("instances", instances);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("Veo request failed: " + respStr);
            }
            JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
            if (obj.has("name")) {
                String operationName = obj.get("name").getAsString();
                String pollUrl = config.baseUrl.replaceAll("/+$", "") + "/v1beta/" + operationName;
                return MediaTask.async(operationName, pollUrl);
            }
            throw new IOException("No operation name in Veo response");
        }
    }

    @Override
    public CapabilityProbeResult probe(ModelDescriptor m) {
        ChatRequest req = new ChatRequest();
        req.modelId = m.id;
        req.maxTokens = 2;
        req.messages = new ArrayList<>();
        req.messages.add(new UnifiedMessage(null, Role.USER, "Hi"));
        try {
            ChatResult res = chatOnce(req);
            return new CapabilityProbeResult(m.id, true, m.capabilities, 95, "Gemini replied: " + res.content);
        } catch (Exception e) {
            return new CapabilityProbeResult(m.id, false, m.capabilities, 30, e.getMessage());
        }
    }

    @Override
    public ErrorDescriptor normalize(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return new ErrorDescriptor(408, ErrorDescriptor.Kind.NETWORK_TIMEOUT, "Gemini request timed out.", true, 2000, t.getMessage());
        }
        if (t instanceof SSLException) {
            return new ErrorDescriptor(0, ErrorDescriptor.Kind.TLS_FAILURE, "TLS connection error to Gemini.", false, 0, t.getMessage());
        }
        return new ErrorDescriptor(0, ErrorDescriptor.Kind.UNKNOWN, t.getMessage(), true, 1000, t.getMessage());
    }

    private JsonObject buildPayload(ChatRequest req) {
        JsonObject json = new JsonObject();

        if (req.systemPrompt != null && !req.systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject p = new JsonObject();
            p.addProperty("text", req.systemPrompt);
            parts.add(p);
            sys.add("parts", parts);
            json.add("systemInstruction", sys);
        }

        JsonArray contents = new JsonArray();
        if (req.messages != null) {
            for (UnifiedMessage m : req.messages) {
                JsonObject c = new JsonObject();
                c.addProperty("role", m.role == Role.ASSISTANT ? "model" : "user");
                JsonArray parts = new JsonArray();
                for (ContentPart part : m.parts) {
                    if (part.type == ContentPartType.TEXT) {
                        JsonObject p = new JsonObject();
                        p.addProperty("text", part.text);
                        parts.add(p);
                    }
                }
                c.add("parts", parts);
                contents.add(c);
            }
        }
        json.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", req.temperature);
        genConfig.addProperty("topP", req.topP);
        if (req.maxTokens > 0) {
            genConfig.addProperty("maxOutputTokens", req.maxTokens);
        }
        json.add("generationConfig", genConfig);

        return json;
    }
}
