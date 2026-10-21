package com.klvcorp.lumio.devices

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray

/** User saved RGB colors, stored locally. Independent of scenes. */
class FavoriteColorRepository(context: Context) {
    private val prefs = context.getSharedPreferences("lumio_favorite_colors", Context.MODE_PRIVATE)
    val colors = mutableStateListOf<Int>()

    init {
        runCatching {
            val data = JSONArray(prefs.getString("colors", "[]"))
            for (i in 0 until data.length()) {
                val value = data.getInt(i) or 0xFF000000.toInt()
                if (!colors.contains(value) && colors.size < 12) colors.add(value)
            }
        }
    }

    fun add(argb: Int) {
        val color = argb or 0xFF000000.toInt()
        if (colors.contains(color) || colors.size >= 12) return
        colors.add(color)
        save()
    }

    fun remove(argb: Int) {
        if (colors.remove(argb)) save()
    }

    private fun save() {
        val json = JSONArray()
        colors.forEach { json.put(it) }
        prefs.edit().putString("colors", json.toString()).apply()
    }
}
