package com.shemiji.emogibattery

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.shemiji.emogibattery.core.remoteconfig.RemoteConfig
import dagger.hilt.android.HiltAndroidApp
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShemijiBatteryApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        
        if (isMainProcess()) {
            applicationScope.launch {
                RemoteConfig.initialize()
                MobileAds.initialize(this@ShemijiBatteryApp)
            }
        }
    }

    private fun isMainProcess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName() == packageName
        } else {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = activityManager.runningAppProcesses
            processes?.any { it.pid == android.os.Process.myPid() && it.processName == packageName } == true
        }
    }
}
