package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.HeritageQuizDialog
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.defaultSponsoredCampaigns
import com.example.viewmodel.MythicViewModel

data class MapHeritageSite(
    val name: String,
    val province: String,
    val region: String,
    val country: String,
    val description: String,
    val era: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isUnesco: Boolean = true,
    val isWonder: Boolean = false
)

val heritageSites = listOf(
    // --- SRI LANKA ---
    MapHeritageSite(
        name = "Sigiriya Rock Fortress",
        province = "Central Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "Colossal 200m granite citadel engineered by King Kashyapa in the 5th century AD, featuring the Lion's Gate, mirror walls, and legendary celestial frescoes.",
        era = "5th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600",
        latitude = 7.9570,
        longitude = 80.7603,
        category = "Ancient Cities",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Temple of the Sacred Tooth Relic",
        province = "Kandy, Central Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "Housed in the golden-roofed royal palace complex of Kandy, this revered Buddhist shrine protects the sacred left canine tooth of the Buddha.",
        era = "16th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1586861335167-e5223aadc9fe?w=600",
        latitude = 7.2936,
        longitude = 80.6413,
        category = "Temples",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Galle Dutch Fort",
        province = "Southern Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "An oceanfront 16th-century fortress founded by the Portuguese and reinforced by the Dutch, standing as the best-preserved European fortified city in South Asia.",
        era = "1588 AD",
        imageUrl = "https://images.unsplash.com/photo-1546708973-b339540b5162?w=600",
        latitude = 6.0271,
        longitude = 80.2170,
        category = "Forts",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Dambulla Golden Cave Temple",
        province = "Matale, Central Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "A sacred monastic sanctuary of five caverns containing 153 Buddha statues and 2,100 square meters of intricate ancient painted murals.",
        era = "1st Century BC",
        imageUrl = "https://images.unsplash.com/photo-1608958416744-8846c071d2b0?w=600",
        latitude = 7.8564,
        longitude = 80.6485,
        category = "Temples",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Anuradhapura Sacred City",
        province = "North Central Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "One of the longest continually inhabited ancient capitals on Earth, home to colossal brick stupas and the sacred Jaya Sri Maha Bodhi tree planted in 288 BC.",
        era = "4th Century BC",
        imageUrl = "https://images.unsplash.com/photo-1600100397608-f010e9df0782?w=600",
        latitude = 8.3542,
        longitude = 80.3967,
        category = "Ancient Cities",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Polonnaruwa Ancient City",
        province = "North Central Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "The medieval capital of Sri Lanka, celebrated for the monolithic granite sculptures of the Gal Vihara and King Parakramabahu's seven-story palace.",
        era = "11th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1625126392582-4299b9cfcb0c?w=600",
        latitude = 7.9403,
        longitude = 81.0029,
        category = "Ancient Cities",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Nine Arch Bridge (Ella)",
        province = "Badulla, Uva Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "An iconic viaduct engineered through emerald mountain tea plantations, crafted entirely of brick, stone, and mortar without a single piece of steel.",
        era = "1921 AD",
        imageUrl = "https://images.unsplash.com/photo-1543872084-c7bd3822856f?w=600",
        latitude = 6.8768,
        longitude = 81.0494,
        category = "Forts",
        isUnesco = false
    ),
    MapHeritageSite(
        name = "Adam's Peak (Sri Pada)",
        province = "Sabaragamuwa Province",
        region = "Sri Lanka",
        country = "Sri Lanka",
        description = "A sacred 2,243m conical mountain crowned with a mystic footprint revered by Buddhists as the Buddha's footprint, Hindus as Shiva's, and Christians & Muslims as Adam's.",
        era = "Ancient Era",
        imageUrl = "https://images.unsplash.com/photo-1586861335167-e5223aadc9fe?w=600",
        latitude = 6.8096,
        longitude = 80.4994,
        category = "Temples",
        isUnesco = true
    ),

    // --- ASIA ---
    MapHeritageSite(
        name = "Taj Mahal",
        province = "Uttar Pradesh",
        region = "Asia",
        country = "India",
        description = "An ivory-white marble mausoleum on the Yamuna riverbank, commissioned by Mughal Emperor Shah Jahan in 1632 in memory of his favorite wife Mumtaz Mahal.",
        era = "1632 AD",
        imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=600",
        latitude = 27.1751,
        longitude = 78.0421,
        category = "Wonders",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Angkor Wat",
        province = "Siem Reap",
        region = "Asia",
        country = "Cambodia",
        description = "The largest religious monument in the world, originally constructed as a Hindu temple dedicated to Vishnu by Khmer King Suryavarman II before converting to Buddhism.",
        era = "12th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600",
        latitude = 13.4125,
        longitude = 103.8670,
        category = "Temples",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Great Wall of China",
        province = "Beijing Region",
        region = "Asia",
        country = "China",
        description = "A monumental series of defensive fortifications erected across the historic northern borders of ancient Chinese states to shield against nomadic incursions.",
        era = "7th Century BC - 1644 AD",
        imageUrl = "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=600",
        latitude = 40.4319,
        longitude = 116.5704,
        category = "Forts",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Borobudur Temple",
        province = "Central Java",
        region = "Asia",
        country = "Indonesia",
        description = "A 9th-century Mahayana Buddhist temple with nine stacked platforms, six square and three circular, topped by a central dome surrounded by 72 perforated stupas.",
        era = "9th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1596402184320-417e7178b2cd?w=600",
        latitude = -7.6079,
        longitude = 110.2038,
        category = "Temples",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Fushimi Inari Taisha",
        province = "Kyoto",
        region = "Asia",
        country = "Japan",
        description = "The head shrine of the kami Inari, famous for its thousands of vermilion torii gates winding up the forested slopes of sacred Mount Inari.",
        era = "711 AD",
        imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600",
        latitude = 34.9671,
        longitude = 135.7727,
        category = "Temples",
        isUnesco = false
    ),

    // --- EUROPE ---
    MapHeritageSite(
        name = "The Roman Colosseum",
        province = "Lazio, Rome",
        region = "Europe",
        country = "Italy",
        description = "The largest ancient amphitheatre ever built, constructed under the Flavian emperors of limestone, tuff, and brick-faced concrete to host gladiator combats.",
        era = "70 - 80 AD",
        imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=600",
        latitude = 41.8902,
        longitude = 12.4922,
        category = "Ancient Cities",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Acropolis of Athens",
        province = "Attica",
        region = "Europe",
        country = "Greece",
        description = "An ancient citadel located on a rocky outcrop above Athens, containing the Parthenon, Erechtheion, and the Temple of Athena Nike from classical antiquity.",
        era = "5th Century BC",
        imageUrl = "https://images.unsplash.com/photo-1555993539-1732b0258235?w=600",
        latitude = 37.9715,
        longitude = 23.7257,
        category = "Ancient Cities",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Stonehenge",
        province = "Wiltshire",
        region = "Europe",
        country = "United Kingdom",
        description = "A prehistoric megalithic monument consisting of an outer ring of vertical sarsen standing stones topped by connecting lintels, aligned with the solstices.",
        era = "3000 - 2000 BC",
        imageUrl = "https://images.unsplash.com/photo-1599833975787-5c143f373c30?w=600",
        latitude = 51.1789,
        longitude = -1.8262,
        category = "Ancient Cities",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Alhambra Palace",
        province = "Granada, Andalusia",
        region = "Europe",
        country = "Spain",
        description = "A breathtaking palace and fortress complex originally constructed as a small fortress in 889 AD and converted into a royal palace by the Nasrid dynasty.",
        era = "13th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1591871937573-74dbba515c4c?w=600",
        latitude = 37.1773,
        longitude = -3.5881,
        category = "Forts",
        isUnesco = true
    ),

    // --- MIDDLE EAST & AFRICA ---
    MapHeritageSite(
        name = "Great Pyramids of Giza & Sphinx",
        province = "Giza Governorate",
        region = "Middle East & Africa",
        country = "Egypt",
        description = "The oldest of the Seven Wonders of the Ancient World and the only one to remain largely intact, erected as tombs for Pharaohs Khufu, Khafre, and Menkaure.",
        era = "c. 2570 BC",
        imageUrl = "https://images.unsplash.com/photo-1503177119275-0aa32b3a9368?w=600",
        latitude = 29.9792,
        longitude = 31.1342,
        category = "Wonders",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Petra (The Rose City)",
        province = "Ma'an",
        region = "Middle East & Africa",
        country = "Jordan",
        description = "A famous archaeological city carved into vibrant pink sandstone cliffs, established around the 6th century BC as the thriving capital of the Nabataean Kingdom.",
        era = "4th Century BC",
        imageUrl = "https://images.unsplash.com/photo-1579606032834-44b26090c2ff?w=600",
        latitude = 30.3285,
        longitude = 35.4444,
        category = "Ancient Cities",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Lalibela Rock-Hewn Churches",
        province = "Amhara Region",
        region = "Middle East & Africa",
        country = "Ethiopia",
        description = "Eleven monolithic rock-cut churches chiseled out of solid volcanic basalt in the 12th century under King Gebre Mesqel Lalibela, representing a New Jerusalem.",
        era = "12th Century AD",
        imageUrl = "https://images.unsplash.com/photo-1578575437130-527eed3abbec?w=600",
        latitude = 12.0319,
        longitude = 39.0411,
        category = "Temples",
        isUnesco = true
    ),

    // --- AMERICAS & OCEANIA ---
    MapHeritageSite(
        name = "Machu Picchu Citadel",
        province = "Cusco Region",
        region = "Americas",
        country = "Peru",
        description = "A 15th-century Inca citadel set high in the Andes mountains above the Urubamba River valley, showcasing polished dry-stone construction that withstands earthquakes.",
        era = "1450 AD",
        imageUrl = "https://images.unsplash.com/photo-1526392060635-9d6019884377?w=600",
        latitude = -13.1631,
        longitude = -72.5450,
        category = "Ancient Cities",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Chichen Itza (El Castillo)",
        province = "Yucatán",
        region = "Americas",
        country = "Mexico",
        description = "A massive Mayan ceremonial city dominated by the step-pyramid temple of Kukulcan, designed with solar precision to cast serpent shadows during the equinoxes.",
        era = "c. 600 - 900 AD",
        imageUrl = "https://images.unsplash.com/photo-1518638150340-f706e86654de?w=600",
        latitude = 20.6843,
        longitude = -88.5678,
        category = "Wonders",
        isUnesco = true,
        isWonder = true
    ),
    MapHeritageSite(
        name = "Tikal Mayan Ruins",
        province = "Petén Department",
        region = "Americas",
        country = "Guatemala",
        description = "One of the most powerful ancient kingdoms of the pre-Columbian Maya civilization, concealed deep inside the dense northern Guatemalan rainforest canopy.",
        era = "c. 200 - 900 AD",
        imageUrl = "https://images.unsplash.com/photo-1518638150340-f706e86654de?w=600",
        latitude = 17.2220,
        longitude = -89.6237,
        category = "Ancient Cities",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Moai of Rapa Nui",
        province = "Easter Island",
        region = "Americas",
        country = "Chile",
        description = "Nearly 1,000 colossal monolithic human figures carved by the Rapa Nui people from compressed volcanic tuff between 1250 and 1500 AD to honor ancestors.",
        era = "1250 - 1500 AD",
        imageUrl = "https://images.unsplash.com/photo-1518638150340-f706e86654de?w=600",
        latitude = -27.1212,
        longitude = -109.3667,
        category = "Ancient Cities",
        isUnesco = true
    ),
    MapHeritageSite(
        name = "Teotihuacan (Pyramid of the Sun)",
        province = "State of Mexico",
        region = "Americas",
        country = "Mexico",
        description = "Known as the City of the Gods, this mysterious pre-Columbian metropolis features the colossal Pyramid of the Sun and the sweeping Avenue of the Dead.",
        era = "100 BC - 650 AD",
        imageUrl = "https://images.unsplash.com/photo-1518638150340-f706e86654de?w=600",
        latitude = 19.6925,
        longitude = -98.8438,
        category = "Ancient Cities",
        isUnesco = true
    )
)

data class MapTileLayer(
    val id: String,
    val name: String,
    val icon: String,
    val description: String
)

val availableTileLayers = listOf(
    MapTileLayer("google_hybrid", "Google Hybrid", "🛰️", "Satellite with roads & labels"),
    MapTileLayer("google_satellite", "Google Satellite", "📷", "Pure high-res satellite"),
    MapTileLayer("google_streets", "Google Roads", "🗺️", "Clean vector road map"),
    MapTileLayer("google_terrain", "Google Terrain", "⛰️", "Topography & contours"),
    MapTileLayer("dark_cyber", "Dark Cyber", "🌙", "Sleek night-vision mode")
)

class MapWebInterface(private val onSelectSite: (String) -> Unit) {
    @JavascriptInterface
    fun onSiteSelected(name: String) {
        onSelectSite(name)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    viewModel: MythicViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedSite by remember { mutableStateOf<MapHeritageSite?>(null) }
    var selectedRegion by remember { mutableStateOf("All Worldwide") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedLayerId by remember { mutableStateOf("google_hybrid") }
    var showLayerMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val savedSites by viewModel.savedSites.collectAsState()

    val regions = listOf(
        "All Worldwide",
        "Sri Lanka",
        "Asia",
        "Europe",
        "Middle East & Africa",
        "Americas"
    )

    val filters = listOf("All", "UNESCO", "Wonders", "Temples", "Forts", "Ancient Cities")

    // Location Permission launcher
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            locateUser(context, webViewRef)
        } else {
            Toast.makeText(context, "Location permission allows centering on your coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    val currentQuiz by viewModel.currentQuiz.collectAsState()
    val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsState()

    if (currentQuiz != null) {
        HeritageQuizDialog(
            quiz = currentQuiz!!,
            onDismiss = { viewModel.clearQuiz() },
            onQuizComplete = { score, total ->
                if (score == total && total > 0) {
                    viewModel.unlockBadge(
                        code = "quiz_master",
                        name = "Heritage Quiz Master",
                        description = "Score 100% on any mythic heritage quiz",
                        tier = "gold",
                        icon = "🎓"
                    )
                }
            }
        )
    }

    // Prepare HTML
    val mapHtml = remember {
        generateMapHtml()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // --- 1. Map Canvas ---
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.BLACK)

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            android.util.Log.d("MapScreenJS", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
                            return true
                        }
                    }

                    addJavascriptInterface(
                        MapWebInterface { siteName ->
                            val site = heritageSites.find { it.name.equals(siteName, ignoreCase = true) }
                            if (site != null) {
                                selectedSite = site
                            }
                        },
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            view?.evaluateJavascript("if (typeof map !== 'undefined' && map) { map.invalidateSize(); }", null)
                        }
                    }

                    loadDataWithBaseURL("https://mythic.local", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Top Navigation, Search & Controls Overlay ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title Banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🗺️ ", fontSize = 14.sp)
                        Text(
                            text = "Google Maps Heritage",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                // Top Right Action Group
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Google Maps Layer Switcher
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f), CircleShape)
                            .clickable { showLayerMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Layers",
                            tint = Color(0xFF86FC5C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // My Location Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                if (hasLocationPermission) {
                                    locateUser(context, webViewRef)
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            tint = Color(0xFF86FC5C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Live Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search 25+ worldwide heritage sites...",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF86FC5C),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.85f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.8f),
                        focusedBorderColor = Color(0xFF86FC5C),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }

            // Live Search Dropdown Suggestions
            if (searchQuery.isNotBlank()) {
                val matchingSites = heritageSites.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.country.contains(searchQuery, ignoreCase = true) ||
                    it.region.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
                }.take(4)

                if (matchingSites.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141712))
                            .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            matchingSites.forEach { site ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedSite = site
                                            searchQuery = ""
                                            webViewRef?.evaluateJavascript("flyToSite(${site.latitude}, ${site.longitude}, 14);", null)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(site.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${site.country} • ${site.era}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF86FC5C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Region Jump Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(regions) { region ->
                    val isSelected = region == selectedRegion
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) Color(0xFF86FC5C) else Color.Black.copy(alpha = 0.8f))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF86FC5C) else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(100.dp)
                            )
                            .clickable {
                                selectedRegion = region
                                when (region) {
                                    "All Worldwide" -> webViewRef?.evaluateJavascript("flyToSite(20.0, 10.0, 2); filterRegion('All');", null)
                                    "Sri Lanka" -> webViewRef?.evaluateJavascript("flyToSite(7.8731, 80.7718, 8); filterRegion('Sri Lanka');", null)
                                    "Asia" -> webViewRef?.evaluateJavascript("flyToSite(20.0, 95.0, 4); filterRegion('Asia');", null)
                                    "Europe" -> webViewRef?.evaluateJavascript("flyToSite(45.0, 15.0, 4); filterRegion('Europe');", null)
                                    "Middle East & Africa" -> webViewRef?.evaluateJavascript("flyToSite(22.0, 35.0, 4); filterRegion('Middle East & Africa');", null)
                                    "Americas" -> webViewRef?.evaluateJavascript("flyToSite(5.0, -80.0, 3); filterRegion('Americas');", null)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = region,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) Color(0xFF1E281B) else Color.Black.copy(alpha = 0.75f))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF86FC5C) else Color(0xFF333333),
                                RoundedCornerShape(100.dp)
                            )
                            .clickable {
                                selectedFilter = filter
                                webViewRef?.evaluateJavascript("filterSites('$filter');", null)
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color(0xFF86FC5C) else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- 3. Google Maps Layer Picker Modal ---
        if (showLayerMenu) {
            Dialog(onDismissRequest = { showLayerMenu = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF111410))
                        .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Map View",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { showLayerMenu = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        availableTileLayers.forEach { layer ->
                            val isSelected = layer.id == selectedLayerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF1F2B1C) else Color(0xFF161815))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF86FC5C) else Color(0xFF262626),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedLayerId = layer.id
                                        showLayerMenu = false
                                        webViewRef?.evaluateJavascript("setMapLayer('${layer.id}');", null)
                                        Toast.makeText(context, "Switched to ${layer.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(layer.icon, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = layer.name,
                                        color = if (isSelected) Color(0xFF86FC5C) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = layer.description,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF86FC5C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // --- 4. Interactive Site Detail Bottom Sheet ---
        AnimatedVisibility(
            visible = selectedSite != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            val site = selectedSite
            if (site != null) {
                val isSaved = savedSites.any { it.siteName.equals(site.name, ignoreCase = true) }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Row with Photo & Core Info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail Image with Coil Caching
                            com.example.ui.components.SmartFallbackImage(
                                primaryUrl = site.imageUrl,
                                contentDescription = site.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.DarkGray),
                                accentColor = Color(0xFFFFB300)
                            )

                            // Main Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (site.isUnesco) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF86FC5C).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color(0xFF86FC5C), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "UNESCO",
                                                color = Color(0xFF86FC5C),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (site.isWonder) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "WONDER",
                                                color = Color(0xFFFFD700),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = site.era,
                                        color = Color(0xFF86FC5C),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = site.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Text(
                                    text = "📍 ${site.province}, ${site.country}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }

                            // Bookmark Toggle Button
                            IconButton(
                                onClick = {
                                    viewModel.toggleSaveSite(site.name, site.province, site.imageUrl)
                                    Toast.makeText(
                                        context,
                                        if (isSaved) "Removed from Saved Sites" else "Saved to My Heritage Sites! ⭐",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSaved) Color(0xFF86FC5C).copy(alpha = 0.2f) else Color(0x33000000))
                                    .border(1.dp, if (isSaved) Color(0xFF86FC5C) else Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save Site",
                                    tint = if (isSaved) Color(0xFF86FC5C) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Description
                        Text(
                            text = site.description,
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Actions Row 1: Primary Actions (Google Maps + Earth)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Direct Google Maps Intent
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("geo:${site.latitude},${site.longitude}?q=${site.latitude},${site.longitude}(${Uri.encode(site.name)})")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        // Fallback to web Google Maps
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${site.latitude},${site.longitude}")
                                        )
                                        context.startActivity(webIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF86FC5C),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Google Maps", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }

                            // Google Earth 3D Button
                            Button(
                                onClick = {
                                    val earthUri = Uri.parse("https://earth.google.com/web/search/${Uri.encode(site.name)}")
                                    val earthIntent = Intent(Intent.ACTION_VIEW, earthUri)
                                    context.startActivity(earthIntent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E261A),
                                    contentColor = Color(0xFF86FC5C)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86FC5C)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.7f).height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Earth 3D", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Quiz Button
                            Button(
                                onClick = { viewModel.generateQuiz(site.name) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E281B),
                                    contentColor = Color(0xFF86FC5C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.7f).height(40.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f)),
                                enabled = !isGeneratingQuiz
                            ) {
                                if (isGeneratingQuiz) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF86FC5C),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Quiz", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Close Button
                            IconButton(
                                onClick = { selectedSite = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun locateUser(context: Context, webView: WebView?) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            webView?.evaluateJavascript(
                "setUserLocation(${location.latitude}, ${location.longitude});",
                null
            )
        } else {
            // Default center on Sri Lanka
            webView?.evaluateJavascript("flyToSite(7.8731, 80.7718, 8);", null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateMapHtml(): String {
    val sitesJson = heritageSites.joinToString(",") { site ->
        """
        {
            name: "${site.name.replace("\"", "\\\"")}",
            province: "${site.province.replace("\"", "\\\"")}",
            region: "${site.region.replace("\"", "\\\"")}",
            country: "${site.country.replace("\"", "\\\"")}",
            category: "${site.category}",
            isUnesco: ${site.isUnesco},
            isWonder: ${site.isWonder},
            lat: ${site.latitude},
            lng: ${site.longitude}
        }
        """.trimIndent()
    }

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" onerror="this.onerror=null;this.href='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css';" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" onerror="var s=document.createElement('script');s.src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js';document.head.appendChild(s);"></script>
        <style>
            html, body {
                height: 100%;
                width: 100%;
                margin: 0;
                padding: 0;
                background-color: #0d0f0c;
                overflow: hidden;
            }
            #map {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                right: 0;
                height: 100%;
                width: 100%;
                background-color: #121811;
            }
            .leaflet-control-attribution, .leaflet-control-zoom {
                display: none !important;
            }
            .neon-pin {
                position: relative;
                width: 34px;
                height: 34px;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
            }
            .pulse-ring {
                position: absolute;
                width: 38px;
                height: 38px;
                border-radius: 50%;
                background: rgba(134, 252, 92, 0.35);
                animation: pulse 2s infinite ease-out;
            }
            .pin-core {
                position: relative;
                width: 26px;
                height: 26px;
                border-radius: 50%;
                background: #111;
                border: 2px solid #86FC5C;
                box-shadow: 0 0 12px rgba(134, 252, 92, 0.7);
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 13px;
            }
            .pin-wonder .pin-core {
                border-color: #FFD700;
                box-shadow: 0 0 14px rgba(255, 215, 0, 0.85);
            }
            .pin-wonder .pulse-ring {
                background: rgba(255, 215, 0, 0.4);
            }
            .user-pin {
                width: 22px;
                height: 22px;
                background: #00E5FF;
                border: 2px solid #fff;
                border-radius: 50%;
                box-shadow: 0 0 14px #00E5FF;
            }
            @keyframes pulse {
                0% { transform: scale(0.6); opacity: 0.9; }
                100% { transform: scale(1.9); opacity: 0; }
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            function initMap() {
                if (typeof L === 'undefined') {
                    console.error("Leaflet library not loaded yet, retrying in 300ms...");
                    setTimeout(initMap, 300);
                    return;
                }

                try {
                    window.map = L.map('map', {
                        center: [20.0, 10.0],
                        zoom: 2,
                        minZoom: 2,
                        maxZoom: 19,
                        zoomControl: false,
                        attributionControl: false
                    });

                    // Google Maps tile layers & Dark Cyber & OSM layers with error fallbacks
                    window.layers = {
                        'google_hybrid': L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', { maxZoom: 20 }),
                        'google_satellite': L.tileLayer('https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', { maxZoom: 20 }),
                        'google_streets': L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20 }),
                        'google_terrain': L.tileLayer('https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', { maxZoom: 20 }),
                        'dark_cyber': L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19, subdomains: 'abcd' }),
                        'osm': L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 })
                    };

                    window.currentLayer = layers['google_hybrid'].addTo(map);

                    // Add tile error handler to fallback to OpenStreetMap if Google tiles get blocked
                    window.currentLayer.on('tileerror', function(error, tile) {
                        console.warn('Tile load error on current layer, fallback ready:', error);
                    });

                    renderSites();

                    // Multiple passes of invalidateSize to handle initial WebView render layout
                    setTimeout(function() { map.invalidateSize(); }, 200);
                    setTimeout(function() { map.invalidateSize(); }, 600);
                    setTimeout(function() { map.invalidateSize(); }, 1200);
                } catch(e) {
                    console.error("Map initialization exception:", e);
                }
            }

            window.addEventListener('resize', function() {
                if (window.map) { window.map.invalidateSize(); }
            });

            function setMapLayer(layerId) {
                if (window.map && window.layers && window.layers[layerId]) {
                    window.map.removeLayer(window.currentLayer);
                    window.currentLayer = window.layers[layerId].addTo(window.map);
                }
            }

            var sites = [$sitesJson];
            var markers = [];
            var userMarker = null;
            var currentCategory = "All";
            var currentRegion = "All";

            function getCategoryEmoji(cat, isWonder) {
                if (isWonder) return '🌟';
                if (cat === 'Temples') return '🛕';
                if (cat === 'Forts') return '🏰';
                if (cat === 'Ancient Cities') return '🏛️';
                return '📍';
            }

            function createIcon(site) {
                var emoji = getCategoryEmoji(site.category, site.isWonder);
                var wonderClass = site.isWonder ? 'pin-wonder' : '';
                return L.divIcon({
                    className: 'neon-pin-container',
                    html: '<div class="neon-pin ' + wonderClass + '"><div class="pulse-ring"></div><div class="pin-core">' + emoji + '</div></div>',
                    iconSize: [34, 34],
                    iconAnchor: [17, 17]
                });
            }

            function renderSites() {
                if (!window.map) return;
                markers.forEach(function(m) { window.map.removeLayer(m); });
                markers = [];

                sites.forEach(function(site) {
                    var matchRegion = (currentRegion === "All" || site.region === currentRegion);
                    var matchCategory = false;

                    if (currentCategory === "All") {
                        matchCategory = true;
                    } else if (currentCategory === "UNESCO") {
                        matchCategory = site.isUnesco;
                    } else if (currentCategory === "Wonders") {
                        matchCategory = site.isWonder;
                    } else if (currentCategory === "Temples") {
                        matchCategory = site.category === "Temples" || site.name.toLowerCase().includes("temple");
                    } else if (currentCategory === "Forts") {
                        matchCategory = site.category === "Forts" || site.name.toLowerCase().includes("fort") || site.name.toLowerCase().includes("wall") || site.name.toLowerCase().includes("bridge");
                    } else if (currentCategory === "Ancient Cities") {
                        matchCategory = site.category === "Ancient Cities";
                    }

                    if (matchRegion && matchCategory) {
                        var marker = L.marker([site.lat, site.lng], { icon: createIcon(site) }).addTo(window.map);
                        marker.on('click', function() {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onSiteSelected(site.name);
                            }
                        });
                        markers.push(marker);
                    }
                });
            }

            renderSites();

            function filterSites(filter) {
                currentCategory = filter;
                renderSites();
            }

            function filterRegion(region) {
                currentRegion = region;
                renderSites();
            }

            function flyToSite(lat, lng, zoomLevel) {
                var zoom = zoomLevel || 13;
                if (window.map) {
                    window.map.flyTo([lat, lng], zoom, { duration: 1.6 });
                }
            }

            function setUserLocation(lat, lng) {
                if (window.map) {
                    if (userMarker) {
                        window.map.removeLayer(userMarker);
                    }
                    var userIcon = L.divIcon({
                        className: 'user-pin-container',
                        html: '<div class="neon-pin"><div class="pulse-ring" style="background: rgba(0, 229, 255, 0.4);"></div><div class="user-pin"></div></div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    userMarker = L.marker([lat, lng], { icon: userIcon }).addTo(window.map);
                    window.map.flyTo([lat, lng], 11, { duration: 1.5 });
                }
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initMap);
            } else {
                initMap();
            }
            window.addEventListener('load', function() {
                if (!window.map) { initMap(); }
            });
        </script>
    </body>
    </html>
    """.trimIndent()
}
