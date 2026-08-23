package com.shemiji.emogibattery.core.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.*
import com.shemiji.emogibattery.R

@SuppressLint("StaticFieldLeak")
object AdsManagerKit {
    var isProVersion = MutableLiveData(false)

    var homeInterstitial: InterstitialAd? = null
    private var isHomeInterLoading = false
    private var loadingDialog: Dialog? = null

    // Config (can be updated via RemoteConfig)
    var splashInterstitialEnabled = true
    var splashInterstitialHighEnabled = true
    var homeInterstatialEnabled = true
    var interstitialAdCounter = 2
    var clickCount = 0

    var langNative1Enabled = true
    var preLoadedNative = true
    var langNativeAd1: NativeAd? = null

    fun loadInterstitialAd(
        activity: Activity,
        adUnitId: String,
        isEnable: Boolean = true,
        onAdLoaded: (interstitialAd: InterstitialAd) -> Unit,
        onAdFailed: ((error: LoadAdError) -> Unit)? = null
    ) {
        if (isProVersion.value == true || !isEnable) return

        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdFailed?.invoke(loadAdError)
                }
            })
    }

    fun loadHomeInterstitial(activity: Activity) {
        if (isProVersion.value == true || !homeInterstatialEnabled || homeInterstitial != null || isHomeInterLoading) return
        isHomeInterLoading = true
        loadInterstitialAd(
            activity = activity,
            adUnitId = activity.getString(R.string.inter_home),
            isEnable = homeInterstatialEnabled,
            onAdLoaded = { ad ->
                homeInterstitial = ad
                isHomeInterLoading = false
            },
            onAdFailed = {
                isHomeInterLoading = false
            }
        )
    }

    fun showInterstitialHome(
        activity: Activity,
        eventName: String = "",
        onDismissed: (() -> Unit)? = null
    ) {
        if (isProVersion.value == true) {
            onDismissed?.invoke()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val adToShow = homeInterstitial
                if (adToShow == null) {
                    onDismissed?.invoke()
                    loadHomeInterstitial(activity)
                    return@launch
                }
                clickCount++
                val shouldShowAd = (clickCount == 1 || (clickCount - 1) % interstitialAdCounter == 0)
                if (shouldShowAd) {
                    showLoading(activity)
                    delay(1500)
                    adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            homeInterstitial = null
                        }
                        override fun onAdDismissedFullScreenContent() {
                            hideLoading()
                            loadHomeInterstitial(activity)
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            hideLoading()
                            loadHomeInterstitial(activity)
                        }
                    }
                    onDismissed?.invoke()
                    adToShow.show(activity)
                } else {
                    onDismissed?.invoke()
                }
            } catch (e: Exception) {
                onDismissed?.invoke()
            }
        }
    }

    fun showLoading(context: Context) {
        if (loadingDialog?.isShowing == true) return
        loadingDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            // Use a simple progress layout or create one if needed
            setContentView(R.layout.ad_dialog_new) 
            window?.apply {
                setBackgroundDrawable(Color.WHITE.toDrawable())
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
        try {
            loadingDialog?.show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
