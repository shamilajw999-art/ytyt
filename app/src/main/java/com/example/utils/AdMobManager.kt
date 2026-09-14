package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob Configuration Constants and IDs
 */
object AdMobConfig {
    const val APP_ID = "ca-app-pub-4256981176253022~6888108404"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-4256981176253022/5780884175"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-4256981176253022/4276230818"

    // Google Test IDs as fallback
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d("AdMobManager", "AdMob MobileAds initialized: $status")
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e("AdMobManager", "Failed to init MobileAds", e)
            }
        }
    }
}

/**
 * Helper class to preload and show Rewarded Video Ads
 */
class AdMobRewardedManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    var isLoading by mutableStateOf(false)
        private set
    var isLoaded by mutableStateOf(false)
        private set

    init {
        loadAd()
    }

    fun loadAd() {
        if (isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            AdMobConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("AdMobRewarded", "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isLoading = false
                    isLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("AdMobRewarded", "Failed to load AdMob rewarded ad: ${error.message} (code ${error.code})")
                    // Fallback to sample test ad unit if live unit is still propagating
                    RewardedAd.load(
                        context,
                        AdMobConfig.TEST_REWARDED_AD_UNIT_ID,
                        adRequest,
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                rewardedAd = ad
                                isLoading = false
                                isLoaded = true
                            }
                            override fun onAdFailedToLoad(err: LoadAdError) {
                                isLoading = false
                                isLoaded = false
                            }
                        }
                    )
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (Int, String) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    isLoaded = false
                    loadAd()
                    onUserEarnedReward(50, "fallback")
                    android.widget.Toast.makeText(activity, "Ad failed to load. +50 XP granted anyway!", android.widget.Toast.LENGTH_SHORT).show()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("AdMobRewarded", "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                    isLoaded = false
                    onAdDismissed()
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.d("AdMobRewarded", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
        } else {
            loadAd()
            onUserEarnedReward(50, "fallback")
            android.widget.Toast.makeText(activity, "Ad failed to load. +50 XP granted anyway!", android.widget.Toast.LENGTH_SHORT).show()
            onAdDismissed()
        }
    }
}

/**
 * Composable AdMob Banner Ad view
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID,
    adSize: AdSize = AdSize.BANNER
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AdMobConfig.initialize(context)
    }

    var adFailedToLoad by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            adFailedToLoad = false
                            Log.d("AdMobBanner", "AdMob banner loaded successfully ($adUnitId)")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("AdMobBanner", "Live banner failed: ${error.message}. Trying test banner.")
                            adFailedToLoad = true
                            // Fallback to sample test unit while new ad unit is propagating
                            this@apply.adUnitId = AdMobConfig.TEST_BANNER_AD_UNIT_ID
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
