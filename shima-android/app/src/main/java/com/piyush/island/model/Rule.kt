package com.piyush.island.model

/** A condition that has to be true for a rule to fire. */
enum class RuleCondition(val id: String, val labelRes: Int) {
    CHARGING("charging", com.piyush.island.R.string.condition_charging),
    MEDIA_PLAYING("media_playing", com.piyush.island.R.string.condition_media_playing),
    DND("dnd", com.piyush.island.R.string.condition_dnd),
    SCREEN_ON("screen_on", com.piyush.island.R.string.condition_screen_on),
    LOCKED("locked", com.piyush.island.R.string.condition_locked),
    BATTERY_LOW("battery_low", com.piyush.island.R.string.condition_battery_low);

    companion object {
        fun fromId(id: String?): RuleCondition? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A rule changes what a gesture does while the phone is in a certain state, which is how the
 * original lets one gesture do different jobs.
 */
data class Rule(
    val id: String,
    val gesture: Gesture,
    val conditions: Set<RuleCondition> = emptySet(),
    val action: GestureAction = GestureAction.None,
    val requireUnlock: Boolean = true,
    val enabled: Boolean = true,
) {
    fun matches(state: RuleState): Boolean {
        if (!enabled) return false
        return conditions.all { condition ->
            when (condition) {
                RuleCondition.CHARGING -> state.charging
                RuleCondition.MEDIA_PLAYING -> state.mediaPlaying
                RuleCondition.DND -> state.dnd
                RuleCondition.SCREEN_ON -> state.screenOn
                RuleCondition.LOCKED -> state.locked
                RuleCondition.BATTERY_LOW -> state.batteryLow
            }
        }
    }
}

/** A snapshot of the phone state that rules are evaluated against. */
data class RuleState(
    val charging: Boolean = false,
    val mediaPlaying: Boolean = false,
    val dnd: Boolean = false,
    val screenOn: Boolean = true,
    val locked: Boolean = false,
    val batteryLow: Boolean = false,
)
