package com.shemiji.emogibattery

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shemiji.emogibattery.core.ads.AdsManagerKit
import com.shemiji.emogibattery.ui.navigation.AppNavigation
import com.shemiji.emogibattery.ui.theme.ShemijiBatteryTheme
import com.shemiji.emogibattery.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color as AndroidColor

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        
        AdsManagerKit.loadHomeInterstitial(this)

        setContent {
            val appTheme by themeViewModel.theme.collectAsState()

            ShemijiBatteryTheme(appTheme = appTheme) {
                AppNavigation(themeViewModel = themeViewModel)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        AdsManagerKit.loadHomeInterstitial(this)
    }
}
