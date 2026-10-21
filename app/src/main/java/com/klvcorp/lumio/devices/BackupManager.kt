package com.klvcorp.lumio.devices

import android.content.Context
import org.json.JSONObject

/** Local settings export. Includes device MAC addresses; share backup files deliberately. */
class BackupManager(private val context: Context) {
    private val sections = listOf(
        "lumio_preferences", "lumio_devices", "lumio_scenes", "lumio_favorite_colors", "lumio_light_per_device"
    )

    fun exportJson(): String {
        val root = JSONObject().put("format", "lumio-backup").put("version", 1)
        val data = JSONObject()
        for (name in sections) {
            val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val values = JSONObject()
            stored.all.forEach { (key, value) ->
                if (value is Int || value is Long || value is Float || value is Boolean || value is String) {
                    values.put(key, value)
                }
            }
            data.put(name, values)
        }
        return root.put("data", data).toString(2)
    }

    /** Import only whitelisted preference sections and primitive values. */
    fun importJson(text: String) {
        require(text.length <= 512_000) { "File is too large" }
        val root = JSONObject(text)
        require(root.optString("format") == "lumio-backup" && root.optInt("version") == 1) { "Invalid Lumio backup" }
        val data = root.getJSONObject("data")
        val parsed = sections.associateWith { name ->
            val obj = data.optJSONObject(name) ?: JSONObject()
            val values = mutableMapOf<String, Any>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.length !in 1..100) continue
                val value = obj.get(key)
                if (value is Int || value is Long || value is Double || value is Boolean || value is String) {
                    values[key] = value
                }
            }
            values
        }
        // All validation occurs before changing any preferences.
        for ((name, values) in parsed) {
            val edit = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            for ((key, value) in values) when (value) {
                is Int -> edit.putInt(key, value)
                is Long -> edit.putLong(key, value)
                is Double -> edit.putFloat(key, value.toFloat())
                is Boolean -> edit.putBoolean(key, value)
                is String -> edit.putString(key, value)
            }
            edit.commit()
        }
    }
}
