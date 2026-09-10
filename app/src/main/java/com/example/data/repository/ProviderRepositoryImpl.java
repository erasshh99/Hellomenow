package com.example.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.data.local.dao.ProviderDao;
import com.example.data.local.entity.ProviderEntity;
import com.example.data.security.SecureKeyStorage;
import com.example.domain.model.ProviderConfig;
import com.example.domain.repository.ProviderRepository;
import com.example.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public class ProviderRepositoryImpl implements ProviderRepository {
    private final ProviderDao providerDao;
    private final SecureKeyStorage keyStorage;
    private final AppExecutors executors;

    public ProviderRepositoryImpl(ProviderDao providerDao, SecureKeyStorage keyStorage, AppExecutors executors) {
        this.providerDao = providerDao;
        this.keyStorage = keyStorage;
        this.executors = executors;
    }

    @Override
    public LiveData<List<ProviderConfig>> getAllProviders() {
        return Transformations.map(providerDao.getAllProviders(), entities -> {
            List<ProviderConfig> list = new ArrayList<>();
            if (entities != null) {
                for (ProviderEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public List<ProviderConfig> getAllProvidersSync() {
        List<ProviderEntity> entities = providerDao.getAllProvidersSync();
        List<ProviderConfig> list = new ArrayList<>();
        if (entities != null) {
            for (ProviderEntity e : entities) {
                list.add(toDomain(e));
            }
        }
        return list;
    }

    @Override
    public ProviderConfig getProviderByIdSync(String id) {
        ProviderEntity entity = providerDao.getProviderByIdSync(id);
        return entity != null ? toDomain(entity) : null;
    }

    @Override
    public void saveProvider(ProviderConfig provider) {
        // Store API key in secure keystore, never in plain SQLite
        if (provider.apiKey != null) {
            keyStorage.storeApiKey(provider.id, provider.apiKey);
        }
        executors.diskIO().execute(() -> providerDao.insert(toEntity(provider)));
    }

    @Override
    public void deleteProvider(String id) {
        keyStorage.removeApiKey(id);
        executors.diskIO().execute(() -> providerDao.deleteById(id));
    }

    private ProviderConfig toDomain(ProviderEntity e) {
        ProviderConfig p = new ProviderConfig(e.id, e.name, e.type, e.baseUrl);
        p.apiKey = keyStorage.getApiKey(e.id);
        p.orgId = e.orgId;
        p.customHeaders = e.customHeaders;
        p.timeoutSeconds = e.timeoutSeconds;
        p.isEnabled = e.isEnabled;
        p.isLocal = e.isLocal;
        p.useAiDetection = e.useAiDetection;
        p.customBodyTemplate = e.customBodyTemplate;
        p.customJsonPath = e.customJsonPath;
        return p;
    }

    private ProviderEntity toEntity(ProviderConfig p) {
        ProviderEntity e = new ProviderEntity(p.id, p.name, p.type, p.baseUrl);
        e.orgId = p.orgId;
        e.customHeaders = p.customHeaders;
        e.timeoutSeconds = p.timeoutSeconds;
        e.isEnabled = p.isEnabled;
        e.isLocal = p.isLocal;
        e.useAiDetection = p.useAiDetection;
        e.customBodyTemplate = p.customBodyTemplate;
        e.customJsonPath = p.customJsonPath;
        return e;
    }
}
