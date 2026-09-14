package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val nextPageToken: String?,
    val items: List<YouTubeSearchResult>?
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResult(
    val id: VideoId?,
    val snippet: VideoSnippet?
)

@JsonClass(generateAdapter = true)
data class VideoId(
    val videoId: String?
)

@JsonClass(generateAdapter = true)
data class VideoSnippet(
    val title: String?,
    val description: String?,
    val channelTitle: String?,
    val thumbnails: VideoThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class VideoThumbnails(
    val default: ThumbnailItem? = null,
    val medium: ThumbnailItem? = null,
    val high: ThumbnailItem? = null,
    val standard: ThumbnailItem? = null,
    val maxres: ThumbnailItem? = null
)

@JsonClass(generateAdapter = true)
data class ThumbnailItem(
    val url: String?
)
