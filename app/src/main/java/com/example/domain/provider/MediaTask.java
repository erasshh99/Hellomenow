package com.example.domain.provider;

import java.io.Serializable;

public class MediaTask implements Serializable {
    public boolean isAsyncJob;
    public String directMediaUrl;
    public String directUrl;
    public String jobId;
    public String pollUrl;
    public String format = "url"; // "url" or "b64_json"

    public MediaTask() {}

    public boolean isDirectUrl() {
        return !isAsyncJob && (directMediaUrl != null || directUrl != null);
    }

    public boolean isAsyncJob() {
        return isAsyncJob;
    }

    public static MediaTask direct(String url) {
        MediaTask task = new MediaTask();
        task.isAsyncJob = false;
        task.directMediaUrl = url;
        task.directUrl = url;
        return task;
    }

    public static MediaTask async(String jobId, String pollUrl) {
        MediaTask task = new MediaTask();
        task.isAsyncJob = true;
        task.jobId = jobId;
        task.pollUrl = pollUrl;
        return task;
    }
}
