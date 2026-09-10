package com.example.data.provider.sse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ServerSentEventParser {

    public interface Callback {
        void onEvent(@Nullable String eventType, @NonNull String data);
        void onComment(@NonNull String comment);
        void onError(@NonNull Throwable throwable);
        void onClosed();
    }

    private volatile boolean isCancelled = false;

    public void cancel() {
        this.isCancelled = true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void parse(@NonNull InputStream inputStream, @NonNull Callback callback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder dataBuffer = new StringBuilder();
            String currentEvent = null;

            while (!isCancelled && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    // Empty line means dispatch current event
                    if (dataBuffer.length() > 0) {
                        String data = dataBuffer.toString().trim();
                        dataBuffer.setLength(0);
                        callback.onEvent(currentEvent, data);
                        currentEvent = null;
                    }
                    continue;
                }

                // Comment line (starts with colon, e.g. ": keep-alive", ": ping")
                if (line.startsWith(":")) {
                    callback.onComment(line.substring(1).trim());
                    continue;
                }

                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String piece = line.substring(5);
                    if (piece.startsWith(" ")) {
                        piece = piece.substring(1);
                    }
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append("\n");
                    }
                    dataBuffer.append(piece);
                } else if (line.startsWith("id:")) {
                    // Optional event id
                } else if (line.startsWith("retry:")) {
                    // Optional reconnect retry duration
                } else {
                    // Malformed or raw payload, treat as data continuation if non-empty
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append("\n");
                    }
                    dataBuffer.append(line);
                }
            }

            // Flush any remaining buffer if stream closed without trailing empty line
            if (dataBuffer.length() > 0 && !isCancelled) {
                callback.onEvent(currentEvent, dataBuffer.toString().trim());
            }

            callback.onClosed();
        } catch (IOException e) {
            if (!isCancelled) {
                callback.onError(e);
            }
        }
    }
}
