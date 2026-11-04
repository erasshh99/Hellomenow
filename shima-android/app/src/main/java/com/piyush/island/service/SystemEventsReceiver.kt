package com.piyush.island.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.piyush.island.core.AppGraph

/**
 * Brings the island back after a reboot or an update.
 *
 * The overlay itself needs the accessibility service, which Android only keeps until the next
 * reboot, so after a restart Shima asks the user to turn it on again instead of pretending it
 * is running.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        AppGraph.install(appContext)
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                if (AppGraph.settings.current.keepAlive) {
                    KeepAliveService.start(appContext)
                }
                WatchdogJob.schedule(appContext)
            }
        }
    }
}
