package com.example.data.provider;

import com.example.data.provider.adapter.AnthropicProvider;
import com.example.data.provider.adapter.AzureOpenAiProvider;
import com.example.data.provider.adapter.GeminiProvider;
import com.example.data.provider.adapter.OpenAiCompatibleProvider;
import com.example.domain.model.ProviderConfig;
import com.example.domain.provider.LlmProvider;
import com.example.util.AppExecutors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.OkHttpClient;

public class ProviderRegistry {
    private final OkHttpClient httpClient;
    private final AppExecutors executors;
    private final Map<String, LlmProvider> activeProviders = new ConcurrentHashMap<>();

    public ProviderRegistry(OkHttpClient httpClient, AppExecutors executors) {
        this.httpClient = httpClient;
        this.executors = executors;
    }

    public LlmProvider getOrCreateProvider(ProviderConfig config) {
        if (config == null) return null;
        String key = config.id;
        LlmProvider existing = activeProviders.get(key);
        if (existing != null) {
            return existing;
        }

        LlmProvider newProvider = createProvider(config);
        if (newProvider != null) {
            activeProviders.put(key, newProvider);
        }
        return newProvider;
    }

    public void invalidate(String providerId) {
        activeProviders.remove(providerId);
    }

    private LlmProvider createProvider(ProviderConfig config) {
        String type = config.type != null ? config.type.toUpperCase() : "OPENAI_COMPATIBLE";

        switch (type) {
            case "OPENAI":
            case "OPENAI_COMPATIBLE":
                return new OpenAiCompatibleProvider(config, httpClient, executors.networkIO());
            case "ANTHROPIC":
                return new AnthropicProvider(config, httpClient, executors.networkIO());
            case "GEMINI":
                return new GeminiProvider(config, httpClient, executors.networkIO());
            case "AZURE_OPENAI":
                return new AzureOpenAiProvider(config, httpClient, executors.networkIO());
            default:
                // Fallback to OpenAI-compatible interface for custom/local endpoints
                return new OpenAiCompatibleProvider(config, httpClient, executors.networkIO());
        }
    }
}
