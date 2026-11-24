package com.example.scrollstop

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scrollstop.ui.FocusGuardScreen
import com.example.scrollstop.ui.theme.FocusGuardTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getInstance(newBase).appLanguage
        super.attachBaseContext(newBase.wrapWithAppLocale(lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsManager.getInstance(applicationContext)
        setContent {
            FocusGuardTheme {
                FocusGuardScreen(settings = settings)
            }
        }
    }
}
