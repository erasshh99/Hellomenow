package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.di.AppContainer;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.provider.CapabilityProbeResult;
import com.example.domain.provider.LlmProvider;
import java.util.List;

public class ModelsViewModel extends ViewModel {
    private final AppContainer container;
    private final LiveData<List<ModelDescriptor>> allModels;

    public ModelsViewModel(@NonNull AppContainer container) {
        this.container = container;
        this.allModels = container.getModelRepository().getAllModels();
    }

    public LiveData<List<ModelDescriptor>> getAllModels() {
        return allModels;
    }

    public void probeModel(ModelDescriptor model, ProbeCallback callback) {
        container.getExecutors().networkIO().execute(() -> {
            try {
                ProviderConfig pConfig = container.getProviderRepository().getProviderSync(model.providerId);
                if (pConfig == null) {
                    container.getExecutors().mainThread().execute(() -> callback.onResult(new CapabilityProbeResult(model.id, false, model.capabilities, 0, "Provider not configured.")));
                    return;
                }
                LlmProvider provider = container.getProviderRegistry().getOrCreateProvider(pConfig);
                CapabilityProbeResult result = provider.probe(model);
                ModelDescriptor updated = container.getDiscoveryEngine().applyProbeResult(model, result);
                container.getModelRepository().updateModel(updated);
                container.getExecutors().mainThread().execute(() -> callback.onResult(result));
            } catch (Exception e) {
                container.getExecutors().mainThread().execute(() -> callback.onResult(new CapabilityProbeResult(model.id, false, model.capabilities, 0, e.getMessage())));
            }
        });
    }

    public interface ProbeCallback {
        void onResult(CapabilityProbeResult result);
    }
}
