package com.piyush.island.ui.screenshot

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

/**
 * Asks the system to delete a screenshot.
 *
 * From Android 11 on, an app is not allowed to delete a picture that it did not create, so the
 * system shows its own confirmation sheet. This tiny activity exists only to start that sheet
 * and to close itself again.
 */
class DeleteRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }

        val raw = intent?.getStringExtra(EXTRA_URI)
        val uri = raw?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (uri == null) {
            finish()
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val target = ContentUris.withAppendedId(
                    MediaStore.Images.Media.getContentUri(MediaStore.getVolumeName(uri)),
                    ContentUris.parseId(uri),
                )
                val request = MediaStore.createDeleteRequest(contentResolver, listOf(target))
                startIntentSenderForResult(request.intentSender, REQUEST_DELETE, null, 0, 0, 0)
            } else {
                contentResolver.delete(uri, null, null)
            }
        }.onFailure {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    companion object {
        const val EXTRA_URI = "uri"
        private const val REQUEST_DELETE = 1
    }
}
