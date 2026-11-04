package com.piyush.island.alerts

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * The small system state alerts: battery saver, Do Not Disturb, USB, no signal, alarms, rotation
 * lock, screen recording and uninstalled apps.
 *
 * The states that Android does not announce with a broadcast (an alarm that is set, a screen
 * recording that started, a rotation lock that changed) are polled instead.
 */
class SystemStateController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val handler = Handler(Looper.getMainLooper())

    private var receiver: BroadcastReceiver? = null
    private var packageReceiver: BroadcastReceiver? = null
    private var current: SystemAlert? = null
    private var lastSignal = true
    private var removedPackage: String? = null

    /** One line about the state of the phone. */
    private data class SystemAlert(
        val type: AlertType,
        val title: String,
        val subtitle: String,
        val icon: String,
        val color: Int,
    )

    private val poll = object : Runnable {
        override fun run() {
            publish()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> publish()
                    "android.hardware.usb.action.USB_STATE" -> publish()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction("android.hardware.usb.action.USB_STATE")
        }
        runCatching { appContext.registerReceiver(receiver, filter) }

        // The package broadcasts carry a data scheme, so they need their own filter.
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_PACKAGE_REMOVED) return
                removedPackage = intent.data?.schemeSpecificPart
                publish()
            }
        }
        val packageFilter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        runCatching { appContext.registerReceiver(packageReceiver, packageFilter) }

        telephony?.listen(object : android.telephony.PhoneStateListener() {
            override fun onServiceStateChanged(state: android.telephony.ServiceState?) {
                val inService = state?.state == android.telephony.ServiceState.STATE_IN_SERVICE
                if (inService != lastSignal) {
                    lastSignal = inService
                    publish()
                }
            }
        }, android.telephony.PhoneStateListener.LISTEN_SERVICE_STATE)

        handler.post(poll)
    }

    fun stop() {
        handler.removeCallbacks(poll)
        receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        receiver = null
        packageReceiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        packageReceiver = null
        current = null
    }

    fun publish() {
        val alert = computeAlert()
        if (alert == current) return
        current = alert
        if (alert == null) {
            onCard(null)
            return
        }
        onCard(
            IslandCard(
                type = alert.type,
                title = alert.title,
                subtitle = alert.subtitle,
                icon = alert.icon,
                color = alert.color,
                autoDismissAfterMs = 4_000,
            )
        )
    }

    /** The state that matters most right now. */
    private fun computeAlert(): SystemAlert? {
        val saver = powerManager?.isPowerSaveMode == true
        val dnd = notificationManager?.currentInterruptionFilter ==
            NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            notificationManager?.currentInterruptionFilter ==
            NotificationManager.INTERRUPTION_FILTER_NONE
        val usb = usbConnected()
        val rotationLocked = Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1,
        ) == 0
        val alarm = nextAlarm()
        val recording = screenRecordingActive()

        return when {
            saver -> SystemAlert(
                AlertType.BATTERY_SAVER,
                "Battery Saver on",
                "Tap for Battery Saver settings",
                "saver",
                0xFF6E8B3D.toInt(),
            )

            dnd -> SystemAlert(AlertType.DND, "Do Not Disturb", "Notifications are paused", "dnd", 0xFF6C5CE7.toInt())
            usb -> SystemAlert(AlertType.USB, "USB", "Connected", "usb", 0xFF3E7BD9.toInt())
            !lastSignal -> SystemAlert(
                AlertType.NO_SIGNAL,
                "No signal",
                "Waiting for the network",
                "signal",
                0xFF8A8F98.toInt(),
            )

            rotationLocked -> SystemAlert(
                AlertType.ROTATION_LOCK,
                "Rotation locked",
                "The screen will not turn",
                "rotate",
                0xFF4E8A8A.toInt(),
            )

            recording -> SystemAlert(
                AlertType.SCREEN_RECORDING,
                "Screen recording",
                "Your screen is being recorded",
                "record",
                0xFFD1495B.toInt(),
            )

            alarm != null -> SystemAlert(AlertType.ALARM, "Alarm", alarm, "alarm", 0xFFE0674B.toInt())
            removedPackage != null -> SystemAlert(
                AlertType.UNINSTALLED_APP,
                "App uninstalled",
                removedPackage.orEmpty(),
                "app",
                0xFF5D6470.toInt(),
            ).also { removedPackage = null }

            else -> null
        }
    }

    /** True when a cable is plugged in. */
    private fun usbConnected(): Boolean {
        val intent = appContext.registerReceiver(
            null,
            IntentFilter("android.hardware.usb.action.USB_STATE"),
        ) ?: return false
        return intent.getBooleanExtra("connected", false)
    }

    /** The time of the next alarm, when one is set. */
    fun nextAlarm(): String? {
        val alarms = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        val clock = alarms.nextAlarmClock ?: return null
        val millis = clock.triggerTime
        return "at ${android.text.format.DateFormat.format("HH:mm", millis)}"
    }

    /**
     * True when a screen recording or projection is running.
     *
     * The platform keeps this behind a non public method, so it is asked for through reflection
     * and the answer is "no" when the system does not allow the call.
     */
    fun screenRecordingActive(): Boolean {
        val manager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as? android.media.projection.MediaProjectionManager ?: return false
        return runCatching {
            val method = android.media.projection.MediaProjectionManager::class.java
                .getMethod("isProjectionActive")
            method.invoke(manager) as? Boolean ?: false
        }.getOrDefault(false)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 10_000L
    }
}
