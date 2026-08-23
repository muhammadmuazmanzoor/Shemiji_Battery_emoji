package com.shemiji.emogibattery.core.remoteconfig

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.shemiji.emogibattery.core.ads.AdsManagerKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object RemoteConfig {
    private const val TAG = "RemoteConfig"

    private const val KEY_HOME_INTER = "home_inter"
    private const val KEY_INTERSTITIAL_AD_COUNTER = "interstitial_ad_counter"

    private val defaults: Map<String, Any> = mapOf(
        KEY_HOME_INTER to true,
        KEY_INTERSTITIAL_AD_COUNTER to 2L
    )

    val isReady = MutableLiveData(false)

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val remoteConfig = Firebase.remoteConfig
            try {
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 3600L
                }
                remoteConfig.setConfigSettingsAsync(configSettings).await()
                remoteConfig.setDefaultsAsync(defaults).await()

                remoteConfig.fetchAndActivate().await()
                apply(remoteConfig)
                isReady.postValue(true)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization error", e)
                apply(remoteConfig)
                isReady.postValue(true)
            }
        }
    }

    private fun apply(remoteConfig: FirebaseRemoteConfig) {
        AdsManagerKit.homeInterstatialEnabled = remoteConfig.getBoolean(KEY_HOME_INTER)
        AdsManagerKit.interstitialAdCounter = remoteConfig.getLong(KEY_INTERSTITIAL_AD_COUNTER).toInt()
    }
}
