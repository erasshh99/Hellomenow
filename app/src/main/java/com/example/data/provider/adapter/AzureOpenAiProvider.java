package com.example.data.provider.adapter;

import com.example.domain.model.ProviderConfig;
import java.util.concurrent.ExecutorService;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AzureOpenAiProvider extends OpenAiCompatibleProvider {

    public AzureOpenAiProvider(ProviderConfig config, OkHttpClient baseClient, ExecutorService executor) {
        super(config, baseClient, executor);
    }

    @Override
    protected Request.Builder applyHeaders(Request.Builder builder) {
        if (config.apiKey != null && !config.apiKey.isEmpty()) {
            builder.header("api-key", config.apiKey);
        }
        if (config.customHeaders != null) {
            for (var entry : config.customHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }
}
