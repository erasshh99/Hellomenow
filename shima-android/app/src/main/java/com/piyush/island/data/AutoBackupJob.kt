package com.piyush.island.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.piyush.island.core.AppGraph
import java.io.File

/**
 * Writes a daily copy of the settings into the app's private storage. It is the automatic half
 * of the backup feature and it never leaves the device.
 */
class AutoBackupJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        AppGraph.install(applicationContext)
        if (!AppGraph.settings.current.autoBackup) return false
        val target = File(filesDir, BACKUP_FILE)
        runCatching { target.writeText(BackupManager.exportJson(AppGraph.settings.current)) }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        const val JOB_ID = 0x53484942 // "SHIB"
        const val BACKUP_FILE = "shima_backup.json"
        private const val INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)
                as? android.app.job.JobScheduler ?: return
            val info = JobInfo.Builder(JOB_ID, ComponentName(context, AutoBackupJob::class.java))
                .setPeriodic(INTERVAL_MILLIS)
                .setPersisted(true)
                .build()
            runCatching { scheduler.schedule(info) }
        }

        fun cancel(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)
                as? android.app.job.JobScheduler ?: return
            runCatching { scheduler.cancel(JOB_ID) }
        }
    }
}
