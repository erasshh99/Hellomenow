package com.piyush.island.model

/**
 * Every alert the island can raise. The order is the order shown in the Alerts tab and it is
 * also the priority order used when more than one alert fires at the same time.
 */
enum class AlertType(
    val id: String,
    val titleRes: Int,
    val summaryRes: Int,
    val category: AlertCategory,
    val defaultIcon: String,
    val defaultColor: Long,
) {
    MEDIA("media", com.piyush.island.R.string.alert_media, com.piyush.island.R.string.alert_media_desc, AlertCategory.MEDIA, "music", 0xFF7C5CFF),
    CHARGING("charging", com.piyush.island.R.string.alert_charging, com.piyush.island.R.string.alert_charging_desc, AlertCategory.SYSTEM, "battery", 0xFF2E9E5B),
    FLASHLIGHT("flashlight", com.piyush.island.R.string.alert_flashlight, com.piyush.island.R.string.alert_flashlight_desc, AlertCategory.SYSTEM, "torch", 0xFFF2A33C),
    LIVE("live", com.piyush.island.R.string.alert_live, com.piyush.island.R.string.alert_live_desc, AlertCategory.SYSTEM, "timer", 0xFF2D8CF0),
    VOLUME("volume", com.piyush.island.R.string.alert_volume, com.piyush.island.R.string.alert_volume_desc, AlertCategory.SYSTEM, "volume", 0xFF3AAFA9),
    PRIVACY("privacy", com.piyush.island.R.string.alert_privacy, com.piyush.island.R.string.alert_privacy_desc, AlertCategory.SYSTEM, "mic", 0xFFD1495B),

    LOW_BATTERY("low_battery", com.piyush.island.R.string.alert_low_battery, com.piyush.island.R.string.alert_low_battery_desc, AlertCategory.BATTERY, "battery_low", 0xFFE24A3A),
    BATTERY_FULL("battery_full", com.piyush.island.R.string.alert_battery_full, com.piyush.island.R.string.alert_battery_full_desc, AlertCategory.BATTERY, "battery_full", 0xFF2E9E5B),
    BATTERY_SAVER("battery_saver", com.piyush.island.R.string.alert_battery_saver, com.piyush.island.R.string.alert_battery_saver_desc, AlertCategory.BATTERY, "saver", 0xFF6E8B3D),
    NO_SIGNAL("no_signal", com.piyush.island.R.string.alert_no_signal, com.piyush.island.R.string.alert_no_signal_desc, AlertCategory.SYSTEM, "signal", 0xFF8A8F98),
    USB("usb", com.piyush.island.R.string.alert_usb, com.piyush.island.R.string.alert_usb_desc, AlertCategory.SYSTEM, "usb", 0xFF3E7BD9),
    DND("dnd", com.piyush.island.R.string.alert_dnd, com.piyush.island.R.string.alert_dnd_desc, AlertCategory.SYSTEM, "dnd", 0xFF6C5CE7),
    ALARM("alarm", com.piyush.island.R.string.alert_alarm, com.piyush.island.R.string.alert_alarm_desc, AlertCategory.SYSTEM, "alarm", 0xFFE0674B),
    ROTATION_LOCK("rotation_lock", com.piyush.island.R.string.alert_rotation_lock, com.piyush.island.R.string.alert_rotation_lock_desc, AlertCategory.SYSTEM, "rotate", 0xFF4E8A8A),
    SCREEN_RECORDING("screen_recording", com.piyush.island.R.string.alert_screen_recording, com.piyush.island.R.string.alert_screen_recording_desc, AlertCategory.SYSTEM, "record", 0xFFD1495B),
    SCREENSHOT("screenshot", com.piyush.island.R.string.alert_screenshot, com.piyush.island.R.string.alert_screenshot_desc, AlertCategory.SYSTEM, "shot", 0xFF4B6BFB),
    UNINSTALLED_APP("uninstalled_app", com.piyush.island.R.string.alert_uninstalled_app, com.piyush.island.R.string.alert_uninstalled_app_desc, AlertCategory.SYSTEM, "app", 0xFF5D6470);

    companion object {
        fun fromId(id: String): AlertType? = entries.firstOrNull { it.id == id }
    }
}

enum class AlertCategory { MEDIA, BATTERY, SYSTEM }
