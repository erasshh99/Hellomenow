package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.MediaJobEntity;
import java.util.List;

@Dao
public interface MediaJobDao {
    @Query("SELECT * FROM media_jobs WHERE status IN ('QUEUED', 'PROCESSING') ORDER BY createdAt DESC")
    LiveData<List<MediaJobEntity>> getActiveJobs();

    @Query("SELECT * FROM media_jobs WHERE status IN ('QUEUED', 'PROCESSING')")
    List<MediaJobEntity> getPendingJobsSync();

    @Query("SELECT * FROM media_jobs WHERE jobId = :jobId LIMIT 1")
    MediaJobEntity getJobByIdSync(String jobId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MediaJobEntity job);

    @Update
    void update(MediaJobEntity job);

    @Query("DELETE FROM media_jobs WHERE jobId = :jobId")
    void deleteById(String jobId);
}
