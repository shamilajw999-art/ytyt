package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.remote.YouTubeSearchResult
import com.example.viewmodel.MythicViewModel
import com.example.viewmodel.ReelsViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun ReelsScreen(
    viewModel: MythicViewModel,
    reelsViewModel: ReelsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToChat: () -> Unit = {}
) {
    val accentColor = Color(0xFF86FC5C)
    val videos by reelsViewModel.videos.collectAsStateWithLifecycle()
    val isLoading by reelsViewModel.isLoading.collectAsStateWithLifecycle()

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = accentColor)
                Text("Discovering Mythic History Reels...", color = Color.White, fontSize = 14.sp)
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { videos.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = videos[page]
            YouTubeShortPlayer(
                video = video,
                isCurrentPage = pagerState.currentPage == page,
                accentColor = accentColor,
                onAskMythic = onNavigateToChat
            )
        }

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Mythic Reels",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "LIVE",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = accentColor,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= videos.size - 2) {
            reelsViewModel.fetchVideos()
        }
    }
}

@Composable
fun YouTubeShortPlayer(
    video: YouTubeSearchResult,
    isCurrentPage: Boolean,
    accentColor: Color,
    onAskMythic: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoId = video.id?.videoId

    var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(1248) }

    // Resolve Best Thumbnail
    val thumbnailUrl = video.snippet?.thumbnails?.maxres?.url
        ?: video.snippet?.thumbnails?.high?.url
        ?: video.snippet?.thumbnails?.medium?.url
        ?: if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

    // Manage Player View
    val playerView = remember(videoId) {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Initialize player with optimal IFrame options
    LaunchedEffect(playerView, videoId) {
        if (videoId != null && !isPlayerReady) {
            val iFrameOptions = IFramePlayerOptions.Builder()
                .controls(0)
                .rel(0)
                .ivLoadPolicy(3)
                .ccLoadPolicy(0)
                .fullscreen(0)
                .build()

            playerView.initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayerRef = youTubePlayer
                        isPlayerReady = true
                        hasError = false
                        if (isCurrentPage) {
                            youTubePlayer.loadVideo(videoId, 0f)
                            isPlaying = true
                        } else {
                            youTubePlayer.cueVideo(videoId, 0f)
                            isPlaying = false
                        }
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        when (state) {
                            PlayerConstants.PlayerState.PLAYING -> isPlaying = true
                            PlayerConstants.PlayerState.PAUSED,
                            PlayerConstants.PlayerState.ENDED -> isPlaying = false
                            else -> {}
                        }
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        hasError = true
                    }
                },
                iFrameOptions
            )
        }
    }

    // Play or pause depending on current page selection
    LaunchedEffect(isCurrentPage, isPlayerReady, videoId) {
        if (isPlayerReady && videoId != null) {
            if (isCurrentPage) {
                youTubePlayerRef?.loadVideo(videoId, 0f)
                isPlaying = true
            } else {
                youTubePlayerRef?.pause()
                isPlaying = false
            }
        }
    }

    // Lifecycle observer to pause playback when app is paused
    DisposableEffect(lifecycleOwner, playerView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    youTubePlayerRef?.pause()
                    isPlaying = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isCurrentPage && isPlayerReady) {
                        youTubePlayerRef?.play()
                        isPlaying = true
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    playerView.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerView.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (isPlayerReady) {
                    if (isPlaying) {
                        youTubePlayerRef?.pause()
                        isPlaying = false
                    } else {
                        youTubePlayerRef?.play()
                        isPlaying = true
                    }
                }
            }
    ) {
        // Thumbnail Backdrop
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = video.snippet?.title ?: "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Active Player Layer
        if (videoId != null && !hasError) {
            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Play/Pause Center Indicator
        AnimatedVisibility(
            visible = !isPlaying && isPlayerReady && !hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Error Fallback UI
        if (hasError && videoId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Video is restricted by publisher for in-app embedding",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Watch on YouTube", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Scrim Gradient for Readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = 1800f
                    )
                )
        )

        // Right-Side Interaction Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    isLiked = !isLiked
                    if (isLiked) likeCount++ else likeCount--
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isLiked) Color(0xFFFF4081).copy(alpha = 0.3f) else Color(0xFF222222).copy(alpha = 0.8f))
                        .border(1.dp, if (isLiked) Color(0xFFFF4081) else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLiked) "❤️" else "🤍", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$likeCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Share Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    val shareUrl = if (videoId != null) "https://youtube.com/shorts/$videoId" else "https://mythic.app"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, video.snippet?.title ?: "Mythic Heritage Reel")
                        putExtra(Intent.EXTRA_TEXT, "${video.snippet?.title ?: "Mythic Reel"}\n\nWatch on Mythic:\n$shareUrl")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222222).copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↗️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Ask Mythic Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAskMythic() }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f))
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ask AI", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Open in YouTube directly
            if (videoId != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                        context.startActivity(ytIntent)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCC0000).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("YouTube", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Bottom Details (Author & Title)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp, end = 90.dp)
        ) {
            val title = video.snippet?.title ?: ""
            val author = video.snippet?.channelTitle ?: "Mythic Heritage"

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor, CircleShape)
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        author.take(1).uppercase(),
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "@${author.replace(" ", "")}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

