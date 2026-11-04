package com.piyush.island

import android.app.Application
import com.piyush.island.core.AppGraph

/**
 * Application entry point. Owns the single [AppGraph] instance so that services, the
 * accessibility overlay and the settings UI all talk to the same store.
 */
class ShimaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.install(this)
    }
}
