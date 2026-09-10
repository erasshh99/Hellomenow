package com.example.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.domain.model.MediaAsset;
import com.example.domain.model.MediaJob;
import java.util.List;

public interface MediaRepository {
    LiveData<List<MediaAsset>> getAllMediaAssets();
    LiveData<List<MediaJob>> getActiveJobs();
    List<MediaJob> getPendingJobsSync();
    void insertMediaJob(MediaJob job);
    void updateMediaJob(MediaJob job);
    MediaJob getJobByIdSync(String jobId);
    void insertMediaAsset(MediaAsset asset);
    void deleteMediaAsset(String assetId);
}
