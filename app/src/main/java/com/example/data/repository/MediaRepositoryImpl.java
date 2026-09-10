package com.example.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.data.local.dao.MediaAssetDao;
import com.example.data.local.dao.MediaJobDao;
import com.example.data.local.entity.MediaAssetEntity;
import com.example.data.local.entity.MediaJobEntity;
import com.example.domain.model.MediaAsset;
import com.example.domain.model.MediaJob;
import com.example.domain.repository.MediaRepository;
import com.example.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public class MediaRepositoryImpl implements MediaRepository {
    private final MediaJobDao mediaJobDao;
    private final MediaAssetDao mediaAssetDao;
    private final AppExecutors executors;

    public MediaRepositoryImpl(MediaJobDao mediaJobDao, MediaAssetDao mediaAssetDao, AppExecutors executors) {
        this.mediaJobDao = mediaJobDao;
        this.mediaAssetDao = mediaAssetDao;
        this.executors = executors;
    }

    @Override
    public LiveData<List<MediaAsset>> getAllMediaAssets() {
        return Transformations.map(mediaAssetDao.getAllAssets(), entities -> {
            List<MediaAsset> list = new ArrayList<>();
            if (entities != null) {
                for (MediaAssetEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public LiveData<List<MediaJob>> getActiveJobs() {
        return Transformations.map(mediaJobDao.getActiveJobs(), entities -> {
            List<MediaJob> list = new ArrayList<>();
            if (entities != null) {
                for (MediaJobEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public List<MediaJob> getPendingJobsSync() {
        List<MediaJobEntity> entities = mediaJobDao.getPendingJobsSync();
        List<MediaJob> list = new ArrayList<>();
        if (entities != null) {
            for (MediaJobEntity e : entities) {
                list.add(toDomain(e));
            }
        }
        return list;
    }

    @Override
    public void insertMediaJob(MediaJob job) {
        executors.diskIO().execute(() -> mediaJobDao.insert(toEntity(job)));
    }

    @Override
    public void updateMediaJob(MediaJob job) {
        executors.diskIO().execute(() -> mediaJobDao.update(toEntity(job)));
    }

    @Override
    public MediaJob getJobByIdSync(String jobId) {
        MediaJobEntity entity = mediaJobDao.getJobByIdSync(jobId);
        return entity != null ? toDomain(entity) : null;
    }

    @Override
    public void insertMediaAsset(MediaAsset asset) {
        executors.diskIO().execute(() -> mediaAssetDao.insert(toEntity(asset)));
    }

    @Override
    public void deleteMediaAsset(String assetId) {
        executors.diskIO().execute(() -> mediaAssetDao.deleteById(assetId));
    }

    private MediaJob toDomain(MediaJobEntity e) {
        MediaJob j = new MediaJob();
        j.jobId = e.jobId;
        j.conversationId = e.conversationId;
        j.messageId = e.messageId;
        j.type = e.type;
        j.providerId = e.providerId;
        j.modelId = e.modelId;
        j.prompt = e.prompt;
        j.status = e.status;
        j.progressPercent = e.progressPercent;
        j.pollUrl = e.pollUrl;
        j.resultUrl = e.resultUrl;
        j.localFilePath = e.localFilePath;
        j.errorMessage = e.errorMessage;
        j.createdAt = e.createdAt;
        j.updatedAt = e.updatedAt;
        return j;
    }

    private MediaJobEntity toEntity(MediaJob j) {
        MediaJobEntity e = new MediaJobEntity(j.jobId);
        e.conversationId = j.conversationId;
        e.messageId = j.messageId;
        e.type = j.type;
        e.providerId = j.providerId;
        e.modelId = j.modelId;
        e.prompt = j.prompt;
        e.status = j.status;
        e.progressPercent = j.progressPercent;
        e.pollUrl = j.pollUrl;
        e.resultUrl = j.resultUrl;
        e.localFilePath = j.localFilePath;
        e.errorMessage = j.errorMessage;
        e.createdAt = j.createdAt;
        e.updatedAt = j.updatedAt;
        return e;
    }

    private MediaAsset toDomain(MediaAssetEntity e) {
        MediaAsset a = new MediaAsset();
        a.id = e.id;
        a.conversationId = e.conversationId;
        a.messageId = e.messageId;
        a.type = e.type;
        a.prompt = e.prompt;
        a.modelId = e.modelId;
        a.providerId = e.providerId;
        a.localUri = e.localUri;
        a.remoteUrl = e.remoteUrl;
        a.mimeType = e.mimeType;
        a.width = e.width;
        a.height = e.height;
        a.durationMs = e.durationMs;
        a.sizeBytes = e.sizeBytes;
        a.createdAt = e.createdAt;
        return a;
    }

    private MediaAssetEntity toEntity(MediaAsset a) {
        MediaAssetEntity e = new MediaAssetEntity(a.id);
        e.conversationId = a.conversationId;
        e.messageId = a.messageId;
        e.type = a.type;
        e.prompt = a.prompt;
        e.modelId = a.modelId;
        e.providerId = a.providerId;
        e.localUri = a.localUri;
        e.remoteUrl = a.remoteUrl;
        e.mimeType = a.mimeType;
        e.width = a.width;
        e.height = a.height;
        e.durationMs = a.durationMs;
        e.sizeBytes = a.sizeBytes;
        e.createdAt = a.createdAt;
        return e;
    }
}
