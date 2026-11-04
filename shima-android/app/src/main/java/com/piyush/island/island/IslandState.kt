package com.piyush.island.island

import android.graphics.Bitmap
import com.piyush.island.model.AlertType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the island is showing right now. */
enum class IslandPhase { HIDDEN, COLLAPSED, EXPANDED }

/** A button the expanded card can offer. */
data class CardAction(
    val id: String,
    val labelRes: Int,
    val kind: Kind,
) {
    enum class Kind { PLAY_PAUSE, PREVIOUS, NEXT, SHARE, COPY, EDIT, DELETE, OPEN, MUTE, BATTERY_SAVER }
}

/**
 * The payload of the island. The renderer only needs these fields, so every kind of alert can
 * reuse the same drawing code.
 */
data class IslandCard(
    val type: AlertType,
    val title: String,
    val subtitle: String = "",
    val icon: String = "",
    val color: Int = 0xFFFFFFFF.toInt(),
    val accent: Int = 0xFF8AD5C6.toInt(),
    val progress: Float? = null,
    val secondaryProgress: Float? = null,
    val artwork: Bitmap? = null,
    val lyric: String? = null,
    val actions: List<CardAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val autoDismissAfterMs: Int = 6_000,
) {
    val hasActions: Boolean get() = actions.isNotEmpty()
}

/** The full state of the overlay. */
data class IslandUiState(
    val phase: IslandPhase = IslandPhase.HIDDEN,
    val card: IslandCard? = null,
    val companion: IslandCard? = null,
    val deck: List<IslandCard> = emptyList(),
) {
    val isVisible: Boolean get() = phase != IslandPhase.HIDDEN
}

/**
 * Holds what the island shows. The alert engine writes into it and the overlay view reads from
 * it, which keeps the two apart.
 */
class IslandStateHolder {

    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    fun show(card: IslandCard, deck: List<IslandCard> = emptyList()) {
        _state.value = _state.value.copy(phase = IslandPhase.COLLAPSED, card = card, deck = deck)
    }

    fun showCompanion(card: IslandCard?) {
        _state.value = _state.value.copy(companion = card)
    }

    fun expand() {
        if (_state.value.card == null) return
        _state.value = _state.value.copy(phase = IslandPhase.EXPANDED)
    }

    fun collapse() {
        if (_state.value.card == null) return
        _state.value = _state.value.copy(phase = IslandPhase.COLLAPSED)
    }

    fun dismiss() {
        _state.value = _state.value.copy(phase = IslandPhase.HIDDEN, card = null, deck = emptyList())
    }

    fun hide() {
        _state.value = _state.value.copy(phase = IslandPhase.HIDDEN)
    }

    fun updateCard(card: IslandCard) {
        _state.value = _state.value.copy(card = card)
    }
}
