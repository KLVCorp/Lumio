package com.klvcorp.lumio.devices

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class LightScene(val title: String, val color: Int, val brightness: Int)

class SceneRepository(context: Context) {
    private val prefs = context.getSharedPreferences("lumio_scenes", Context.MODE_PRIVATE)
    val scenes = mutableStateListOf<LightScene>()
    init {
        runCatching {
            val arr = JSONArray(prefs.getString("scenes", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                scenes.add(LightScene(o.getString("title"), o.getInt("color"), o.getInt("brightness")))
            }
        }
    }
    fun add(title: String, color: Int, brightness: Int) {
        val cleaned = title.trim().take(32)
        if (cleaned.isBlank()) return
        scenes.removeAll { it.title.equals(cleaned, true) }
        scenes.add(LightScene(cleaned, color, brightness)); persist()
    }
    fun remove(title: String) { scenes.removeAll { it.title == title }; persist() }
    private fun persist() {
        val arr = JSONArray()
        scenes.forEach { arr.put(JSONObject().put("title", it.title).put("color", it.color).put("brightness", it.brightness)) }
        prefs.edit().putString("scenes", arr.toString()).apply()
    }
}
