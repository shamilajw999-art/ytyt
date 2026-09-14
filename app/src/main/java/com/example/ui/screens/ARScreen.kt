package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ScanHistoryEntity
import com.example.ui.components.GlassCard
import com.example.viewmodel.MythicViewModel
import com.example.viewmodel.SubscriptionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.abs

data class ARLandmark(
    val name: String,
    val lat: Double,
    val lng: Double,
    val era: String,
    val description: String,
    val isTreasure: Boolean = false,
    val imageUrl: String = "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600"
)

enum class ARScanMode {
    RADAR_HUD, VISION_SCANNER, MODEL_PLACER_3D
}

@Composable
fun ARScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeSubscription by subscriptionViewModel.activeSubscription.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    
    LaunchedEffect(Unit) {
        subscriptionViewModel.fetchSubscriptions()
    }
    
    BackHandler {
        onDismiss()
    }

    var activeMode by remember { mutableStateOf(ARScanMode.RADAR_HUD) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    // Gallery Picker for Vision AI
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    viewModel.scanWithGeminiVision(
                        base64Image = base64,
                        mimeType = mimeType,
                        defaultSiteName = "Scanned Heritage Site",
                        fallbackImageUrl = uri.toString()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // AR Landmarks dataset
    val landmarks = remember {
        listOf(
            ARLandmark(
                name = "Sigiriya Rock Fortress",
                lat = 7.9570,
                lng = 80.7603,
                era = "5th Century AD",
                description = "Ancient palace & fortress built atop a 200-meter sheer granite rock with famous frescoes and Lion Gate.",
                isTreasure = true,
                imageUrl = "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600"
            ),
            ARLandmark(
                name = "Ruwanwelisaya Stupa",
                lat = 8.3500,
                lng = 80.3967,
                era = "140 BC",
                description = "Colossal stupa constructed by King Dutugemunu, enshrining sacred relics of the Buddha.",
                imageUrl = "https://images.unsplash.com/photo-1600100397608-f010e9df0782?w=600"
            ),
            ARLandmark(
                name = "Galle Dutch Fort",
                lat = 6.0260,
                lng = 80.2168,
                era = "1588 AD",
                description = "South Asia's best-preserved fortified maritime citadel, guarding the southern coastline.",
                isTreasure = true,
                imageUrl = "https://images.unsplash.com/photo-1546708973-b339540b5162?w=600"
            ),
            ARLandmark(
                name = "Temple of the Tooth",
                lat = 7.2936,
                lng = 80.6413,
                era = "1595 AD",
                description = "Royal Buddhist palace shrine housing the Sacred Relic of the tooth of Gautama Buddha.",
                imageUrl = "https://images.unsplash.com/photo-1586861335167-e5223aadc9fe?w=600"
            ),
            ARLandmark(
                name = "Dambulla Cave Complex",
                lat = 7.8564,
                lng = 80.6485,
                era = "1st Century BC",
                description = "Living Buddhist cave sanctuary featuring 153 statues and ancient ceiling frescoes.",
                imageUrl = "https://images.unsplash.com/photo-1608958416744-8846c071d2b0?w=600"
            ),
            ARLandmark(
                name = "Nine Arch Bridge",
                lat = 6.8768,
                lng = 81.0494,
                era = "1921 AD",
                description = "Colonial railway viaduct in Ella hills crafted entirely from solid stone and mortar.",
                imageUrl = "https://images.unsplash.com/photo-1543872084-c7bd3822856f?w=600"
            )
        )
    }

    var selectedLandmark by remember { mutableStateOf<ARLandmark?>(null) }
    var selectedMockTarget by remember { mutableStateOf(landmarks[0]) }

    // 3D Heritage Model Datasets & Surface Anchors State
    val available3DModels = remember {
        listOf(
            Heritage3DModel(
                id = "3d_sigiriya",
                name = "Sigiriya Fortress",
                era = "5th Century AD",
                category = "Royal Palace Citadel",
                description = "Monolithic granite fortress peak with majestic twin lion paws gateway, water gardens, and royal summit palace.",
                unescoStatus = "UNESCO World Heritage Site",
                heightMeters = "180 m",
                iconEmoji = "🏰",
                primaryColor = Color(0xFFC88B37),
                secondaryColor = Color(0xFF7A5124),
                accentColor = Color(0xFFFFD700),
                modelType = Model3DType.SIGIRIYA_CITADEL,
                facts = listOf(
                    "Built by King Kashyapa (477–495 AD) as an impregnable summit stronghold.",
                    "Features world-famous 5th-century frescoes of heavenly maidens.",
                    "Original entrance was accessed through the gaping jaws of a giant brick lion."
                )
            ),
            Heritage3DModel(
                id = "3d_tooth_temple",
                name = "Temple of the Tooth",
                era = "1595 AD",
                category = "Royal Buddhist Shrine",
                description = "Sacred Kandyan palace complex housing the sacred tooth relic of Gautama Buddha, featuring golden octagonal roof.",
                unescoStatus = "UNESCO World Heritage Site",
                heightMeters = "24 m",
                iconEmoji = "⛩️",
                primaryColor = Color(0xFFFFC107),
                secondaryColor = Color(0xFF8D6E63),
                accentColor = Color(0xFFFFF8E1),
                modelType = Model3DType.TEMPLE_OF_TOOTH,
                facts = listOf(
                    "Houses the Sacred Relic of the Tooth of Gautama Buddha.",
                    "Features the iconic octagonal 'Pattirippuwa' tower built in 1802.",
                    "Central shrine features two-tiered golden roof and carved ivory arches."
                )
            ),
            Heritage3DModel(
                id = "3d_stupa",
                name = "Ruwanwelisaya Stupa",
                era = "140 BC",
                category = "Colossal Stupa Monument",
                description = "Sacred white hemispherical dome enshrining sacred relics, surrounded by a wall of 1,900 carved elephant heads.",
                unescoStatus = "UNESCO World Heritage Site",
                heightMeters = "103 m",
                iconEmoji = "🛕",
                primaryColor = Color(0xFFECEFF1),
                secondaryColor = Color(0xFFB0BEC5),
                accentColor = Color(0xFFFFD54F),
                modelType = Model3DType.RUWANWELISAYA_STUPA,
                facts = listOf(
                    "Constructed by King Dutugemunu in 140 BC as a sacred stupa monument.",
                    "Stands at 338 feet tall with a circumference of 950 feet.",
                    "Topped with a rock crystal pinnacle set in pure gold."
                )
            ),
            Heritage3DModel(
                id = "3d_buddha",
                name = "Gal Vihara Seated Buddha",
                era = "12th Century AD",
                category = "Granite Rock Sculpture",
                description = "Masterpiece rock-cut Buddha statue sculpted directly out of a single granite wall in Polonnaruwa.",
                unescoStatus = "UNESCO World Heritage Site",
                heightMeters = "4.6 m",
                iconEmoji = "🗿",
                primaryColor = Color(0xFF78909C),
                secondaryColor = Color(0xFF455A64),
                accentColor = Color(0xFF80DEEA),
                modelType = Model3DType.GAL_VIHARA_BUDDHA,
                facts = listOf(
                    "Commissioned by King Parakramabahu the Great in the 12th century.",
                    "Sculpted into a massive charnockite granite rock face.",
                    "Depicts Buddha in the Dhyana Mudra posture on a decorated lotus pedestal."
                )
            ),
            Heritage3DModel(
                id = "3d_bridge",
                name = "Demodara Nine Arch Bridge",
                era = "1921 AD",
                category = "Colonial Engineering Viaduct",
                description = "Iconic mountain rail bridge built entirely out of solid granite blocks and cement without a single piece of steel.",
                unescoStatus = "National Architectural Monument",
                heightMeters = "24 m",
                iconEmoji = "🌉",
                primaryColor = Color(0xFF6D4C41),
                secondaryColor = Color(0xFF3E2723),
                accentColor = Color(0xFF81C784),
                modelType = Model3DType.NINE_ARCH_BRIDGE,
                facts = listOf(
                    "Built during British Ceylon era connecting Ella and Demodara rail lines.",
                    "Constructed entirely without structural steel during World War I material shortages.",
                    "Measures 300 feet in length and 80 feet in height across a lush valley."
                )
            ),
            Heritage3DModel(
                id = "3d_crown",
                name = "King Kashyapa Royal Crown",
                era = "5th Century AD",
                category = "Royal Artifact",
                description = "Ornate 5th-century gold filigree royal crown set with Ceylon rubies, sapphires, and lotus crests.",
                unescoStatus = "Royal Treasure Artifact",
                heightMeters = "0.4 m",
                iconEmoji = "🧭",
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFDAA520),
                accentColor = Color(0xFFFF4081),
                modelType = Model3DType.ROYAL_CROWN_ARTIFACT,
                facts = listOf(
                    "Designed with ancient Anuradhapura goldsmith craftsmanship.",
                    "Features filigree lotus motifs symbolising royalty and purity.",
                    "Encrusted with natural blue sapphires from Ratnapura gem mines."
                )
            )
        )
    }

    var activeTemplateModel by remember { mutableStateOf(available3DModels[0]) }
    var placedAnchors by remember { mutableStateOf<List<PlacedModelAnchor>>(emptyList()) }
    var selectedAnchorId by remember { mutableStateOf<String?>(null) }
    var activeFactsModel by remember { mutableStateOf<Heritage3DModel?>(null) }
    var arPhotoNotice by remember { mutableStateOf<String?>(null) }

    // Compass Sensor (Azimuth)
    var azimuth by remember { mutableFloatStateOf(0f) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (deg < 0) deg += 360f
                    azimuth = deg
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Location listener
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = location
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (hasLocationPermission && locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, locationListener)
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                if (currentLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, locationListener)
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Collect scan result & vision state
    val isScanningWithVision by viewModel.isScanningWithVision.collectAsState()
    val visionScanError by viewModel.visionScanError.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val showScanResultDialog by viewModel.showScanResultDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("ar_screen_root")
    ) {
        // --- 1. Real CameraX Live View ---
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraInstance = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera & Location Required for AR",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mythic AR combines camera video, orientation compass sensors, and GPS to overlay Sri Lankan heritage monuments directly in your field of view.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D), contentColor = Color.Black)
                ) {
                    Text("Grant Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 2. Top Bar & Mode Switcher ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { onDismiss() }
                        .testTag("ar_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Mode Tabs Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeMode == ARScanMode.RADAR_HUD) Color(0xFF9AF04D) else Color.Transparent)
                            .clickable { activeMode = ARScanMode.RADAR_HUD }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🧭 AR Radar",
                            color = if (activeMode == ARScanMode.RADAR_HUD) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeMode == ARScanMode.VISION_SCANNER) Color(0xFF9AF04D) else Color.Transparent)
                            .clickable { activeMode = ARScanMode.VISION_SCANNER }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📸 Vision",
                            color = if (activeMode == ARScanMode.VISION_SCANNER) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeMode == ARScanMode.MODEL_PLACER_3D) Color(0xFF9AF04D) else Color.Transparent)
                            .clickable { activeMode = ARScanMode.MODEL_PLACER_3D }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🏛️ 3D Surface",
                            color = if (activeMode == ARScanMode.MODEL_PLACER_3D) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Flash Toggle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(
                            1.dp,
                            if (isFlashOn) Color(0xFF9AF04D) else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                        .clickable {
                            isFlashOn = !isFlashOn
                            cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isFlashOn) Color(0xFF9AF04D) else Color.White
                    )
                }
            }

            // Compass Azimuth Indicator Banner
            if (activeMode == ARScanMode.RADAR_HUD) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heading: ${azimuth.toInt()}° ",
                        color = Color(0xFF9AF04D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (azimuth.toInt()) {
                            in 338..360, in 0..22 -> "N (North)"
                            in 23..67 -> "NE"
                            in 68..112 -> "E (East)"
                            in 113..157 -> "SE"
                            in 158..202 -> "S (South)"
                            in 203..247 -> "SW"
                            in 248..292 -> "W (West)"
                            else -> "NW"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 3. Mode: AR RADAR HUD OVERLAY ---
        if (activeMode == ARScanMode.RADAR_HUD) {
            // Horizontal 360° Compass Radar Notch
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                // Central alignment crosshair
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(2.dp)
                        .height(24.dp)
                        .background(Color(0xFF9AF04D))
                )

                // Virtual reference position (defaults to Sri Lanka center if GPS pending)
                val userLat = currentLocation?.latitude ?: 7.8731
                val userLng = currentLocation?.longitude ?: 80.7718

                val myLoc = Location("").apply {
                    latitude = userLat
                    longitude = userLng
                }

                landmarks.forEach { landmark ->
                    val landmarkLoc = Location("").apply {
                        latitude = landmark.lat
                        longitude = landmark.lng
                    }

                    val bearingTo = myLoc.bearingTo(landmarkLoc)
                    var relBearing = bearingTo - azimuth
                    if (relBearing < -180) relBearing += 360
                    if (relBearing > 180) relBearing -= 360

                    val distanceKm = myLoc.distanceTo(landmarkLoc) / 1000f

                    // Visible in front 100° FOV
                    if (abs(relBearing) < 50) {
                        val bias = (relBearing / 50f).coerceIn(-0.95f, 0.95f)

                        Column(
                            modifier = Modifier
                                .align(BiasAlignment(bias, 0f))
                                .width(130.dp)
                                .clickable { selectedLandmark = landmark },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (landmark.isTreasure) Color(0xFFFFD700) else Color(0xFF9AF04D))
                                    .border(2.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (landmark.isTreasure) "🏺" else "🏛️",
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = landmark.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${String.format("%.1f", distanceKm)} km",
                                color = Color(0xFF9AF04D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (landmark.isTreasure) {
                                Text(
                                    text = "QUEST SITE",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFD700))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // AR Info Card on Landmark Tap
            if (selectedLandmark != null) {
                val lm = selectedLandmark!!
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = lm.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Era: ${lm.era}",
                                    color = Color(0xFF9AF04D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { selectedLandmark = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        Text(
                            text = lm.description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerScan(lm.name, lm.imageUrl)
                                    selectedLandmark = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9AF04D),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discover & Collect +50 XP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Mode: 3D SURFACE MODEL PLACER OVERLAY ---
        if (activeMode == ARScanMode.MODEL_PLACER_3D) {
            AR3DModelPlacerOverlay(
                availableModels = available3DModels,
                activeTemplateModel = activeTemplateModel,
                placedAnchors = placedAnchors,
                selectedAnchorId = selectedAnchorId,
                onSelectTemplate = { activeTemplateModel = it },
                onTapSurfaceGrid = { nx, ny ->
                    val hasAr3dScan = entitlements?.entitlements?.ar3dScan == true || activeSubscription != null
                    if (!hasAr3dScan && placedAnchors.size >= 1) {
                        Toast.makeText(context, "Upgrade to Pro to place unlimited 3D models!", Toast.LENGTH_LONG).show()
                    } else {
                        val newAnchor = PlacedModelAnchor(
                            model = activeTemplateModel,
                            normalizedX = nx,
                            normalizedY = ny,
                            isSelected = true
                        )
                        placedAnchors = placedAnchors.map { it.copy(isSelected = false) } + newAnchor
                        selectedAnchorId = newAnchor.id
                    }
                },
                onSelectAnchor = { id ->
                    selectedAnchorId = id
                    placedAnchors = placedAnchors.map { it.copy(isSelected = (it.id == id)) }
                },
                onUpdateAnchor = { updated ->
                    placedAnchors = placedAnchors.map { if (it.id == updated.id) updated else it }
                },
                onDeleteAnchor = { id ->
                    placedAnchors = placedAnchors.filter { it.id != id }
                    if (selectedAnchorId == id) {
                        selectedAnchorId = placedAnchors.lastOrNull()?.id
                    }
                },
                onClearAll = {
                    placedAnchors = emptyList()
                    selectedAnchorId = null
                },
                onShowFacts = { model ->
                    activeFactsModel = model
                },
                onTakeARPhoto = {
                    val selModel = placedAnchors.find { it.id == selectedAnchorId }?.model ?: activeTemplateModel
                    viewModel.triggerScan(selModel.name, "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600")
                    arPhotoNotice = "📸 AR Scene Captured! Saved 3D ${selModel.name} on surface."
                }
            )
        }

        // --- 5. Mode: VISION AI SCANNER OVERLAY ---
        if (activeMode == ARScanMode.VISION_SCANNER) {
            // Viewfinder Targeting Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(3.dp, Color(0xFF9AF04D), RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Center crosshair
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF9AF04D).copy(alpha = 0.6f))
                                .align(Alignment.Center)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFF9AF04D).copy(alpha = 0.6f))
                                .align(Alignment.Center)
                        )
                    }
                }

                Text(
                    text = "Align Sri Lankan monument within the frame",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Quick Landmark Simulation Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 125.dp)
            ) {
                Text(
                    text = "Quick Target Simulation:",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(landmarks) { target ->
                        val isSelected = target.name == selectedMockTarget.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isSelected) Color(0xFF9AF04D) else Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, Color(0xFF9AF04D).copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                .clickable { selectedMockTarget = target }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = target.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Capture & Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(bottom = 32.dp, top = 16.dp, start = 30.dp, end = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info button
                IconButton(
                    onClick = { selectedLandmark = selectedMockTarget },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Landmark Details",
                        tint = Color.White
                    )
                }

                // Shutter / Capture Trigger Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9AF04D))
                        .border(5.dp, Color.Black, CircleShape)
                        .clickable {
                            viewModel.triggerScan(selectedMockTarget.name, selectedMockTarget.imageUrl)
                        }
                        .testTag("scan_trigger_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(3.dp, Color.Black, CircleShape)
                    )
                }

                // Gallery Import
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                        .testTag("gallery_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 5. VISION SCAN LOADING OVERLAY ---
        if (isScanningWithVision) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF9AF04D),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Gemini Vision AI Scanning...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Identifying architecture, historical inscriptions, and heritage origins.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- 6. SUCCESSFUL SCAN DISCOVERY DIALOG ---
        if (showScanResultDialog && scanResult != null) {
            val res = scanResult!!
            AlertDialog(
                onDismissRequest = { viewModel.dismissScanResult() },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🎉", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Heritage Discovered!",
                            color = Color(0xFF9AF04D),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!res.imageUrl.isNullOrBlank()) {
                            com.example.ui.components.SmartFallbackImage(
                                primaryUrl = res.imageUrl,
                                contentDescription = res.siteName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                accentColor = Color(0xFF9AF04D)
                            )
                        }

                        Text(
                            text = res.siteName,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${res.province}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• ${res.era}",
                                color = Color(0xFF9AF04D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = res.description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )

                        // Reward Tag
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF9AF04D).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF9AF04D), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐ +50 Explorer XP Unlocked!",
                                color = Color(0xFF9AF04D),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissScanResult() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9AF04D),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- 7. FACTS DIALOG FOR 3D HERITAGE MODEL ---
        activeFactsModel?.let { model ->
            AlertDialog(
                onDismissRequest = { activeFactsModel = null },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = model.iconEmoji, fontSize = 28.sp)
                        Column {
                            Text(text = model.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "${model.category} • ${model.era}", color = model.primaryColor, fontSize = 12.sp)
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = model.description, color = Color.LightGray, fontSize = 13.sp)
                        HorizontalDivider(color = Color.DarkGray)
                        Text(text = "Historical Insights:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        model.facts.forEach { fact ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "•", color = model.accentColor, fontWeight = FontWeight.Bold)
                                Text(text = fact, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeFactsModel = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- 8. AR PHOTO CAPTURE TOAST / BANNER ---
        arPhotoNotice?.let { notice ->
            LaunchedEffect(notice) {
                kotlinx.coroutines.delay(3000L)
                arPhotoNotice = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 160.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF9AF04D))
                    .border(2.dp, Color.Black, RoundedCornerShape(100.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = notice,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ==========================================
// 3D AR SURFACE PLANE DETECTOR & MODEL PLACER
// ==========================================

enum class Model3DType {
    SIGIRIYA_CITADEL,
    TEMPLE_OF_TOOTH,
    RUWANWELISAYA_STUPA,
    GAL_VIHARA_BUDDHA,
    NINE_ARCH_BRIDGE,
    ROYAL_CROWN_ARTIFACT
}

data class Heritage3DModel(
    val id: String,
    val name: String,
    val era: String,
    val category: String,
    val description: String,
    val unescoStatus: String,
    val heightMeters: String,
    val iconEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val modelType: Model3DType,
    val facts: List<String>
)

data class PlacedModelAnchor(
    val id: String = java.util.UUID.randomUUID().toString(),
    val model: Heritage3DModel,
    var normalizedX: Float,
    var normalizedY: Float,
    var scale: Float = 1.0f,
    var yawRotationDegrees: Float = 0f,
    var elevationDp: Float = 0f,
    var isSelected: Boolean = true
)

@Composable
fun AR3DModelPlacerOverlay(
    availableModels: List<Heritage3DModel>,
    activeTemplateModel: Heritage3DModel,
    placedAnchors: List<PlacedModelAnchor>,
    selectedAnchorId: String?,
    onSelectTemplate: (Heritage3DModel) -> Unit,
    onTapSurfaceGrid: (Float, Float) -> Unit,
    onSelectAnchor: (String) -> Unit,
    onUpdateAnchor: (PlacedModelAnchor) -> Unit,
    onDeleteAnchor: (String) -> Unit,
    onClearAll: () -> Unit,
    onShowFacts: (Heritage3DModel) -> Unit,
    onTakeARPhoto: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_surface_grid")
    val gridPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grid_pulse"
    )
    val gridScanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_offset"
    )

    val selectedAnchor = placedAnchors.find { it.id == selectedAnchorId }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Surface Detection Plane Canvas & 3D Renderer ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(placedAnchors, activeTemplateModel) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val surfaceStartY = h * 0.35f
                        val surfaceHeight = h * 0.55f

                        // Check if tap hit an existing placed model anchor
                        var hitAnchorId: String? = null
                        for (anchor in placedAnchors.reversed()) {
                            val ax = w * anchor.normalizedX
                            val ay = surfaceStartY + surfaceHeight * anchor.normalizedY - (anchor.elevationDp * 2.5f)
                            val dist = kotlin.math.hypot(tapOffset.x - ax, tapOffset.y - ay)
                            if (dist < 90f * anchor.scale) {
                                hitAnchorId = anchor.id
                                break
                            }
                        }

                        if (hitAnchorId != null) {
                            onSelectAnchor(hitAnchorId)
                        } else if (tapOffset.y >= surfaceStartY) {
                            val nx = (tapOffset.x / w).coerceIn(0.1f, 0.9f)
                            val ny = ((tapOffset.y - surfaceStartY) / surfaceHeight).coerceIn(0.05f, 0.95f)
                            onTapSurfaceGrid(nx, ny)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val surfaceStartY = h * 0.35f
            val surfaceHeight = h * 0.55f
            val surfaceEndY = surfaceStartY + surfaceHeight

            // 1. Draw Perspective AR Surface Detection Mesh Grid
            val gridCols = 10
            val gridRows = 8

            // Horizontal perspective lines
            for (r in 0..gridRows) {
                val ratio = r.toFloat() / gridRows
                val perspectiveY = surfaceStartY + (ratio * ratio) * surfaceHeight
                val alpha = (0.2f + ratio * 0.5f) * gridPulseAlpha
                val xMargin = (1f - ratio) * (w * 0.25f)

                drawLine(
                    color = Color(0xFF9AF04D).copy(alpha = alpha),
                    start = Offset(xMargin, perspectiveY),
                    end = Offset(w - xMargin, perspectiveY),
                    strokeWidth = if (r == gridRows / 2) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Vertical perspective fan lines
            val vanishingX = w / 2f
            for (c in 0..gridCols) {
                val ratio = c.toFloat() / gridCols
                val bottomX = ratio * w
                val alpha = (0.2f + (1f - kotlin.math.abs(0.5f - ratio)) * 0.4f) * gridPulseAlpha

                drawLine(
                    color = Color(0xFF9AF04D).copy(alpha = alpha),
                    start = Offset(vanishingX + (bottomX - vanishingX) * 0.25f, surfaceStartY),
                    end = Offset(bottomX, surfaceEndY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Animated Surface Scanline Wave
            val scanY = surfaceStartY + gridScanlineOffset * surfaceHeight
            drawLine(
                color = Color(0xFFFFD700).copy(alpha = 0.6f * gridPulseAlpha),
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 3.dp.toPx()
            )

            // AR Point Cloud Tracking Particles (Simulating ARCore feature points)
            val particleCount = 28
            for (i in 0 until particleCount) {
                val pSeedX = ((i * 137.5f) % w)
                val pSeedY = surfaceStartY + ((i * 73.1f) % surfaceHeight)
                val pAlpha = (0.3f + (sin(i + gridScanlineOffset * 6.28f).toFloat() + 1f) * 0.35f) * gridPulseAlpha

                drawCircle(
                    color = Color(0xFF9AF04D).copy(alpha = pAlpha),
                    radius = (2f + (i % 3)).dp.toPx(),
                    center = Offset(pSeedX, pSeedY)
                )
            }

            // Target Reticle Crosshair at center if no anchors placed
            if (placedAnchors.isEmpty()) {
                val reticleX = w / 2f
                val reticleY = surfaceStartY + surfaceHeight * 0.5f

                drawCircle(
                    color = Color(0xFF9AF04D).copy(alpha = 0.7f),
                    radius = 24.dp.toPx(),
                    center = Offset(reticleX, reticleY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 4.dp.toPx(),
                    center = Offset(reticleX, reticleY)
                )
            }

            // 2. Render Placed 3D Virtual Heritage Models on Surface Plane
            placedAnchors.forEach { anchor ->
                val ax = w * anchor.normalizedX
                val ay = surfaceStartY + surfaceHeight * anchor.normalizedY
                val depthFactor = 0.65f + (ay - surfaceStartY) / surfaceHeight * 0.5f
                val baseScalePx = depthFactor * anchor.scale * 48.dp.toPx()

                // Ambient Ground Shadow on Surface Plane
                drawOval(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(ax - baseScalePx * 1.1f, ay - baseScalePx * 0.35f),
                    size = Size(baseScalePx * 2.2f, baseScalePx * 0.7f)
                )

                // Selection Ring on Surface Plane
                if (anchor.isSelected) {
                    drawOval(
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        topLeft = Offset(ax - baseScalePx * 1.25f, ay - baseScalePx * 0.45f),
                        size = Size(baseScalePx * 2.5f, baseScalePx * 0.9f),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Axis Direction Marker
                    val rad = (anchor.yawRotationDegrees * PI / 180f).toFloat()
                    val dx = cos(rad) * baseScalePx * 1.2f
                    val dy = sin(rad) * baseScalePx * 0.45f
                    drawLine(
                        color = Color(0xFF9AF04D),
                        start = Offset(ax, ay),
                        end = Offset(ax + dx, ay + dy),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Render 3D Model Volumetric Mesh at (ax, ay - elevation)
                val modelCenterY = ay - (anchor.elevationDp.dp.toPx())
                translate(left = ax, top = modelCenterY) {
                    rotate(degrees = anchor.yawRotationDegrees) {
                        draw3DModelGeometry(
                            modelType = anchor.model.modelType,
                            scalePx = baseScalePx,
                            primaryColor = anchor.model.primaryColor,
                            secondaryColor = anchor.model.secondaryColor,
                            accentColor = anchor.model.accentColor
                        )
                    }
                }
            }
        }

        // --- Surface Detection Status Banner ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .border(1.dp, Color(0xFF9AF04D).copy(alpha = 0.6f), RoundedCornerShape(100.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9AF04D))
                )
                Text(
                    text = if (placedAnchors.isEmpty()) "🟢 Surface Grid Active • Tap plane to place 3D model" else "🟢 ${placedAnchors.size} 3D Model(s) Anchored on Surface",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Bottom Control Drawer & Model Selector Carousel ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f), Color.Black)
                    )
                )
                .padding(bottom = 20.dp, top = 8.dp)
        ) {
            // Selected Model Editing Sliders Toolbar (if an anchor is selected)
            if (selectedAnchor != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF161616).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = selectedAnchor.model.iconEmoji, fontSize = 20.sp)
                            Column {
                                Text(
                                    text = selectedAnchor.model.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "3D AR Anchor Locked",
                                    color = Color(0xFF9AF04D),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onShowFacts(selectedAnchor.model) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Facts", tint = Color.White)
                            }
                            IconButton(
                                onClick = { onTakeARPhoto() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color(0xFF9AF04D))
                            }
                            IconButton(
                                onClick = { onDeleteAnchor(selectedAnchor.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Rotation Yaw & Scale Sliders Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Rotation Slider
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔄 Rotate: ${selectedAnchor.yawRotationDegrees.toInt()}°",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            Slider(
                                value = selectedAnchor.yawRotationDegrees,
                                onValueChange = { onUpdateAnchor(selectedAnchor.copy(yawRotationDegrees = it)) },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        // Scale Slider
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔍 Scale: ${String.format("%.1f", selectedAnchor.scale)}x",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            Slider(
                                value = selectedAnchor.scale,
                                onValueChange = { onUpdateAnchor(selectedAnchor.copy(scale = it)) },
                                valueRange = 0.3f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF9AF04D),
                                    activeTrackColor = Color(0xFF9AF04D)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Model Selection Carousel
            Text(
                text = "Select 3D Virtual Heritage Model:",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableModels) { model ->
                    val isSelected = model.id == activeTemplateModel.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF222222) else Color(0xFF141414)
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF9AF04D) else Color(0xFF333333),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTemplate(model) }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = model.iconEmoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = model.era,
                                color = model.primaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Actions Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearAll) {
                    Text("🧹 Clear Surface", color = Color.Gray, fontSize = 11.sp)
                }

                Button(
                    onClick = onTakeARPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D), contentColor = Color.Black),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AR Snapshot", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// 3D Geometry Rendering Engine for Volumetric Models
private fun DrawScope.draw3DModelGeometry(
    modelType: Model3DType,
    scalePx: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    when (modelType) {
        Model3DType.SIGIRIYA_CITADEL -> {
            // Monolithic Rock Base Block
            val rockPath = Path().apply {
                moveTo(-scalePx * 0.8f, 0f)
                lineTo(-scalePx * 0.6f, -scalePx * 1.0f)
                lineTo(scalePx * 0.6f, -scalePx * 1.0f)
                lineTo(scalePx * 0.8f, 0f)
                close()
            }
            drawPath(path = rockPath, color = secondaryColor)

            // Top Palace Terrace Tier
            val summitPath = Path().apply {
                moveTo(-scalePx * 0.5f, -scalePx * 1.0f)
                lineTo(-scalePx * 0.35f, -scalePx * 1.35f)
                lineTo(scalePx * 0.35f, -scalePx * 1.35f)
                lineTo(scalePx * 0.5f, -scalePx * 1.0f)
                close()
            }
            drawPath(path = summitPath, color = primaryColor)

            // Twin Lion Paws at entrance base
            drawCircle(color = primaryColor, radius = scalePx * 0.22f, center = Offset(-scalePx * 0.35f, -scalePx * 0.15f))
            drawCircle(color = primaryColor, radius = scalePx * 0.22f, center = Offset(scalePx * 0.35f, -scalePx * 0.15f))

            // Royal Flag Pinnacle
            drawLine(
                color = accentColor,
                start = Offset(0f, -scalePx * 1.35f),
                end = Offset(0f, -scalePx * 1.7f),
                strokeWidth = 3f
            )
            drawCircle(color = accentColor, radius = scalePx * 0.08f, center = Offset(0f, -scalePx * 1.7f))
        }

        Model3DType.TEMPLE_OF_TOOTH -> {
            // Octagonal Pattirippuwa Base
            val octPath = Path().apply {
                moveTo(-scalePx * 0.6f, 0f)
                lineTo(-scalePx * 0.6f, -scalePx * 0.6f)
                lineTo(-scalePx * 0.3f, -scalePx * 0.9f)
                lineTo(scalePx * 0.3f, -scalePx * 0.9f)
                lineTo(scalePx * 0.6f, -scalePx * 0.6f)
                lineTo(scalePx * 0.6f, 0f)
                close()
            }
            drawPath(path = octPath, color = secondaryColor)

            // Two-Tiered Kandyan Golden Roof
            val roof1 = Path().apply {
                moveTo(-scalePx * 0.8f, -scalePx * 0.9f)
                lineTo(0f, -scalePx * 1.35f)
                lineTo(scalePx * 0.8f, -scalePx * 0.9f)
                close()
            }
            drawPath(path = roof1, color = primaryColor)

            val roof2 = Path().apply {
                moveTo(-scalePx * 0.5f, -scalePx * 1.35f)
                lineTo(0f, -scalePx * 1.7f)
                lineTo(scalePx * 0.5f, -scalePx * 1.35f)
                close()
            }
            drawPath(path = roof2, color = accentColor)

            // Golden Pinnacle Tip
            drawCircle(color = Color.White, radius = scalePx * 0.09f, center = Offset(0f, -scalePx * 1.75f))
        }

        Model3DType.RUWANWELISAYA_STUPA -> {
            // Elephant Base Wall
            drawRect(
                color = secondaryColor,
                topLeft = Offset(-scalePx * 0.9f, -scalePx * 0.25f),
                size = Size(scalePx * 1.8f, scalePx * 0.25f)
            )

            // Hemispherical White Dome (Garbhaya)
            val domePath = Path().apply {
                moveTo(-scalePx * 0.75f, -scalePx * 0.25f)
                cubicTo(
                    -scalePx * 0.75f, -scalePx * 1.25f,
                    scalePx * 0.75f, -scalePx * 1.25f,
                    scalePx * 0.75f, -scalePx * 0.25f
                )
                close()
            }
            drawPath(path = domePath, color = primaryColor)

            // Square Hatharas Kotuwa
            drawRect(
                color = secondaryColor,
                topLeft = Offset(-scalePx * 0.22f, -scalePx * 1.25f),
                size = Size(scalePx * 0.44f, scalePx * 0.22f)
            )

            // Conical Spire & Crystal Pinnacle
            val spire = Path().apply {
                moveTo(-scalePx * 0.15f, -scalePx * 1.47f)
                lineTo(0f, -scalePx * 1.9f)
                lineTo(scalePx * 0.15f, -scalePx * 1.47f)
                close()
            }
            drawPath(path = spire, color = accentColor)
            drawCircle(color = Color.White, radius = scalePx * 0.08f, center = Offset(0f, -scalePx * 1.95f))
        }

        Model3DType.GAL_VIHARA_BUDDHA -> {
            // Granite Rock Wall Backdrop
            drawRect(
                color = secondaryColor,
                topLeft = Offset(-scalePx * 0.85f, -scalePx * 1.4f),
                size = Size(scalePx * 1.7f, scalePx * 1.4f)
            )

            // Lotus Pedestal
            drawOval(
                color = accentColor,
                topLeft = Offset(-scalePx * 0.5f, -scalePx * 0.35f),
                size = Size(scalePx * 1.0f, scalePx * 0.35f)
            )

            // Seated Buddha Figure
            val figure = Path().apply {
                moveTo(-scalePx * 0.35f, -scalePx * 0.35f)
                lineTo(0f, -scalePx * 1.1f)
                lineTo(scalePx * 0.35f, -scalePx * 0.35f)
                close()
            }
            drawPath(path = figure, color = primaryColor)

            // Head Halo
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = scalePx * 0.18f, center = Offset(0f, -scalePx * 1.1f))
        }

        Model3DType.NINE_ARCH_BRIDGE -> {
            // Green Terrain Hill Base
            drawArc(
                color = accentColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(-scalePx * 1.0f, -scalePx * 0.3f),
                size = Size(scalePx * 2.0f, scalePx * 0.6f)
            )

            // Bridge Arch Piers
            val archCount = 3
            for (i in 0 until archCount) {
                val cx = -scalePx * 0.5f + i * (scalePx * 0.5f)
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(cx - scalePx * 0.15f, -scalePx * 0.9f),
                    size = Size(scalePx * 0.3f, scalePx * 0.7f)
                )
                // Arch Portal cutout
                drawArc(
                    color = secondaryColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - scalePx * 0.12f, -scalePx * 0.6f),
                    size = Size(scalePx * 0.24f, scalePx * 0.4f)
                )
            }

            // Top Rail Track Bridge Deck
            drawRect(
                color = secondaryColor,
                topLeft = Offset(-scalePx * 0.95f, -scalePx * 1.05f),
                size = Size(scalePx * 1.9f, scalePx * 0.15f)
            )
        }

        Model3DType.ROYAL_CROWN_ARTIFACT -> {
            // Crown Base Ring Band
            drawRect(
                color = secondaryColor,
                topLeft = Offset(-scalePx * 0.6f, -scalePx * 0.3f),
                size = Size(scalePx * 1.2f, scalePx * 0.3f)
            )

            // Crown Lotus Crest Spires
            val crownPath = Path().apply {
                moveTo(-scalePx * 0.6f, -scalePx * 0.3f)
                lineTo(-scalePx * 0.4f, -scalePx * 1.2f)
                lineTo(-scalePx * 0.2f, -scalePx * 0.5f)
                lineTo(0f, -scalePx * 1.5f)
                lineTo(scalePx * 0.2f, -scalePx * 0.5f)
                lineTo(scalePx * 0.4f, -scalePx * 1.2f)
                lineTo(scalePx * 0.6f, -scalePx * 0.3f)
                close()
            }
            drawPath(path = crownPath, color = primaryColor)

            // Sapphire & Ruby Gem Jewels
            drawCircle(color = accentColor, radius = scalePx * 0.12f, center = Offset(0f, -scalePx * 0.9f))
            drawCircle(color = Color(0xFF29B6F6), radius = scalePx * 0.08f, center = Offset(-scalePx * 0.35f, -scalePx * 0.6f))
            drawCircle(color = Color(0xFF29B6F6), radius = scalePx * 0.08f, center = Offset(scalePx * 0.35f, -scalePx * 0.6f))
        }
    }
}
