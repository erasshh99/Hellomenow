package com.piyush.island.alerts

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.piyush.island.island.CardAction
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Controls the torch and shows its state on the island. The phone needs a camera with a flash;
 * devices without one simply never report a card.
 */
class FlashlightController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var torchOn = false

    fun start() {
        publish()
    }

    fun stop() {
        torchOn = false
        onCard(null)
    }

    /** Flips the torch and returns the new state. */
    fun toggle(): Boolean {
        val cameraId = cameraWithFlash() ?: return torchOn
        return try {
            torchOn = !torchOn
            cameraManager?.setTorchMode(cameraId, torchOn)
            publish()
            torchOn
        } catch (_: CameraAccessException) {
            torchOn
        } catch (_: SecurityException) {
            torchOn
        }
    }

    fun isOn(): Boolean = torchOn

    private fun cameraWithFlash(): String? = try {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (_: Exception) {
        null
    }

    private fun publish() {
        if (!torchOn) {
            onCard(null)
            return
        }
        onCard(
            IslandCard(
                type = AlertType.FLASHLIGHT,
                title = "Flashlight",
                subtitle = "On",
                icon = "torch",
                color = 0xFFF2A33C.toInt(),
                accent = 0xFFFFD08A.toInt(),
                actions = listOf(
                    CardAction("torch", com.piyush.island.R.string.card_torch_off, CardAction.Kind.MUTE),
                ),
                autoDismissAfterMs = 4_000,
            )
        )
    }
}
