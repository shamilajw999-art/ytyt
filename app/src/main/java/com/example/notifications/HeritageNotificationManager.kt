package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

data class HeritageSiteLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val prompt: String
)

object HeritageNotificationManager {
    const val CHANNEL_ID = "mythic_heritage_channel"
    private const val CHANNEL_NAME = "Mythic Heritage & Streaks"

    val HERITAGE_SITES = listOf(
        HeritageSiteLocation("Sigiriya Lion Rock", 7.9570, 80.7603, "Tell me the ancient story of Sigiriya Lion Rock Fortress"),
        HeritageSiteLocation("Temple of the Sacred Tooth Relic", 7.2936, 80.6413, "Tell me the story of the Sacred Tooth Relic in Kandy"),
        HeritageSiteLocation("Galle Dutch Fort", 6.0267, 80.2170, "Tell me the history of Galle Dutch Fort"),
        HeritageSiteLocation("Nine Arch Bridge, Ella", 6.8768, 81.0608, "Tell me how the Nine Arch Bridge was constructed in Ella"),
        HeritageSiteLocation("Ruwanwelisaya Stupa", 8.3500, 80.3961, "Tell me the story of Ruwanwelisaya Stupa in Anuradhapura"),
        HeritageSiteLocation("Polonnaruwa Vatadage", 7.9482, 81.0003, "Tell me about the ancient kingdom of Polonnaruwa and Vatadage"),
        HeritageSiteLocation("Dambulla Cave Temple", 7.8567, 80.6483, "Tell me the story of Dambulla Royal Cave Temple"),
        HeritageSiteLocation("Mihintale Sacred Peak", 8.3508, 80.5036, "Tell me the story of Mihintale and the arrival of Buddhism in Sri Lanka")
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for near heritage site stories, login streaks, and usage cap updates"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showStreakNotification(context: Context, streakDays: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔥 $streakDays-Day Login Streak Maintained!")
            .setContentText("You are on a $streakDays-day streak! Open Mythic Ceylon daily to collect bonus XP and unlock rare heritage badges.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
    }

    fun checkLocationAndNotifyNearSite(context: Context, userLat: Double, userLng: Double) {
        createNotificationChannel(context)

        for ((index, site) in HERITAGE_SITES.withIndex()) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, site.lat, site.lng, results)
            val distanceInMeters = results[0]

            // Trigger notification if within 15 km (15,000 meters)
            if (distanceInMeters <= 15000f) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "lumo_story")
                    putExtra("site_name", site.name)
                    putExtra("prompt", site.prompt)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    2000 + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle("🏛️ You are near ${site.name}!")
                    .setContentText("Do you want Lumo AI to tell you the ancient story of ${site.name}?")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("You are near ${site.name}! Tap here to open Lumo AI and listen to the ancient legend and heritage story of this site.")
                    )
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(2000 + index, builder.build())
                break // Notify for closest site found
            }
        }
    }

    fun showDailyLimitCapNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "subscription")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚡ Free Daily Usage Cap Reached (5/5)")
            .setContentText("You hit your free daily AI story limit. Tap to upgrade to Mythic Pro/Max for unlimited Lumo AI and zero ads!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3001, builder.build())
    }

    fun showSubscriptionApprovedNotification(context: Context, plan: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "profile")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            4001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMax = plan.equals("max", ignoreCase = true)
        val emblemTitle = if (isMax) "👑 Mythic MAX Activated!" else "⭐ Mythic PRO Activated!"
        val perkSummary = if (isMax) {
            "Your payment was approved! You unlocked the Gold Max Emblem, Zero Ads, Unlimited AI, 3D AR Scans, and all exclusive perks."
        } else {
            "Your payment was approved! You unlocked the Silver Pro Emblem, Zero Ads, and Unlimited AI Chats."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(emblemTitle)
            .setContentText(perkSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(perkSummary))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(4001, builder.build())
    }

    fun showSubscriptionExpiringSoonNotification(context: Context, plan: String, daysLeft: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "subscription")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            4002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val planName = plan.replaceFirstChar { it.uppercase() }
        val title = "⚠️ Mythic $planName Subscription Expiring in $daysLeft Days"
        val message = "Your Mythic $planName membership will end soon. Renew your subscription now to avoid losing your subscriber emblem, ad-free access, and exclusive perks."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(4002, builder.build())
    }

    fun showSubscriptionExpiredNotification(context: Context, plan: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "subscription")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            4003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val planName = plan.replaceFirstChar { it.uppercase() }
        val title = "⚠️ Mythic $planName Subscription Ended"
        val message = "Your subscription has expired. Tap to renew and restore your subscriber emblem, zero ads, and AI features."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(4003, builder.build())
    }
}
