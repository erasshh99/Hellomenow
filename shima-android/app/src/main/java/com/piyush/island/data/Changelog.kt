package com.piyush.island.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The "what's new" list. It is bundled with the app, so it is always available and never needs
 * a network call.
 */
object Changelog {

    data class Entry(
        val version: String,
        val date: String,
        val channel: String,
        val headline: String,
        val summary: String,
        val changes: List<Change>,
    )

    data class Change(val kind: String, val text: String)

    fun load(context: Context): List<Entry> = try {
        val raw = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
        parse(raw)
    } catch (_: Exception) {
        emptyList()
    }

    fun parse(raw: String): List<Entry> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val changesJson = json.optJSONArray("changes")
            val changes = (0 until (changesJson?.length() ?: 0)).mapNotNull { changeIndex ->
                val change = changesJson?.optJSONObject(changeIndex) ?: return@mapNotNull null
                Change(change.optString("kind"), change.optString("text"))
            }
            Entry(
                version = json.optString("version"),
                date = json.optString("date"),
                channel = json.optString("channel"),
                headline = json.optString("headline"),
                summary = json.optString("summary"),
                changes = changes,
            )
        }
    }

    /** The entry that matches the running version, if there is one. */
    fun forVersion(entries: List<Entry>, version: String): Entry? =
        entries.firstOrNull { it.version == version }
}
