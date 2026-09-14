package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.remote.HeritageArticle
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.defaultSponsoredCampaigns
import com.example.utils.AdMobBanner
import com.example.viewmodel.MythicViewModel

@Composable
fun FeedScreen(
    viewModel: MythicViewModel,
    onNavigateToArticle: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val accentColor = Color(0xFF86FC5C)
    val cardBackground = Color(0xFF141414)
    val borderColor = Color(0xFF262626)

    val articles by viewModel.feedArticles.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedArticleForDetail by remember { mutableStateOf<HeritageArticle?>(null) }

    val categories = listOf("All", "Ancient Cities", "Religious", "Architecture", "Nature", "Culture", "Wikipedia")

    val filteredArticles = remember(articles, selectedCategory, searchQuery) {
        articles.filter { article ->
            val matchesCategory = if (selectedCategory == "All") true else article.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                article.siteName.contains(searchQuery, ignoreCase = true) ||
                article.description.contains(searchQuery, ignoreCase = true) ||
                article.province.contains(searchQuery, ignoreCase = true) ||
                article.category.equals("Wikipedia", ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 72.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier.testTag("feed_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Heritage Stories",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Powered by Wikimedia REST API",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2E17))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${filteredArticles.size} Stories",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wikipedia Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search or query Wikipedia...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            isSearching = true
                            viewModel.searchWikipediaArticle(searchQuery) { success ->
                                isSearching = false
                                if (success) {
                                    selectedCategory = "All"
                                }
                            }
                        }
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Fetch Wikipedia Summary", tint = accentColor)
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        isSearching = true
                        viewModel.searchWikipediaArticle(searchQuery) { success ->
                            isSearching = false
                            if (success) {
                                selectedCategory = "All"
                            }
                        }
                    }
                }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF141414),
                unfocusedContainerColor = Color(0xFF141414),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("feed_search_bar")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accentColor else cardBackground)
                        .border(1.dp, if (isSelected) Color.Transparent else borderColor, RoundedCornerShape(14.dp))
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Articles List
        if (filteredArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No articles found for '$searchQuery'" else "Loading heritage articles...",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (searchQuery.isNotBlank()) {
                        Button(
                            onClick = {
                                isSearching = true
                                viewModel.searchWikipediaArticle(searchQuery) {
                                    isSearching = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Query Wikipedia for '$searchQuery'", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredArticles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        accentColor = accentColor,
                        onArticleClick = {
                            selectedArticleForDetail = article
                            viewModel.triggerArticleRead()
                        },
                        onLikeClick = {
                            viewModel.triggerArticleLike(article.id, article.initialLikes + 1)
                        }
                    )

                    // Insert sponsored campaign card after the 1st article
                    if (filteredArticles.indexOf(article) == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SponsoredBannerCard(
                            campaign = defaultSponsoredCampaigns[3]
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Insert Google AdMob Adaptive Banner after the 3rd article
                    if (filteredArticles.indexOf(article) == 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101310)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SPONSORED",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Google AdMob",
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                AdMobBanner()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedArticleForDetail?.let { article ->
        ArticleDetailDialog(
            article = article,
            accentColor = accentColor,
            onDismiss = { selectedArticleForDetail = null }
        )
    }
}

@Composable
fun ArticleCoilImage(
    imageModel: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFFB300)
) {
    com.example.ui.components.SmartFallbackImage(
        primaryUrl = imageModel,
        contentDescription = contentDescription,
        modifier = modifier,
        accentColor = accentColor,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ArticleCard(
    article: HeritageArticle,
    accentColor: Color,
    onArticleClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember(article.id) { mutableStateOf(false) }
    var likesCount by remember(article.id, article.initialLikes) { mutableIntStateOf(article.initialLikes) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242424), RoundedCornerShape(18.dp))
            .clickable { onArticleClick() }
            .testTag("article_card_${article.id}")
    ) {
        Column {
            // Real Image with Coil AsyncImage and Caching / Error Fallbacks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF1F1F1F))
            ) {
                ArticleCoilImage(
                    imageModel = article.getImageModel(context),
                    contentDescription = article.siteName,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = article.category.uppercase(),
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (article.unescoStatus.contains("UNESCO", ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF003366).copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "UNESCO 🏛️",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dark Bottom Gradient for Text Legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF131313))
                            )
                        )
                )
            }

            // Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = article.siteName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = article.description,
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Footer with Province & Interaction Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = article.province,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (!isLiked) {
                                        isLiked = true
                                        likesCount += 1
                                        onLikeClick()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = if (isLiked) "❤️" else "🤍",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$likesCount",
                                color = if (isLiked) accentColor else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Read Story Action
                        Text(
                            text = "Read Story →",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleDetailDialog(
    article: HeritageArticle,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                item {
                    // Banner Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        ArticleCoilImage(
                            imageModel = article.getImageModel(context),
                            contentDescription = article.siteName,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                        )

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .align(Alignment.TopStart)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        // Bottom Gradient for Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF0A0A0A))
                                    )
                                )
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Category & Status
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B2B16))
                                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = article.category,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (article.unescoStatus.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A2634))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = article.unescoStatus,
                                        color = Color(0xFF64B5F6),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = article.siteName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "📍 ${article.province}  •  ⏳ ${article.era}",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Facts Box
                        if (article.facts.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141414))
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚡ Key Highlights & Facts",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = article.facts,
                                        color = Color(0xFFDDDDDD),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Full Narrative Text
                        Text(
                            text = "Historical Overview",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = article.description,
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Wikipedia Link Button
                        val wikiTitle = article.siteName.replace(" ", "_")
                        val wikiUrl = "https://en.wikipedia.org/wiki/$wikiTitle"
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(accentColor, accentColor)))
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Wikipedia", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Full Article on Wikipedia", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF111111))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Close Story", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
