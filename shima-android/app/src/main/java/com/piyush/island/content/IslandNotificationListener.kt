package com.piyush.island.content

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.piyush.island.core.AppGraph
import com.piyush.island.island.CardAction
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Reads the notifications Shima is allowed to see. Two kinds matter:
 *
 *  - on-going notifications that carry a progress bar, which are the timers, rides and
 *    deliveries that show up as live cards;
 *  - the system's own "screenshot taken" notification.
 *
 * Nothing is stored: the text is read, turned into a card and dropped again.
 */
class IslandNotificationListener : NotificationListenerService() {

    companion object {
        /**
         * The picture of the last screenshot the system told us about. The card uses it for the
         * share, copy, edit and delete buttons.
         */
        @Volatile
        var lastScreenshotUri: android.net.Uri? = null
            private set
    }

    private var liveNotificationKey: String? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val packageName = notification.packageName ?: return
        if (packageName == applicationContext.packageName) return

        val extras = notification.notification?.extras ?: return
        val progress = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, -1)

        if (progress >= 0 && progressMax > 0 && notification.isOngoing) {
            showLive(notification, packageName, progress, progressMax)
            return
        }

        if (isScreenshotNotification(packageName, extras.getCharSequence(android.app.Notification.EXTRA_TITLE))) {
            showScreenshot(packageName, notification)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.key == liveNotificationKey) {
            liveNotificationKey = null
            AppGraph.alertEngine.offerFromNotification(AlertType.LIVE, null, null)
        }
    }

    private fun isScreenshotNotification(packageName: String, title: CharSequence?): Boolean {
        if (packageName != "com.android.systemui") return false
        val text = title?.toString()?.lowercase().orEmpty()
        return text.contains("screenshot") || text.contains("screen capture")
    }

    private fun showLive(
        notification: StatusBarNotification,
        packageName: String,
        progress: Int,
        progressMax: Int,
    ) {
        val extras = notification.notification?.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        liveNotificationKey = notification.key
        val card = IslandCard(
            type = AlertType.LIVE,
            title = title.ifEmpty { text },
            subtitle = text,
            icon = "timer",
            color = 0xFF2D8CF0.toInt(),
            accent = 0xFF8AD5C6.toInt(),
            progress = progress.toFloat() / progressMax,
            actions = listOf(
                CardAction("open", com.piyush.island.R.string.card_open_app, CardAction.Kind.OPEN),
            ),
            autoDismissAfterMs = 8_000,
        )
        AppGraph.alertEngine.offerFromNotification(AlertType.LIVE, card, packageName)
    }

    private fun showScreenshot(packageName: String, notification: StatusBarNotification) {
        lastScreenshotUri = latestScreenshot()
        val card = IslandCard(
            type = AlertType.SCREENSHOT,
            title = "Screenshot",
            subtitle = "Taken",
            icon = "shot",
            color = 0xFF4B6BFB.toInt(),
            accent = 0xFF8AD5C6.toInt(),
            actions = listOf(
                CardAction("share", com.piyush.island.R.string.card_share, CardAction.Kind.SHARE),
                CardAction("copy", com.piyush.island.R.string.card_copy, CardAction.Kind.COPY),
                CardAction("edit", com.piyush.island.R.string.card_edit, CardAction.Kind.EDIT),
                CardAction("delete", com.piyush.island.R.string.card_delete, CardAction.Kind.DELETE),
            ),
            autoDismissAfterMs = 6_000,
        )
        AppGraph.alertEngine.offerFromNotification(AlertType.SCREENSHOT, card, packageName)
    }

    /**
     * Finds the picture that was just added to the gallery. The system does not tell us the
     * address of a screenshot, so the newest picture is looked up and it has to be one that was
     * written a moment ago.
     */
    private fun latestScreenshot(): android.net.Uri? {
        val resolver = applicationContext.contentResolver
        val projection = arrayOf(
            android.provider.MediaStore.Images.Media._ID,
            android.provider.MediaStore.Images.Media.DATE_ADDED,
        )
        val selection = "${android.provider.MediaStore.Images.Media.DATE_ADDED} > ?"
        val since = System.currentTimeMillis() / 1000 - 15
        return runCatching {
            resolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(since.toString()),
                "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.getLong(0)
                android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
            }
        }.getOrNull()
    }
}
