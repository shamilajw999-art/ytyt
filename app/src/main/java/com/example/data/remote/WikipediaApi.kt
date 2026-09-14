package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// The structured data from Wikipedia REST API
@JsonClass(generateAdapter = true)
data class WikiSummary(
    val title: String,
    val extract: String? = null,
    val description: String? = null,
    val thumbnail: Thumbnail? = null,
    val originalimage: Thumbnail? = null,
    @Json(name = "content_urls") val contentUrls: WikiContentUrls? = null
)

@JsonClass(generateAdapter = true)
data class WikiContentUrls(
    val desktop: WikiPageUrl? = null,
    val mobile: WikiPageUrl? = null
)

@JsonClass(generateAdapter = true)
data class WikiPageUrl(
    val page: String? = null
)

@JsonClass(generateAdapter = true)
data class Thumbnail(
    val source: String,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class WikiSearchResponse(
    val pages: List<WikiSearchPage>? = null
)

@JsonClass(generateAdapter = true)
data class WikiSearchPage(
    val id: Long? = null,
    val key: String? = null,
    val title: String? = null,
    val excerpt: String? = null,
    val description: String? = null,
    val thumbnail: WikiSearchThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class WikiSearchThumbnail(
    val url: String? = null
)

interface WikipediaService {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getSummary(
        @Path("title") title: String,
        @Header("User-Agent") userAgent: String = "MythicExplorer/1.0 (contact@mythicexplorer.com)"
    ): WikiSummary

    @GET("w/rest.php/v1/search/page")
    suspend fun searchPage(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Header("User-Agent") userAgent: String = "MythicExplorer/1.0 (contact@mythicexplorer.com)"
    ): WikiSearchResponse
}

object WikipediaClient {
    private const val BASE_URL = "https://en.wikipedia.org/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: WikipediaService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaService::class.java)
    }
}
