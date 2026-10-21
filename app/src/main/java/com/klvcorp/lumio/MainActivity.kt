package com.klvcorp.lumio

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.klvcorp.lumio.ui.screens.LumioApp
import com.klvcorp.lumio.ui.theme.LumioTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("lumio_preferences", Context.MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        if (language == "system") { super.attachBaseContext(newBase); return }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = LumioRuntime.get(applicationContext)
        if (controller.backgroundEnabled && controller.ble.hasPermissions()) controller.setBackgroundEnabled(true)
        setContent {
            val dark = when (controller.theme) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            androidx.compose.runtime.SideEffect {
                window.statusBarColor = if (dark) android.graphics.Color.rgb(17,16,25) else android.graphics.Color.WHITE
                window.navigationBarColor = if (dark) android.graphics.Color.rgb(17,16,25) else android.graphics.Color.WHITE
                window.decorView.systemUiVisibility = if (dark) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            LumioTheme(darkTheme = dark) { LumioApp(controller) }
        }
    }
}
