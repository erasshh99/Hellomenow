package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.di.AppContainer;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.provider.LlmProvider;
import java.util.List;

public class ProvidersViewModel extends ViewModel {
    private final AppContainer container;
    private final LiveData<List<ProviderConfig>> providers;

    public ProvidersViewModel(@NonNull AppContainer container) {
        this.container = container;
        this.providers = container.getProviderRepository().getAllProviders();
    }

    public LiveData<List<ProviderConfig>> getProviders() {
        return providers;
    }

    public void saveProvider(ProviderConfig config) {
        container.getProviderRepository().saveProvider(config);
        container.getProviderRegistry().invalidate(config.id);
    }

    public void deleteProvider(String id) {
        container.getProviderRepository().deleteProvider(id);
        container.getProviderRegistry().invalidate(id);
    }

    public void testConnection(ProviderConfig config, SimpleCallback<String> callback) {
        container.getExecutors().networkIO().execute(() -> {
            try {
                LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(config);
                List<ModelDescriptor> models = provider.fetchModels();
                container.getExecutors().mainThread().execute(() -> {
                    callback.onSuccess("Success! Retrieved " + models.size() + " models.");
                });
            } catch (Exception e) {
                container.getExecutors().mainThread().execute(() -> {
                    callback.onError(e.getMessage());
                });
            }
        });
    }

    public void syncCatalog(ProviderConfig config, SimpleCallback<Integer> callback) {
        container.getExecutors().networkIO().execute(() -> {
            try {
                LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(config);
                List<ModelDescriptor> fetched = provider.fetchModels();
                for (ModelDescriptor m : fetched) {
                    ModelDescriptor discovered = container.getDiscoveryEngine().discoverCapabilities(m.id, config.id, m.rawJson);
                    m.capabilities = discovered.capabilities;
                    m.confidence = discovered.confidence;
                    m.source = discovered.source;
                    m.contextWindow = discovered.contextWindow;
                    m.maxOutputTokens = discovered.maxOutputTokens;
                    m.tier = discovered.tier;
                }
                container.getModelRepository().saveModels(fetched);
                container.getExecutors().mainThread().execute(() -> {
                    callback.onSuccess(fetched.size());
                });
            } catch (Exception e) {
                container.getExecutors().mainThread().execute(() -> {
                    callback.onError(e.getMessage());
                });
            }
        });
    }

    public interface SimpleCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
