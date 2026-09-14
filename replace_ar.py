import re

with open("app/src/main/java/com/example/ui/screens/ARScreen.kt", "r") as f:
    content = f.read()

# Replace imports
imports = """
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.unit.sp
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.Context
import kotlin.math.abs
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.BiasAlignment
"""

content = content.replace("import androidx.activity.result.contract.ActivityResultContracts", imports)

# Add Landmark data class
content = content.replace("@Composable\nfun ARScreen(", "data class Landmark(val name: String, val lat: Double, val lng: Double, val description: String)\n\n@Composable\nfun ARScreen(")

# Update permissions logic
old_perms = """    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }"""

new_perms = """    var hasCameraPermission by remember {
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
        contract = RequestMultiplePermissions(),
        onResult = { permissions -> 
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }"""

content = content.replace(old_perms, new_perms)

# Add sensor logic
old_showInfo = "var showInfo by remember { mutableStateOf(false) }"

new_sensor = """var showInfo by remember { mutableStateOf(false) }

    // Sensors & Location State
    var azimuth by remember { mutableStateOf(0f) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    
    val landmarks = remember { listOf(
        Landmark("Sigiriya Lion Rock", 7.9570, 80.7603, "Treasure Hunt 🏺"),
        Landmark("Ruwanwelisaya", 8.3500, 80.3967, "Ancient Stupa"),
        Landmark("Galle Fort", 6.0260, 80.2168, "Colonial Fort"),
        Landmark("Temple of the Tooth", 7.2936, 80.6413, "Sacred Relic")
    ) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = location
            }
        }
        try {
            if (hasLocationPermission) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
            try {
                locationManager.removeUpdates(locationListener)
            } catch (e: SecurityException) {}
        }
    }"""

content = content.replace(old_showInfo, new_sensor)

# Add AR UI HUD
old_infoOverlay = "// Info Overlay"

new_ar_hud = """// AR Navigation HUD
        if (hasLocationPermission && currentLocation != null) {
            // Draw compass/radar line at top
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp).fillMaxWidth().height(80.dp)) {
                // Central notch
                Box(modifier = Modifier.align(Alignment.TopCenter).width(2.dp).height(20.dp).background(Color.White))
                
                landmarks.forEach { landmark ->
                    val landmarkLoc = Location("").apply {
                        latitude = landmark.lat
                        longitude = landmark.lng
                    }
                    val bearingTo = currentLocation!!.bearingTo(landmarkLoc)
                    var relativeBearing = bearingTo - azimuth
                    if (relativeBearing < -180) relativeBearing += 360
                    if (relativeBearing > 180) relativeBearing -= 360
                    
                    val distance = currentLocation!!.distanceTo(landmarkLoc) / 1000f // km
                    
                    // Only show if it's within front 90 degrees (+/- 45)
                    if (abs(relativeBearing) < 45) {
                        // Map relative bearing to screen X offset
                        // -45 deg -> 0 (left edge), 45 deg -> screenWidth (right edge), 0 deg -> center
                        val screenWidthPct = (relativeBearing + 45) / 90f // 0.0 to 1.0
                        val alignBias = (screenWidthPct * 2f - 1f) // -1.0 to 1.0
                        
                        Column(
                            modifier = Modifier
                                .align(BiasAlignment(alignBias, 0f))
                                .width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (landmark.description.contains("Treasure")) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = landmark.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${String.format("%.1f", distance)} km",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (landmark.description.contains("Treasure")) {
                                Text(
                                    text = "ACTIVE HUNT",
                                    color = Color.Red,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Info Overlay"""

content = content.replace(old_infoOverlay, new_ar_hud)

with open("app/src/main/java/com/example/ui/screens/ARScreen.kt", "w") as f:
    f.write(content)
