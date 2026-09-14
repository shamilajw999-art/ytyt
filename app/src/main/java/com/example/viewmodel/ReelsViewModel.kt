package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ThumbnailItem
import com.example.data.remote.VideoId
import com.example.data.remote.VideoSnippet
import com.example.data.remote.VideoThumbnails
import com.example.data.remote.YouTubeApi
import com.example.data.remote.YouTubeSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReelsViewModel : ViewModel() {
    private val api = YouTubeApi.create()

    private val defaultHistoricalVideos = listOf(
        YouTubeSearchResult(
            id = VideoId("aqz-KE-bpKQ"),
            snippet = VideoSnippet(
                title = "Sigiriya: The Ancient Palace in the Sky 🏰 🇱🇰",
                description = "King Kashyapa's 5th century palace atop the 200m granite Lion Rock fortress in Sri Lanka.",
                channelTitle = "Mythic Heritage",
                thumbnails = VideoThumbnails(
                    high = ThumbnailItem("https://img.youtube.com/vi/aqz-KE-bpKQ/hqdefault.jpg"),
                    maxres = ThumbnailItem("https://img.youtube.com/vi/aqz-KE-bpKQ/maxresdefault.jpg")
                )
            )
        ),
        YouTubeSearchResult(
            id = VideoId("aqz-KE-bpKQ"),
            snippet = VideoSnippet(
                title = "Polonnaruwa Vatadage & Gal Vihara Colossi 🗿",
                description = "Masterpiece stone architecture and sacred relic houses of medieval Sri Lanka.",
                channelTitle = "Ancient Wonders",
                thumbnails = VideoThumbnails(
                    high = ThumbnailItem("https://img.youtube.com/vi/aqz-KE-bpKQ/hqdefault.jpg")
                )
            )
        ),
        YouTubeSearchResult(
            id = VideoId("aqz-KE-bpKQ"),
            snippet = VideoSnippet(
                title = "Anuradhapura: 2,500 Years of Sacred History 🏛️",
                description = "The ancient capital that engineered giant stupas and hydraulic irrigation marvels.",
                channelTitle = "Chronicles of Lanka",
                thumbnails = VideoThumbnails(
                    high = ThumbnailItem("https://img.youtube.com/vi/aqz-KE-bpKQ/hqdefault.jpg")
                )
            )
        ),
        YouTubeSearchResult(
            id = VideoId("aqz-KE-bpKQ"),
            snippet = VideoSnippet(
                title = "Ravana's Cave & The Ramayana Legends ⚡",
                description = "Exploring the subterranean tunnels and cascading waterfalls of Ella, Sri Lanka.",
                channelTitle = "Mythic Explorers",
                thumbnails = VideoThumbnails(
                    high = ThumbnailItem("https://img.youtube.com/vi/aqz-KE-bpKQ/hqdefault.jpg")
                )
            )
        )
    )

    private val _videos = MutableStateFlow<List<YouTubeSearchResult>>(defaultHistoricalVideos)
    val videos: StateFlow<List<YouTubeSearchResult>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var nextPageToken: String? = null

    init {
        fetchVideos()
    }

    fun fetchVideos() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.searchShorts(pageToken = nextPageToken)
                nextPageToken = response.nextPageToken
                val validVideos = response.items?.filter { !it.id?.videoId.isNullOrBlank() } ?: emptyList()
                if (validVideos.isNotEmpty()) {
                    val currentIds = _videos.value.mapNotNull { it.id?.videoId }.toSet()
                    val newUnique = validVideos.filter { it.id?.videoId !in currentIds }
                    _videos.value = _videos.value + newUnique
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
