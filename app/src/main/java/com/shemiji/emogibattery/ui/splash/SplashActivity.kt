package com.shemiji.emogibattery.ui.splash

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.shemiji.emogibattery.MainActivity
import com.shemiji.emogibattery.core.remoteconfig.RemoteConfig
import com.shemiji.emogibattery.databinding.ActivitySplashBinding
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

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

        setupGradientText()
        startLoadingAnimation()

        lifecycleScope.launch {
            RemoteConfig.isReady.observe(this@SplashActivity) { ready ->
                if (ready) {
                    // Logic for ads could go here
                }
            }
        }
    }

    private fun setupGradientText() {
        binding.tvShimeji.post {
            val paint = binding.tvShimeji.paint
            val width = paint.measureText(binding.tvShimeji.text.toString())
            val textShader: Shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    "#632EFA".toColorInt(),
                    "#E334EC".toColorInt()
                ),
                null,
                Shader.TileMode.CLAMP
            )
            binding.tvShimeji.paint.shader = textShader
            binding.tvShimeji.invalidate()
        }
    }

    private fun startLoadingAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Update Progress Fill Width
                val fillParams = binding.vProgressFill.layoutParams as ConstraintLayout.LayoutParams
                fillParams.matchConstraintPercentWidth = progress
                binding.vProgressFill.layoutParams = fillParams
                
                // Update Star Position (Bias)
                val starParams = binding.ivProgressStar.layoutParams as ConstraintLayout.LayoutParams
                starParams.horizontalBias = progress
                binding.ivProgressStar.layoutParams = starParams
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    lifecycleScope.launch {
                        delay(400)
                        navigateToNext()
                    }
                }
            })
            start()
        }
    }

    private fun navigateToNext() {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
