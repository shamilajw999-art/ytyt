import re

with open("app/src/main/java/com/example/ui/screens/ARScreen.kt", "r") as f:
    content = f.read()

old_perms = """    // Camera Permissions
    var hasCameraPermission by remember {
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

new_perms = """    // Permissions
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

with open("app/src/main/java/com/example/ui/screens/ARScreen.kt", "w") as f:
    f.write(content)
