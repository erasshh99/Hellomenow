package com.example.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.domain.model.ModelDescriptor;
import java.util.List;

public interface ModelRepository {
    LiveData<List<ModelDescriptor>> getAllModels();
    LiveData<List<ModelDescriptor>> getModelsForProvider(String providerId);
    List<ModelDescriptor> getModelsForProviderSync(String providerId);
    ModelDescriptor getModelByIdSync(String modelId);
    default ModelDescriptor getModelSync(String modelId) { return getModelByIdSync(modelId); }
    void saveModel(ModelDescriptor model);
    default void updateModel(ModelDescriptor model) { saveModel(model); }
    void saveModels(List<ModelDescriptor> models);
    void deleteModelsForProvider(String providerId);
}
