package com.piyush.island.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.piyush.island.core.AppGraph

/**
 * Checks every few hours that the island is still running and, if the user has switched the
 * accessibility service off, tells them so in the app instead of silently disappearing.
 */
class WatchdogJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        AppGraph.install(applicationContext)
        val enabled = AppGraph.settings.current.enabled
        if (!enabled) return false

        val accessibilityOn = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().contains(applicationContext.packageName)

        if (!accessibilityOn) {
            AppGraph.islandState.hide()
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        const val JOB_ID = 0x5348494D
        private const val INTERVAL_MILLIS = 3 * 60 * 60 * 1000L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return
            val info = JobInfo.Builder(JOB_ID, ComponentName(context, WatchdogJob::class.java))
                .setPeriodic(INTERVAL_MILLIS)
                .setPersisted(true)
                .build()
            runCatching { scheduler.schedule(info) }
        }

        fun cancel(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return
            runCatching { scheduler.cancel(JOB_ID) }
        }
    }
}
