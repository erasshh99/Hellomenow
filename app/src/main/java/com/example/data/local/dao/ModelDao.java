package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.ModelEntity;
import java.util.List;

@Dao
public interface ModelDao {
    @Query("SELECT * FROM models ORDER BY isFavorite DESC, displayName ASC")
    LiveData<List<ModelEntity>> getAllModels();

    @Query("SELECT * FROM models WHERE providerId = :providerId ORDER BY isFavorite DESC, displayName ASC")
    LiveData<List<ModelEntity>> getModelsForProvider(String providerId);

    @Query("SELECT * FROM models WHERE providerId = :providerId ORDER BY displayName ASC")
    List<ModelEntity> getModelsForProviderSync(String providerId);

    @Query("SELECT * FROM models WHERE id = :id LIMIT 1")
    ModelEntity getModelByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ModelEntity model);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ModelEntity> models);

    @Update
    void update(ModelEntity model);

    @Query("DELETE FROM models WHERE providerId = :providerId")
    void deleteModelsForProvider(String providerId);

    @Query("DELETE FROM models WHERE id = :id")
    void deleteById(String id);
}
