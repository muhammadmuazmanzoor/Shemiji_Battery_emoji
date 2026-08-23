package com.shemiji.emogibattery.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.shemiji.emogibattery.MainActivity
import com.shemiji.emogibattery.R
import com.shemiji.emogibattery.core.ads.AdsManagerKit
import com.shemiji.emogibattery.core.remoteconfig.RemoteConfig
import com.shemiji.emogibattery.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var interstitialSplash: InterstitialAd? = null

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
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            // Wait for RemoteConfig to be ready
            RemoteConfig.isReady.observe(this@SplashActivity) { ready ->
                if (ready) {
//loadAndShowAds()
                }
            }
        }
        lifecycleScope.launch {
            delay(2000)
            navigateToNext()
        }
    }

/*    private fun loadAndShowAds() {
        if (AdsManagerKit.isProVersion.value == true || !AdsManagerKit.splashInterstitialEnabled) {
            lifecycleScope.launch {
                delay(2000)
                navigateToNext()
            }
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            getString(R.string.inter_splash),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialSplash = ad
                    showInterstitial()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    navigateToNext()
                }
            })
    }

    private fun showInterstitial() {
        interstitialSplash?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                navigateToNext()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                navigateToNext()
            }
        }
        interstitialSplash?.show(this)
    }*/

    private fun navigateToNext() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
