package com.piyush.island.gestures

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import com.piyush.island.core.AppGraph
import com.piyush.island.model.GestureAction
import com.piyush.island.model.SystemActionKind
import java.io.File

/**
 * Runs the action a gesture was bound to.
 *
 * Everything happens through public APIs: the accessibility service for system gestures and
 * global actions, the package manager for apps, and the media session for transport controls.
 */
class ActionExecutor(private val service: AccessibilityService) {

    private val context: Context = service.applicationContext

    fun execute(action: GestureAction): Boolean = when (action) {
        is GestureAction.None -> false
        is GestureAction.LaunchApp -> launchApp(action.packageName)
        is GestureAction.OpenWidget -> openWidget(action.appWidgetId)
        is GestureAction.SystemAction -> systemAction(action.action)
        is GestureAction.TaskerTask -> runTaskerTask(action.packageName, action.taskName)
    }

    fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openWidget(widgetProviderId: Int): Boolean {
        val bridge = AppWidgetHostBridge.get(context) ?: return false
        return bridge.launch(widgetProviderId)
    }

    fun runTaskerTask(packageName: String, taskName: String): Boolean {
        val intent = Intent("net.dinglisch.android.tasker.TASKER_TASK").apply {
            setPackage(packageName)
            putExtra("task_name", taskName)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun systemAction(kind: SystemActionKind): Boolean = when (kind) {
        SystemActionKind.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        SystemActionKind.HOME -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        SystemActionKind.RECENTS -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        SystemActionKind.NOTIFICATION_SHADE -> {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }

        SystemActionKind.QUICK_SETTINGS -> {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        }

        SystemActionKind.LOCK_SCREEN -> {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) || lockScreen()
        }

        SystemActionKind.SCREENSHOT -> takeScreenshot()
        SystemActionKind.TORCH -> {
            AppGraph.alertEngine.flashlight.toggle()
            true
        }

        SystemActionKind.ROTATION_LOCK -> toggleRotationLock()
        SystemActionKind.DND -> toggleDnd()
        SystemActionKind.MEDIA_PREVIOUS -> {
            AppGraph.alertEngine.media.skipToPrevious()
            true
        }

        SystemActionKind.MEDIA_NEXT -> {
            AppGraph.alertEngine.media.skipToNext()
            true
        }

        SystemActionKind.MEDIA_PLAY_PAUSE -> {
            AppGraph.alertEngine.media.togglePlayPause()
            true
        }
    }

    /**
     * Locking the screen with the global action needs the service to be allowed to lock it, which
     * not every device grants. The hidden API is used as a second chance.
     */
    private fun lockScreen(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return runCatching {
            val method = PowerManager::class.java.getMethod("goToSleep", Boolean::class.javaPrimitiveType)
            method.invoke(power, true)
            true
        }.getOrDefault(false)
    }

    /** The system takes the screenshot itself, which keeps the app out of the picture taking. */
    private fun takeScreenshot(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    private fun toggleRotationLock(): Boolean = runCatching {
        val resolver = context.contentResolver
        val current = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, if (current == 1) 0 else 1)
        true
    }.getOrDefault(false)

    private fun toggleDnd(): Boolean = runCatching {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return@runCatching false
        val filter = manager.currentInterruptionFilter
        manager.setInterruptionFilter(
            if (filter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
        )
        true
    }.getOrDefault(false)

    companion object {
        fun fileProviderUri(context: Context, file: File): Uri? = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }
}

/**
 * Lets the user pick a home screen widget and open the app that provides it. Shima does not
 * embed other apps' widgets, it opens the widget's own app, which keeps the permissions small.
 */
object AppWidgetHostBridge {

    private const val HOST_ID = 0x5348494D // "SHIM"

    private var host: android.appwidget.AppWidgetHost? = null

    class WidgetHost(private val context: Context) {

        /** Opens the app that provides the widget with the given provider id. */
        fun launch(widgetProviderId: Int): Boolean {
            val manager = AppWidgetManager.getInstance(context) ?: return false
            val provider = manager.getInstalledProviders()
                .firstOrNull { providerId(it) == widgetProviderId } ?: return false
            val intent = context.packageManager.getLaunchIntentForPackage(provider.provider.packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(intent) }.isSuccess
        }

        /** Asks the system to let the user pin a widget, used by the widget picker. */
        fun requestPin(provider: AppWidgetProviderInfo): Boolean {
            val manager = AppWidgetManager.getInstance(context) ?: return false
            return runCatching {
                manager.requestPinAppWidget(provider.provider, null, null)
            }.getOrDefault(false)
        }
    }

    fun get(context: Context): WidgetHost? {
        val existing = host
        if (existing != null) return WidgetHost(context)
        return runCatching {
            val created = android.appwidget.AppWidgetHost(context.applicationContext, HOST_ID)
            created.startListening()
            host = created
            WidgetHost(context.applicationContext)
        }.getOrNull()
    }

    data class WidgetEntry(val id: Int, val label: String, val packageName: String)

    /**
     * A stable number for a widget provider. The platform does not give a public id to a
     * provider, so the name of the provider is used to make one that stays the same.
     */
    fun providerId(provider: AppWidgetProviderInfo): Int = provider.provider.flattenToString().hashCode()

    /** All widgets the user can pick from, one entry per provider that is installed. */
    fun widgets(context: Context): List<WidgetEntry> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val result = mutableListOf<WidgetEntry>()
        manager.getInstalledProviders().forEach { provider ->
            val label = provider.loadLabel(context.packageManager).toString()
            result.add(WidgetEntry(providerId(provider), label, provider.provider.packageName))
        }
        return result.distinctBy { it.id }
    }
}
