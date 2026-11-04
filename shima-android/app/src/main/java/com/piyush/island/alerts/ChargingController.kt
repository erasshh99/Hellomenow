package com.piyush.island.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Reports the state of the battery: charging (and on what), low battery and battery full.
 */
class ChargingController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private var receiver: BroadcastReceiver? = null
    private var lastState: String? = null

    var charging: Boolean = false
        private set

    var batteryLevel: Int = 0
        private set

    fun start() {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    publish(intent)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        appContext.registerReceiver(receiver, filter)
    }

    fun stop() {
        receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        receiver = null
    }

    private fun publish(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else 0

        batteryLevel = percent
        charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING

        if (charging) {
            val kind = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> ""
            }
            show(
                state = "charging",
                card = IslandCard(
                    type = AlertType.CHARGING,
                    title = kind.ifEmpty { "Charging" },
                    subtitle = "$percent%",
                    icon = "battery",
                    color = 0xFF2E9E5B.toInt(),
                    accent = 0xFF8AD5C6.toInt(),
                    progress = percent / 100f,
                    autoDismissAfterMs = 5_000,
                ),
            )
            return
        }

        when {
            percent in 1..LOW_BATTERY_PERCENT -> show(
                state = "low",
                card = IslandCard(
                    type = AlertType.LOW_BATTERY,
                    title = "Low battery",
                    subtitle = "$percent%",
                    icon = "battery_low",
                    color = 0xFFE24A3A.toInt(),
                    accent = 0xFFFF9A8B.toInt(),
                    progress = percent / 100f,
                    autoDismissAfterMs = 6_000,
                ),
            )

            percent >= FULL_BATTERY_PERCENT -> show(
                state = "full",
                card = IslandCard(
                    type = AlertType.BATTERY_FULL,
                    title = "Battery full",
                    subtitle = "$percent%",
                    icon = "battery_full",
                    color = 0xFF2E9E5B.toInt(),
                    accent = 0xFF8AD5C6.toInt(),
                    progress = 1f,
                    autoDismissAfterMs = 6_000,
                ),
            )

            else -> clear()
        }
    }

    /** A card is only sent when the state actually changed, so the island does not flicker. */
    private fun show(state: String, card: IslandCard) {
        if (lastState == state) return
        lastState = state
        onCard(card)
    }

    private fun clear() {
        if (lastState == null) return
        lastState = null
        onCard(null)
    }

    companion object {
        const val LOW_BATTERY_PERCENT = 15
        const val FULL_BATTERY_PERCENT = 100
    }
}
