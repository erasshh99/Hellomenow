package com.piyush.island.data

import com.piyush.island.model.AlertType
import com.piyush.island.model.ShimaSettings
import org.json.JSONObject

/**
 * Saves and restores the whole setup as one JSON file. The format is deliberately plain so a
 * backup can be inspected and edited by hand.
 */
object BackupManager {

    const val FILE_NAME = "shima-setup.json"
    const val FORMAT_VERSION = 1

    fun exportJson(settings: ShimaSettings): String = JSONObject().apply {
        put("format", FORMAT_VERSION)
        put("app", "shima")
        put("enabled", settings.enabled)
        put("keepAlive", settings.keepAlive)
        put("autoBackup", settings.autoBackup)
        // The Pro entitlement is deliberately left out: it belongs to the Play purchase on this
        // device, so a backup file must never be able to carry it to another one.
        put("style", StyleJson.encode(settings.style))
        put("alerts", AlertsJson.encode(settings.alerts))
        put("gestures", GesturesJson.encode(settings.gestures))
        put("rules", RulesJson.encode(settings.rules))
        put("appRow", JsonUtil.stringArray(settings.appRow))
        put("decks", DecksJson.encode(settings.decks))
    }.toString(2)

    /** Applies a backup. Returns false when the file cannot be read. */
    fun importJson(store: SettingsStore, raw: String): Boolean {
        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return false
        }
        if (json.optString("app") != "shima") return false

        val style = json.optString("style").takeIf { it.isNotEmpty() }?.let { StyleJson.decode(it) }
        val alerts = json.optString("alerts").takeIf { it.isNotEmpty() }?.let { AlertsJson.decode(it) }
        val gestures = json.optString("gestures").takeIf { it.isNotEmpty() }?.let { GesturesJson.decode(it) }
        val rules = json.optString("rules").takeIf { it.isNotEmpty() }?.let { RulesJson.decode(it) }
        val decks = json.optString("decks").takeIf { it.isNotEmpty() }?.let { DecksJson.decode(it) }

        store.update { current ->
            current.copy(
                enabled = json.optBoolean("enabled", current.enabled),
                keepAlive = json.optBoolean("keepAlive", current.keepAlive),
                autoBackup = json.optBoolean("autoBackup", current.autoBackup),
                style = style ?: current.style,
                alerts = alerts?.takeIf { it.isNotEmpty() } ?: current.alerts,
                gestures = gestures ?: current.gestures,
                rules = rules ?: current.rules,
                appRow = JsonUtil.stringList(json.optString("appRow")),
                decks = decks ?: current.decks,
            )
        }
        return true
    }

    /** A readable summary of what a backup contains, used by the restore screen. */
    fun describe(raw: String): String? {
        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return null
        }
        if (json.optString("app") != "shima") return null
        val alerts = json.optJSONObject("alerts")
        val enabledAlerts = AlertType.entries.count { alerts?.optJSONObject(it.id)?.optBoolean("enabled") == true }
        val rules = json.optJSONArray("rules")?.length() ?: 0
        val apps = JsonUtil.stringList(json.optString("appRow")).size
        return "$enabledAlerts alerts, $rules rules, $apps apps in the row"
    }
}
