package com.example.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.domain.model.ProviderConfig;
import java.util.List;

public interface ProviderRepository {
    LiveData<List<ProviderConfig>> getAllProviders();
    List<ProviderConfig> getAllProvidersSync();
    ProviderConfig getProviderByIdSync(String id);
    default ProviderConfig getProviderSync(String id) { return getProviderByIdSync(id); }
    void saveProvider(ProviderConfig provider);
    void deleteProvider(String id);
}
