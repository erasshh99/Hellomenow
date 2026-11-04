package com.piyush.island.core

import android.content.Context
import com.piyush.island.alerts.AlertEngine
import com.piyush.island.data.SettingsStore
import com.piyush.island.island.IslandStateHolder

/**
 * Manual dependency graph. The app is small enough that a service locator is clearer than a
 * full DI framework, and it keeps the services (accessibility, tile, notification listener)
 * talking to exactly the same state.
 */
object AppGraph {

    @Volatile
    private var installed = false

    lateinit var settings: SettingsStore
        private set

    lateinit var islandState: IslandStateHolder
        private set

    lateinit var alertEngine: AlertEngine
        private set

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        val app = context.applicationContext
        settings = SettingsStore(app)
        islandState = IslandStateHolder()
        alertEngine = AlertEngine(app, settings, islandState)
        installed = true
    }
}
