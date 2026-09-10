package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.ProviderEntity;
import java.util.List;

@Dao
public interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY name ASC")
    LiveData<List<ProviderEntity>> getAllProviders();

    @Query("SELECT * FROM providers ORDER BY name ASC")
    List<ProviderEntity> getAllProvidersSync();

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    ProviderEntity getProviderByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProviderEntity provider);

    @Update
    void update(ProviderEntity provider);

    @Query("DELETE FROM providers WHERE id = :id")
    void deleteById(String id);
}
