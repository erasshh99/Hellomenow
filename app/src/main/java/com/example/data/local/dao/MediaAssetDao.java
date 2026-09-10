package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.data.local.entity.MediaAssetEntity;
import java.util.List;

@Dao
public interface MediaAssetDao {
    @Query("SELECT * FROM media_assets ORDER BY createdAt DESC")
    LiveData<List<MediaAssetEntity>> getAllAssets();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MediaAssetEntity asset);

    @Query("DELETE FROM media_assets WHERE id = :id")
    void deleteById(String id);
}
