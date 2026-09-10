package com.example.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.data.local.dao.ModelDao;
import com.example.data.local.entity.ModelEntity;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.PricingInfo;
import com.example.domain.repository.ModelRepository;
import com.example.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public class ModelRepositoryImpl implements ModelRepository {
    private final ModelDao modelDao;
    private final AppExecutors executors;

    public ModelRepositoryImpl(ModelDao modelDao, AppExecutors executors) {
        this.modelDao = modelDao;
        this.executors = executors;
    }

    @Override
    public LiveData<List<ModelDescriptor>> getAllModels() {
        return Transformations.map(modelDao.getAllModels(), entities -> {
            List<ModelDescriptor> list = new ArrayList<>();
            if (entities != null) {
                for (ModelEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public LiveData<List<ModelDescriptor>> getModelsForProvider(String providerId) {
        return Transformations.map(modelDao.getModelsForProvider(providerId), entities -> {
            List<ModelDescriptor> list = new ArrayList<>();
            if (entities != null) {
                for (ModelEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public List<ModelDescriptor> getModelsForProviderSync(String providerId) {
        List<ModelEntity> entities = modelDao.getModelsForProviderSync(providerId);
        List<ModelDescriptor> list = new ArrayList<>();
        if (entities != null) {
            for (ModelEntity e : entities) {
                list.add(toDomain(e));
            }
        }
        return list;
    }

    @Override
    public ModelDescriptor getModelByIdSync(String modelId) {
        ModelEntity entity = modelDao.getModelByIdSync(modelId);
        return entity != null ? toDomain(entity) : null;
    }

    @Override
    public void saveModel(ModelDescriptor model) {
        executors.diskIO().execute(() -> modelDao.insert(toEntity(model)));
    }

    @Override
    public void saveModels(List<ModelDescriptor> models) {
        executors.diskIO().execute(() -> {
            List<ModelEntity> entities = new ArrayList<>();
            for (ModelDescriptor m : models) {
                entities.add(toEntity(m));
            }
            modelDao.insertAll(entities);
        });
    }

    @Override
    public void deleteModelsForProvider(String providerId) {
        executors.diskIO().execute(() -> modelDao.deleteModelsForProvider(providerId));
    }

    private ModelDescriptor toDomain(ModelEntity e) {
        ModelDescriptor m = new ModelDescriptor(e.id, e.displayName, e.providerId);
        m.family = e.family;
        m.version = e.version;
        m.contextWindow = e.contextWindow;
        m.maxOutputTokens = e.maxOutputTokens;
        m.capabilities = e.capabilities != null ? e.capabilities : new CapabilitySet();
        m.pricing = new PricingInfo(e.promptPricePerMillion, e.completionPricePerMillion, e.currency);
        m.tier = e.tier;
        m.deprecated = e.deprecated;
        m.rawJson = e.rawJson;
        m.detectedAt = e.detectedAt;
        m.confidence = e.confidence;
        m.source = e.source;
        m.isFavorite = e.isFavorite;
        return m;
    }

    private ModelEntity toEntity(ModelDescriptor m) {
        ModelEntity e = new ModelEntity(m.id, m.displayName, m.providerId);
        e.family = m.family;
        e.version = m.version;
        e.contextWindow = m.contextWindow;
        e.maxOutputTokens = m.maxOutputTokens;
        e.capabilities = m.capabilities;
        if (m.pricing != null) {
            e.promptPricePerMillion = m.pricing.promptPricePerMillion;
            e.completionPricePerMillion = m.pricing.completionPricePerMillion;
            e.currency = m.pricing.currency;
        }
        e.tier = m.tier;
        e.deprecated = m.deprecated;
        e.rawJson = m.rawJson;
        e.detectedAt = m.detectedAt;
        e.confidence = m.confidence;
        e.source = m.source;
        e.isFavorite = m.isFavorite;
        return e;
    }
}
