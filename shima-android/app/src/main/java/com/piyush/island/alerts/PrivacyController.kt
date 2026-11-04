package com.piyush.island.alerts

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Watches for the microphone and the camera being used by another app.
 */
class PrivacyController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraOpen = false
    private var micActive = false

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            cameraOpen = false
            publish()
        }

        override fun onCameraUnavailable(cameraId: String) {
            cameraOpen = true
            publish()
        }
    }

    fun start() {
        runCatching {
            cameraManager?.registerAvailabilityCallback(availabilityCallback, null)
        }
        publish()
    }

    fun stop() {
        runCatching { cameraManager?.unregisterAvailabilityCallback(availabilityCallback) }
        onCard(null)
    }

    fun refresh() = publish()

    /**
     * Whether another app is holding the microphone.
     *
     * The platform does not expose this in the public SDK, so the service is called through
     * reflection and the answer is simply "no" when the system does not allow the call.
     */
    private fun microphoneActive(): Boolean {
        val audio = audioManager ?: return false
        return runCatching {
            val method = AudioManager::class.java.getMethod("isMicrophoneActive")
            method.invoke(audio) as? Boolean ?: false
        }.getOrDefault(false)
    }

    private fun publish() {
        val mic = microphoneActive()
        if (mic != micActive) {
            micActive = mic
            if (mic) {
                onCard(
                    IslandCard(
                        type = AlertType.PRIVACY,
                        title = "Microphone",
                        subtitle = "An app is using it",
                        icon = "mic",
                        color = 0xFFD1495B.toInt(),
                        accent = 0xFFFF9A8B.toInt(),
                        autoDismissAfterMs = 4_000,
                    )
                )
                return
            }
        }
        if (cameraOpen) {
            onCard(
                IslandCard(
                    type = AlertType.PRIVACY,
                    title = "Camera",
                    subtitle = "An app is using it",
                    icon = "camera",
                    color = 0xFFD1495B.toInt(),
                    accent = 0xFFFF9A8B.toInt(),
                    autoDismissAfterMs = 4_000,
                )
            )
        } else {
            onCard(null)
        }
    }
}
