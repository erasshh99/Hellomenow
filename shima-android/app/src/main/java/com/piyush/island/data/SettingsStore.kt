package com.piyush.island.data

import android.content.Context
import android.content.SharedPreferences
import com.piyush.island.model.AlertConfig
import com.piyush.island.model.AlertType
import com.piyush.island.model.CloseMode
import com.piyush.island.model.CutoutSide
import com.piyush.island.model.Gesture
import com.piyush.island.model.GestureAction
import com.piyush.island.model.GestureBindings
import com.piyush.island.model.IslandAnimation
import com.piyush.island.model.IslandShape
import com.piyush.island.model.IslandStyle
import com.piyush.island.model.PaletteId
import com.piyush.island.model.Rule
import com.piyush.island.model.RuleCondition
import com.piyush.island.model.ShimaSettings
import com.piyush.island.model.SystemActionKind
import com.piyush.island.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The single source of truth for the user's configuration.
 *
 * Everything lives in one [SharedPreferences] file so a backup is a simple copy, and the whole
 * snapshot is exposed as a [StateFlow] that the overlay and the UI observe.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The Pro entitlement lives in its own file, which the backup rules keep out of the Android
     * cloud backup and out of the device transfer. The entitlement belongs to the Play purchase
     * on this device, so it must not travel to another one.
     */
    private val proPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PRO_PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ShimaSettings> = _settings.asStateFlow()

    val current: ShimaSettings get() = _settings.value

    // ---------------------------------------------------------------- top level toggles

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setKeepAlive(enabled: Boolean) = update { it.copy(keepAlive = enabled) }

    fun setAutoBackup(enabled: Boolean) = update { it.copy(autoBackup = enabled) }

    fun setOnboardingDone(done: Boolean) = update { it.copy(onboardingDone = done) }

    fun setProUnlocked(unlocked: Boolean) {
        proPrefs.edit().putBoolean(KEY_PRO, unlocked).apply()
        update { it.copy(proUnlocked = unlocked) }
    }

    // ---------------------------------------------------------------- style

    fun updateStyle(transform: (IslandStyle) -> IslandStyle) = update {
        it.copy(style = transform(it.style))
    }

    fun setPalette(palette: PaletteId) = updateStyle { it.copy(palette = palette) }

    fun setThemeMode(mode: ThemeMode) = updateStyle { it.copy(themeMode = mode) }

    // ---------------------------------------------------------------- alerts

    fun setAlertEnabled(type: AlertType, enabled: Boolean) = update {
        it.copy(alerts = it.alerts + (type to it.configFor(type).copy(enabled = enabled)))
    }

    fun updateAlert(type: AlertType, transform: (AlertConfig) -> AlertConfig) = update {
        it.copy(alerts = it.alerts + (type to transform(it.configFor(type))))
    }

    fun setAlertPackages(type: AlertType, packages: Set<String>) =
        updateAlert(type) { it.copy(packages = packages) }

    // ---------------------------------------------------------------- gestures

    fun setGesture(surface: com.piyush.island.model.Surface, gesture: Gesture, action: GestureAction) =
        update { it.copy(gestures = it.gestures.with(surface, gesture, action)) }

    // ---------------------------------------------------------------- rules

    fun upsertRule(rule: Rule) = update { current ->
        val index = current.rules.indexOfFirst { it.id == rule.id }
        val rules = if (index >= 0) {
            current.rules.toMutableList().apply { set(index, rule) }
        } else {
            current.rules + rule
        }
        current.copy(rules = rules)
    }

    fun deleteRule(id: String) = update { it.copy(rules = it.rules.filterNot { r -> r.id == id }) }

    // ---------------------------------------------------------------- app row and decks

    fun setAppRow(packages: List<String>) = update { it.copy(appRow = packages.take(MAX_APP_ROW)) }

    fun toggleAppRow(packageName: String) = update { current ->
        val row = current.appRow.toMutableList()
        if (!row.remove(packageName)) {
            if (row.size >= MAX_APP_ROW) return@update current
            row.add(packageName)
        }
        current.copy(appRow = row)
    }

    fun setDeck(key: String, cards: List<String>) = update {
        it.copy(decks = it.decks + (key to cards))
    }

    // ---------------------------------------------------------------- persistence

    /** Applies a change to the whole snapshot and writes it to disk. */
    fun update(transform: (ShimaSettings) -> ShimaSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        persist(next)
    }

    private fun persist(settings: ShimaSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putBoolean(KEY_KEEP_ALIVE, settings.keepAlive)
            putBoolean(KEY_AUTO_BACKUP, settings.autoBackup)
            putBoolean(KEY_ONBOARDING, settings.onboardingDone)
            putString(KEY_STYLE, StyleJson.encode(settings.style))
            putString(KEY_ALERTS, AlertsJson.encode(settings.alerts))
            putString(KEY_GESTURES, GesturesJson.encode(settings.gestures))
            putString(KEY_RULES, RulesJson.encode(settings.rules))
            putString(KEY_APP_ROW, JsonUtil.stringArray(settings.appRow))
            putString(KEY_DECKS, DecksJson.encode(settings.decks))
        }.apply()
    }

    private fun load(): ShimaSettings {
        val defaults = ShimaSettings()
        val style = prefs.getString(KEY_STYLE, null)?.let { StyleJson.decode(it) } ?: defaults.style
        val alerts = prefs.getString(KEY_ALERTS, null)?.let { AlertsJson.decode(it) } ?: defaults.alerts
        return ShimaSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            keepAlive = prefs.getBoolean(KEY_KEEP_ALIVE, false),
            autoBackup = prefs.getBoolean(KEY_AUTO_BACKUP, false),
            style = style,
            alerts = alerts,
            gestures = prefs.getString(KEY_GESTURES, null)?.let { GesturesJson.decode(it) } ?: defaults.gestures,
            rules = prefs.getString(KEY_RULES, null)?.let { RulesJson.decode(it) } ?: emptyList(),
            appRow = prefs.getString(KEY_APP_ROW, null)?.let { JsonUtil.stringList(it) } ?: emptyList(),
            decks = prefs.getString(KEY_DECKS, null)?.let { DecksJson.decode(it) } ?: emptyMap(),
            proUnlocked = proPrefs.getBoolean(KEY_PRO, false),
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false),
        )
    }

    companion object {
        const val PREFS_NAME = "shima_settings"

        /** The Pro entitlement, kept out of the backup. */
        const val PRO_PREFS_NAME = "island_pro"
        const val MAX_APP_ROW = 10

        private const val KEY_ENABLED = "enabled"
        private const val KEY_KEEP_ALIVE = "keep_alive"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_PRO = "pro_unlocked"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_STYLE = "style"
        private const val KEY_ALERTS = "alerts"
        private const val KEY_GESTURES = "gestures"
        private const val KEY_RULES = "rules"
        private const val KEY_APP_ROW = "app_row"
        private const val KEY_DECKS = "decks"
    }
}

/* ------------------------------------------------------------------ JSON helpers */

internal object JsonUtil {
    fun stringArray(values: List<String>): String {
        val array = JSONArray()
        values.forEach { array.put(it) }
        return array.toString()
    }

    fun stringList(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun longSet(raw: String?): Set<Long> {
        if (raw.isNullOrEmpty()) return emptySet()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optLong(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun stringSet(values: Set<String>): String = stringArray(values.toList())

    fun stringSetOf(raw: String?): Set<String> = stringList(raw).toSet()
}

internal object StyleJson {
    fun encode(style: IslandStyle): String = JSONObject().apply {
        put("width", style.widthDp)
        put("height", style.heightDp)
        put("offsetX", style.offsetXDp)
        put("offsetY", style.offsetYDp)
        put("radius", style.cornerRadiusDp)
        put("shape", style.shape.id)
        put("transparency", style.transparency)
        put("outlineWidth", style.outlineWidthDp)
        put("outlineColor", style.outlineColor)
        put("shadowRadius", style.shadowRadiusDp)
        put("shadowColor", style.shadowColor)
        put("expandedTransparency", style.expandedTransparency)
        put("expandedOutlineWidth", style.expandedOutlineWidthDp)
        put("expandedOutlineColor", style.expandedOutlineColor)
        put("expandedShadowRadius", style.expandedShadowRadiusDp)
        put("animation", style.animation.id)
        put("cutoutSide", style.cutoutSide.id)
        put("palette", style.palette.id)
        put("themeMode", style.themeMode.id)
        put("customBackground", style.customBackground)
        put("customForeground", style.customForeground)
        put("companionEnabled", style.companionEnabled)
        put("companionColor", style.companionColor)
        put("fitToCutout", style.fitToCutout)
        put("hideInFullscreen", style.hideInFullscreen)
        put("hiddenPackages", JsonUtil.stringSet(style.hiddenPackages))
        put("showOnLockScreen", style.showOnLockScreen)
    }.toString()

    fun decode(raw: String): IslandStyle? = try {
        val json = JSONObject(raw)
        IslandStyle(
            widthDp = json.optInt("width", 220),
            heightDp = json.optInt("height", 36),
            offsetXDp = json.optInt("offsetX", 0),
            offsetYDp = json.optInt("offsetY", 0),
            cornerRadiusDp = json.optInt("radius", 18),
            shape = IslandShape.fromId(json.optString("shape")),
            transparency = json.optInt("transparency", 0),
            outlineWidthDp = json.optInt("outlineWidth", 0),
            outlineColor = json.optInt("outlineColor", 0x33FFFFFF),
            shadowRadiusDp = json.optInt("shadowRadius", 0),
            shadowColor = json.optInt("shadowColor", 0x40000000),
            expandedTransparency = json.optInt("expandedTransparency", 0),
            expandedOutlineWidthDp = json.optInt("expandedOutlineWidth", 0),
            expandedOutlineColor = json.optInt("expandedOutlineColor", 0x33FFFFFF),
            expandedShadowRadiusDp = json.optInt("expandedShadowRadius", 12),
            animation = IslandAnimation.fromId(json.optString("animation")),
            cutoutSide = CutoutSide.fromId(json.optString("cutoutSide")),
            palette = PaletteId.fromId(json.optString("palette")),
            themeMode = ThemeMode.fromId(json.optString("themeMode")),
            customBackground = json.optInt("customBackground", 0),
            customForeground = json.optInt("customForeground", 0),
            companionEnabled = json.optBoolean("companionEnabled", false),
            companionColor = json.optInt("companionColor", 0xFF2E9E5B.toInt()),
            fitToCutout = json.optBoolean("fitToCutout", true),
            hideInFullscreen = json.optBoolean("hideInFullscreen", true),
            hiddenPackages = JsonUtil.stringSetOf(json.optString("hiddenPackages")).toSet(),
            showOnLockScreen = json.optBoolean("showOnLockScreen", true),
        )
    } catch (_: Exception) {
        null
    }
}

internal object AlertsJson {
    fun encode(alerts: Map<AlertType, AlertConfig>): String = JSONObject().apply {
        alerts.forEach { (type, config) ->
            put(type.id, JSONObject().apply {
                put("enabled", config.enabled)
                put("icon", config.icon)
                put("color", config.color)
                put("animation", config.animation.id)
                put("duration", config.durationMs)
                put("closeMode", config.closeMode.id)
                put("autoOpen", config.autoOpen)
                put("miniAlert", config.miniAlert)
                put("packages", JsonUtil.stringSet(config.packages))
                put("template", config.template)
                put("accent", config.accentColor)
                put("typeface", config.typeface)
            })
        }
    }.toString()

    fun decode(raw: String): Map<AlertType, AlertConfig> = try {
        val json = JSONObject(raw)
        val result = mutableMapOf<AlertType, AlertConfig>()
        AlertType.entries.forEach { type ->
            val item = json.optJSONObject(type.id) ?: return@forEach
            result[type] = AlertConfig(
                enabled = item.optBoolean("enabled", false),
                icon = item.optString("icon", type.defaultIcon),
                color = item.optLong("color", type.defaultColor),
                animation = IslandAnimation.fromId(item.optString("animation")),
                durationMs = item.optInt("duration", 6_000),
                closeMode = CloseMode.fromId(item.optString("closeMode")),
                autoOpen = item.optBoolean("autoOpen", false),
                miniAlert = item.optBoolean("miniAlert", true),
                packages = JsonUtil.stringSetOf(item.optString("packages")),
                template = item.optString("template", ""),
                accentColor = item.optLong("accent", 0xFF8AD5C6),
                typeface = item.optString("typeface", ""),
            )
        }
        result
    } catch (_: Exception) {
        emptyMap()
    }
}

internal object GesturesJson {
    fun encode(bindings: GestureBindings): String = JSONObject().apply {
        put("collapsed", encodeMap(bindings.collapsed))
        put("expanded", encodeMap(bindings.expanded))
    }.toString()

    private fun encodeMap(map: Map<Gesture, GestureAction>): String {
        val json = JSONObject()
        map.forEach { (gesture, action) -> json.put(gesture.id, encodeAction(action)) }
        return json.toString()
    }

    private fun encodeAction(action: GestureAction): String = when (action) {
        is GestureAction.None -> "none"
        is GestureAction.LaunchApp -> "app:${action.packageName}"
        is GestureAction.OpenWidget -> "widget:${action.appWidgetId}"
        is GestureAction.SystemAction -> "system:${action.action.id}"
        is GestureAction.TaskerTask -> "tasker:${action.packageName}:${action.taskName}"
    }

    fun decode(raw: String): GestureBindings = try {
        val json = JSONObject(raw)
        GestureBindings(
            collapsed = decodeMap(json.optJSONObject("collapsed")),
            expanded = decodeMap(json.optJSONObject("expanded")),
        )
    } catch (_: Exception) {
        GestureBindings()
    }

    private fun decodeMap(json: JSONObject?): Map<Gesture, GestureAction> {
        if (json == null) return emptyMap()
        val map = mutableMapOf<Gesture, GestureAction>()
        json.keys().forEach { key ->
            val gesture = Gesture.fromId(key) ?: return@forEach
            map[gesture] = decodeAction(json.optString(key))
        }
        return map
    }

    private fun decodeAction(raw: String): GestureAction {
        val parts = raw.split(":")
        return when (parts.firstOrNull()) {
            "app" -> GestureAction.LaunchApp(parts.getOrNull(1).orEmpty(), parts.getOrNull(1).orEmpty())
            "widget" -> GestureAction.OpenWidget(parts.getOrNull(1)?.toIntOrNull() ?: 0, "")
            "system" -> SystemActionKind.fromId(parts.getOrNull(1))
                ?.let { GestureAction.SystemAction(it) }
                ?: GestureAction.None
            "tasker" -> GestureAction.TaskerTask(
                parts.getOrNull(1).orEmpty(),
                parts.drop(2).joinToString(":"),
            )
            else -> GestureAction.None
        }
    }
}

internal object RulesJson {
    fun encode(rules: List<Rule>): String = JSONArray().apply {
        rules.forEach { rule ->
            put(JSONObject().apply {
                put("id", rule.id)
                put("gesture", rule.gesture.id)
                put("conditions", JsonUtil.stringSet(rule.conditions.map { it.id }.toSet()))
                put("action", encodeAction(rule.action))
                put("requireUnlock", rule.requireUnlock)
                put("enabled", rule.enabled)
            })
        }
    }.toString()

    private fun encodeAction(action: GestureAction): String = when (action) {
        is GestureAction.None -> "none"
        is GestureAction.LaunchApp -> "app:${action.packageName}"
        is GestureAction.OpenWidget -> "widget:${action.appWidgetId}"
        is GestureAction.SystemAction -> "system:${action.action.id}"
        is GestureAction.TaskerTask -> "tasker:${action.packageName}:${action.taskName}"
    }

    fun decode(raw: String): List<Rule> = try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val gesture = Gesture.fromId(json.optString("gesture")) ?: return@mapNotNull null
            val conditions = JsonUtil.stringList(json.optString("conditions"))
                .mapNotNull { RuleCondition.fromId(it) }
                .toSet()
            Rule(
                id = json.optString("id"),
                gesture = gesture,
                conditions = conditions,
                action = decodeAction(json.optString("action")),
                requireUnlock = json.optBoolean("requireUnlock", true),
                enabled = json.optBoolean("enabled", true),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun decodeAction(raw: String): GestureAction {
        val parts = raw.split(":")
        return when (parts.firstOrNull()) {
            "app" -> GestureAction.LaunchApp(parts.getOrNull(1).orEmpty(), parts.getOrNull(1).orEmpty())
            "widget" -> GestureAction.OpenWidget(parts.getOrNull(1)?.toIntOrNull() ?: 0, "")
            "system" -> SystemActionKind.fromId(parts.getOrNull(1))
                ?.let { GestureAction.SystemAction(it) }
                ?: GestureAction.None
            "tasker" -> GestureAction.TaskerTask(
                parts.getOrNull(1).orEmpty(),
                parts.drop(2).joinToString(":"),
            )
            else -> GestureAction.None
        }
    }
}

internal object DecksJson {
    fun encode(decks: Map<String, List<String>>): String = JSONObject().apply {
        decks.forEach { (key, cards) -> put(key, JsonUtil.stringArray(cards)) }
    }.toString()

    fun decode(raw: String): Map<String, List<String>> = try {
        val json = JSONObject(raw)
        buildMap {
            json.keys().forEach { key -> put(key, JsonUtil.stringList(json.optString(key))) }
        }
    } catch (_: Exception) {
        emptyMap()
    }
}
