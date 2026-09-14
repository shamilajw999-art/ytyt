package com.example.data.remote

import android.content.Context
import com.example.R

data class HeritageArticle(
    val id: String,
    val siteName: String,
    val province: String,
    val description: String,
    val imageUrl: String, // Can be a local drawable name (e.g. "img_sigiriya") or a web URL
    val category: String,
    val unescoStatus: String = "UNESCO World Heritage Site",
    val era: String = "Ancient",
    val facts: String = "Historical facts",
    val initialLikes: Int
) {
    fun getImageModel(context: Context): Any {
        val trimmed = imageUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        if (trimmed.isNotBlank()) {
            val cleanName = trimmed.substringBeforeLast(".")
            val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
            if (resId != 0) {
                return resId
            }
        }
        val nameLower = (siteName + " " + category + " " + province + " " + description).lowercase()
        return when {
            nameLower.contains("sigiriya") || nameLower.contains("kashyapa") -> R.drawable.img_sigiriya
            nameLower.contains("kandy") || nameLower.contains("tooth") || nameLower.contains("dalada") -> R.drawable.img_kandy
            nameLower.contains("galle") || nameLower.contains("fort") || nameLower.contains("dutch") -> R.drawable.img_galle
            nameLower.contains("anuradhapura") || nameLower.contains("ruwanweli") || nameLower.contains("bodhi") -> R.drawable.img_anuradhapura
            else -> R.drawable.app_logo_custom
        }
    }
}
