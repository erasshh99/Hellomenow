package com.piyush.island.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.piyush.island.core.AppGraph

/**
 * The Quick Settings tile switches the island on and off.
 */
@RequiresApi(Build.VERSION_CODES.N)
class IslandTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        AppGraph.install(applicationContext)
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        AppGraph.install(applicationContext)
        val enabled = !AppGraph.settings.current.enabled
        AppGraph.settings.setEnabled(enabled)
        if (enabled) {
            KeepAliveService.start(applicationContext)
        } else {
            AppGraph.alertEngine.clear()
            KeepAliveService.stop(applicationContext)
        }
        syncTile()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        tile.state = if (AppGraph.settings.current.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        runCatching { tile.updateTile() }
    }
}
