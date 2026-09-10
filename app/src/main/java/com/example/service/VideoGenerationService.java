package com.example.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.OmniChatApp;
import com.example.di.AppContainer;
import com.example.domain.model.MediaAsset;
import com.example.domain.model.MediaJob;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoGenerationService extends Service {
    private static final String CHANNEL_ID = "omnichat_video_gen";
    private static final int NOTIF_ID = 9001;

    private AppContainer container;
    private ScheduledFuture<?> pollingTask;

    @Override
    public void onCreate() {
        super.onCreate();
        container = ((OmniChatApp) getApplication()).getAppContainer();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification("OmniChat Video Engine", "Monitoring background video generation..."));
        startPolling();
        return START_STICKY;
    }

    private void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }

        container.getExecutors().networkIO().execute(this::checkAndPollJobs);
    }

    private void checkAndPollJobs() {
        List<MediaJob> pendingJobs = container.getMediaRepository().getPendingJobsSync();
        if (pendingJobs == null || pendingJobs.isEmpty()) {
            stopForeground(true);
            stopSelf();
            return;
        }

        for (MediaJob job : pendingJobs) {
            if (job.pollUrl != null && !job.pollUrl.isEmpty()) {
                pollJob(job);
            }
        }
    }

    private void pollJob(MediaJob job) {
        try {
            Request request = new Request.Builder().url(job.pollUrl).get().build();
            try (Response response = container.getHttpClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    // Check standard video status schema (OpenAI/Veo/Kling)
                    String status = json.has("status") ? json.get("status").getAsString().toUpperCase() : null;
                    if ("COMPLETED".equals(status) || "SUCCESS".equals(status)) {
                        job.status = MediaJob.STATUS_COMPLETED;
                        job.progressPercent = 100;
                        if (json.has("result_url")) {
                            job.resultUrl = json.get("result_url").getAsString();
                        } else if (json.has("output") && json.get("output").isJsonObject()) {
                            job.resultUrl = json.getAsJsonObject("output").get("url").getAsString();
                        }
                        container.getMediaRepository().updateMediaJob(job);

                        // Save media asset
                        MediaAsset asset = new MediaAsset();
                        asset.id = "asset_" + job.jobId;
                        asset.conversationId = job.conversationId;
                        asset.messageId = job.messageId;
                        asset.type = "VIDEO";
                        asset.prompt = job.prompt;
                        asset.modelId = job.modelId;
                        asset.providerId = job.providerId;
                        asset.remoteUrl = job.resultUrl;
                        container.getMediaRepository().insertMediaAsset(asset);

                    } else if ("FAILED".equals(status)) {
                        job.status = MediaJob.STATUS_FAILED;
                        job.errorMessage = json.has("error") ? json.get("error").getAsString() : "Video generation failed upstream.";
                        container.getMediaRepository().updateMediaJob(job);
                    } else {
                        // In progress
                        if (json.has("progress")) {
                            job.progressPercent = json.get("progress").getAsInt();
                        }
                        container.getMediaRepository().updateMediaJob(job);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore temporary network hiccup, keep polling
        }
    }

    private Notification buildNotification(String title, String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Video Generation Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
