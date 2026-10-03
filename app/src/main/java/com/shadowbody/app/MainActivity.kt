package com.shadowbody.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shadowbody.app.navigation.ShadowBodyNavHost
import com.shadowbody.app.ui.theme.ShadowBodyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        (application as ShadowBodyApp).scheduleWakeUp()
        setContent {
            ShadowBodyTheme {
                ShadowBodyNavHost()
            }
        }
    }
}
