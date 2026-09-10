package com.example.data.provider.adapter;

import androidx.annotation.NonNull;
import com.example.data.provider.sse.ServerSentEventParser;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.ContentPart;
import com.example.domain.model.ContentPartType;
import com.example.domain.model.ErrorDescriptor;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.model.Role;
import com.example.domain.model.Ternary;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAiCompatibleProvider implements LlmProvider {
    protected final ProviderConfig config;
    protected final OkHttpClient httpClient;
    protected final ExecutorService executor;
    protected final Gson gson = new Gson();

    public OpenAiCompatibleProvider(ProviderConfig config, OkHttpClient baseClient, ExecutorService executor) {
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

    protected Request.Builder applyHeaders(Request.Builder builder) {
        if (config.apiKey != null && !config.apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + config.apiKey);
        }
        if (config.orgId != null && !config.orgId.isEmpty()) {
            builder.header("OpenAI-Organization", config.orgId);
        }
        if (config.customHeaders != null) {
            for (Map.Entry<String, String> entry : config.customHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    @Override
    public List<ModelDescriptor> fetchModels() throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/models";
        Request request = applyHeaders(new Request.Builder().url(url).get()).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch models: HTTP " + response.code() + " " + response.message());
            }
            String body = response.body() != null ? response.body().string() : "{}";
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            List<ModelDescriptor> list = new ArrayList<>();
            if (data != null) {
                for (JsonElement el : data) {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    ModelDescriptor desc = new ModelDescriptor(id, id, config.id);
                    desc.rawJson = obj.toString();
                    list.add(desc);
                }
            }
            return list;
        }
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
                        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/chat/completions";
                        JsonObject payload = buildPayload(req, true);
                        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                        Request request = applyHeaders(new Request.Builder().url(url).post(body))
                                .header("Accept", "text/event-stream")
                                .build();

                        call = httpClient.newCall(request);
                        Response response = call.execute();

                        if (!response.isSuccessful()) {
                            String errBody = response.body() != null ? response.body().string() : "";
                            ErrorDescriptor err = parseErrorResponse(response.code(), response.headers().get("Retry-After"), errBody);
                            listener.onError(err);
                            return;
                        }

                        InputStream is = response.body() != null ? response.body().byteStream() : null;
                        if (is == null) {
                            listener.onError(new ErrorDescriptor(500, ErrorDescriptor.Kind.SERVER_ERROR, "Empty response stream", true, 0, ""));
                            return;
                        }

                        final TokenUsage[] capturedUsage = new TokenUsage[]{new TokenUsage()};

                        parser.parse(is, new ServerSentEventParser.Callback() {
                            @Override
                            public void onEvent(String eventType, @NonNull String data) {
                                if ("[DONE]".equalsIgnoreCase(data.trim())) {
                                    return;
                                }
                                try {
                                    JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                                    if (obj.has("usage") && !obj.get("usage").isJsonNull()) {
                                        JsonObject u = obj.getAsJsonObject("usage");
                                        int p = u.has("prompt_tokens") ? u.get("prompt_tokens").getAsInt() : 0;
                                        int c = u.has("completion_tokens") ? u.get("completion_tokens").getAsInt() : 0;
                                        int t = u.has("total_tokens") ? u.get("total_tokens").getAsInt() : (p + c);
                                        capturedUsage[0] = new TokenUsage(p, c, t);
                                    }

                                    JsonArray choices = obj.getAsJsonArray("choices");
                                    if (choices != null && choices.size() > 0) {
                                        JsonObject choice = choices.get(0).getAsJsonObject();
                                        if (choice.has("delta")) {
                                            JsonObject delta = choice.getAsJsonObject("delta");
                                            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                                listener.onDelta(delta.get("content").getAsString());
                                            }
                                            if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                                                listener.onReasoningDelta(delta.get("reasoning_content").getAsString());
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            @Override
                            public void onComment(@NonNull String comment) {}

                            @Override
                            public void onError(@NonNull Throwable throwable) {
                                if (!cancelled) {
                                    listener.onError(normalize(throwable));
                                }
                            }

                            @Override
                            public void onClosed() {
                                if (!cancelled) {
                                    listener.onComplete(capturedUsage[0]);
                                }
                            }
                        });

                    } catch (Throwable t) {
                        if (!cancelled) {
                            listener.onError(normalize(t));
                        }
                    }
                });
            }

            @Override
            public void cancel() {
                cancelled = true;
                parser.cancel();
                if (call != null) {
                    call.cancel();
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        };
    }

    @Override
    public ChatResult chatOnce(ChatRequest req) throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/chat/completions";
        JsonObject payload = buildPayload(req, false);
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + respBody);
            }
            JsonObject obj = JsonParser.parseString(respBody).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            String content = "";
            String reasoning = null;
            if (choices != null && choices.size() > 0) {
                JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (msg != null && msg.has("content") && !msg.get("content").isJsonNull()) {
                    content = msg.get("content").getAsString();
                }
                if (msg != null && msg.has("reasoning_content") && !msg.get("reasoning_content").isJsonNull()) {
                    reasoning = msg.get("reasoning_content").getAsString();
                }
            }
            TokenUsage usage = new TokenUsage();
            if (obj.has("usage")) {
                JsonObject u = obj.getAsJsonObject("usage");
                int p = u.has("prompt_tokens") ? u.get("prompt_tokens").getAsInt() : 0;
                int c = u.has("completion_tokens") ? u.get("completion_tokens").getAsInt() : 0;
                usage = new TokenUsage(p, c, p + c);
            }
            return new ChatResult(content, reasoning, usage);
        }
    }

    @Override
    public MediaTask generateImage(ImageRequest req) throws Exception {
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/images/generations";
        JsonObject payload = new JsonObject();
        payload.addProperty("model", req.modelId != null ? req.modelId : "dall-e-3");
        payload.addProperty("prompt", req.prompt);
        payload.addProperty("n", req.numImages);
        payload.addProperty("size", req.width + "x" + req.height);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("Image gen failed: " + respStr);
            }
            JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
            JsonArray data = obj.getAsJsonArray("data");
            if (data != null && data.size() > 0) {
                JsonObject item = data.get(0).getAsJsonObject();
                if (item.has("url")) {
                    return MediaTask.direct(item.get("url").getAsString());
                } else if (item.has("b64_json")) {
                    MediaTask t = MediaTask.direct("data:image/png;base64," + item.get("b64_json").getAsString());
                    t.format = "b64_json";
                    return t;
                }
            }
            throw new IOException("No image URL returned");
        }
    }

    @Override
    public MediaTask generateVideo(VideoRequest req) throws Exception {
        // Fallback for OpenAI compatible video (e.g. Luma, Kling, Sora endpoints)
        String url = config.baseUrl.replaceAll("/+$", "") + "/v1/video/generations";
        JsonObject payload = new JsonObject();
        payload.addProperty("model", req.modelId);
        payload.addProperty("prompt", req.prompt);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = applyHeaders(new Request.Builder().url(url).post(body)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respStr = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException("Video gen failed: " + respStr);
            }
            JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
            if (obj.has("id")) {
                String jobId = obj.get("id").getAsString();
                String pollUrl = config.baseUrl.replaceAll("/+$", "") + "/v1/video/generations/" + jobId;
                return MediaTask.async(jobId, pollUrl);
            }
            throw new IOException("No video job ID returned");
        }
    }

    @Override
    public CapabilityProbeResult probe(ModelDescriptor m) {
        long start = System.currentTimeMillis();
        try {
            // Test 1: Simple 1-token probe
            ChatRequest testReq = new ChatRequest();
            testReq.modelId = m.id;
            testReq.maxTokens = 1;
            testReq.messages = new ArrayList<>();
            testReq.messages.add(new UnifiedMessage(null, Role.USER, "Hi"));
            ChatResult result = chatOnce(testReq);

            CapabilitySet caps = m.capabilities != null ? m.capabilities.copy() : new CapabilitySet();
            caps.streaming = Ternary.TRUE;
            caps.systemPromptSupport = Ternary.TRUE;

            long duration = System.currentTimeMillis() - start;
            return new CapabilityProbeResult(m.id, true, caps, 90, "Model responded in " + duration + "ms: " + result.content);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            CapabilityProbeResult res = new CapabilityProbeResult(m.id, false, m.capabilities, 40, "Probe failed: " + e.getMessage());
            res.durationMs = duration;
            res.errorMessage = e.getMessage();
            return res;
        }
    }

    @Override
    public ErrorDescriptor normalize(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return new ErrorDescriptor(408, ErrorDescriptor.Kind.NETWORK_TIMEOUT, "Request timed out. The model server may be slow or overloaded.", true, 2000, t.getMessage());
        }
        if (t instanceof SSLException) {
            return new ErrorDescriptor(0, ErrorDescriptor.Kind.TLS_FAILURE, "TLS/SSL connection handshake failed.", false, 0, t.getMessage());
        }
        String msg = t.getMessage() != null ? t.getMessage() : "";
        if (msg.contains("CLEARTEXT") || msg.contains("cleartext")) {
            return new ErrorDescriptor(0, ErrorDescriptor.Kind.CLEARTEXT_BLOCKED, "Cleartext HTTP traffic was blocked. Verify network security config or use HTTPS.", false, 0, msg);
        }
        return new ErrorDescriptor(0, ErrorDescriptor.Kind.UNKNOWN, "Connection error: " + msg, true, 1000, msg);
    }

    protected ErrorDescriptor parseErrorResponse(int code, String retryAfterHeader, String rawBody) {
        long retryAfterMs = 0;
        if (retryAfterHeader != null) {
            try {
                retryAfterMs = Long.parseLong(retryAfterHeader.trim()) * 1000L;
            } catch (Exception ignored) {}
        }

        ErrorDescriptor.Kind kind;
        String userMsg;
        boolean retryable;

        switch (code) {
            case 400:
                kind = ErrorDescriptor.Kind.INVALID_MODEL;
                userMsg = "Invalid model ID or request payload format.";
                retryable = false;
                break;
            case 401:
            case 403:
                kind = ErrorDescriptor.Kind.AUTH_FAILURE;
                userMsg = "Authentication failed. Please check your API key.";
                retryable = false;
                break;
            case 404:
                kind = ErrorDescriptor.Kind.UNKNOWN_ENDPOINT;
                userMsg = "Endpoint or model not found (HTTP 404).";
                retryable = false;
                break;
            case 413:
                kind = ErrorDescriptor.Kind.PAYLOAD_TOO_LARGE;
                userMsg = "Context size exceeded model limits. History has been trimmed.";
                retryable = true;
                break;
            case 422:
                kind = ErrorDescriptor.Kind.UNSUPPORTED_MODALITY;
                userMsg = "Model does not support one of the attached modalities.";
                retryable = false;
                break;
            case 429:
                kind = ErrorDescriptor.Kind.RATE_LIMIT;
                userMsg = "Rate limit exceeded. " + (retryAfterMs > 0 ? "Retrying in " + (retryAfterMs/1000) + "s." : "Please slow down.");
                retryable = true;
                break;
            case 500:
            case 502:
            case 503:
            case 504:
                kind = ErrorDescriptor.Kind.SERVER_ERROR;
                userMsg = "Upstream model server error (HTTP " + code + "). Retrying...";
                retryable = true;
                break;
            default:
                kind = ErrorDescriptor.Kind.UNKNOWN;
                userMsg = "Request error (HTTP " + code + "): " + rawBody;
                retryable = false;
                break;
        }

        return new ErrorDescriptor(code, kind, userMsg, retryable, retryAfterMs, rawBody);
    }

    protected JsonObject buildPayload(ChatRequest req, boolean stream) {
        JsonObject json = new JsonObject();
        json.addProperty("model", req.modelId);
        json.addProperty("stream", stream);
        if (stream) {
            JsonObject streamOpts = new JsonObject();
            streamOpts.addProperty("include_usage", true);
            json.add("stream_options", streamOpts);
        }
        json.addProperty("temperature", req.temperature);
        json.addProperty("top_p", req.topP);
        if (req.maxTokens > 0) {
            json.addProperty("max_tokens", req.maxTokens);
        }

        JsonArray msgs = new JsonArray();
        if (req.systemPrompt != null && !req.systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", req.systemPrompt);
            msgs.add(sys);
        }

        if (req.messages != null) {
            for (UnifiedMessage m : req.messages) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", m.role.name().toLowerCase(Locale.US));

                // Check for multimodal parts
                boolean hasMedia = false;
                for (ContentPart p : m.parts) {
                    if (p.type == ContentPartType.IMAGE) {
                        hasMedia = true;
                        break;
                    }
                }

                if (!hasMedia) {
                    msgObj.addProperty("content", m.getTextContent());
                } else {
                    JsonArray partsArray = new JsonArray();
                    for (ContentPart p : m.parts) {
                        if (p.type == ContentPartType.TEXT) {
                            JsonObject t = new JsonObject();
                            t.addProperty("type", "text");
                            t.addProperty("text", p.text);
                            partsArray.add(t);
                        } else if (p.type == ContentPartType.IMAGE) {
                            JsonObject img = new JsonObject();
                            img.addProperty("type", "image_url");
                            JsonObject urlObj = new JsonObject();
                            urlObj.addProperty("url", p.mediaUrl != null ? p.mediaUrl : p.localPath);
                            img.add("image_url", urlObj);
                            partsArray.add(img);
                        }
                    }
                    msgObj.add("content", partsArray);
                }
                msgs.add(msgObj);
            }
        }
        json.add("messages", msgs);
        return json;
    }
}
