package com.example.data.remote

import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApi {
    @GET("search")
    suspend fun searchShorts(
        @Query("part") part: String = "snippet",
        @Query("q") query: String = "Sri Lanka ancient history Sigiriya mythology shorts",
        @Query("type") type: String = "video",
        @Query("videoDuration") videoDuration: String = "short",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("videoSyndicated") videoSyndicated: String = "true",
        @Query("safeSearch") safeSearch: String = "moderate",
        @Query("maxResults") maxResults: Int = 15,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String = BuildConfig.YOUTUBE_API_KEY
    ): YouTubeSearchResponse

    companion object {
        fun create(): YouTubeApi {
            return Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(YouTubeApi::class.java)
        }
    }
}
