package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MythicViewModel
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun AuthScreen(
    viewModel: MythicViewModel,
    onAuthSuccess: () -> Unit
) {
    var showWelcome by remember { mutableStateOf(true) }
    var isLoginMode by remember { mutableStateOf(true) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val verificationSentEmail by viewModel.verificationSentEmail.collectAsState()
    
    val accentColor = Color(0xFF86FC5C) // Neon Green from design

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showWelcome) {
            // Welcome Screen matching the image
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Branded Logo
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_mythic_logo_brand),
                    contentDescription = "Mythic Logo",
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "MYTHIC",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Sri Lanka's AI Companion",
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    listOf("Mythic AI", "AR Scan", "Live Map").forEach { tag ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(tag, color = Color(0xFFCCCCCC), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { showWelcome = false; isLoginMode = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "I already have an account",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { showWelcome = false; isLoginMode = true }
                        .padding(16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            // Login / Signup Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (verificationSentEmail != null) {
                    // Email Verification Sent Screen
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verify Your Email",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "We've sent a verification link to:\n$verificationSentEmail",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onAuthSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("I've verified my email", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = if (isLoginMode) "Welcome Back" else "Join the Guild",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isLoginMode) "Enter your credentials to continue" else "Create your profile",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (!isLoginMode) {
                        val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            selectedImageUri = uri
                        }

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                                    Text("Pick Image", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            
                            if (isUploading) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = accentColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )
                    
                    if (isLoginMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = { showResetDialog = true }) {
                                Text("Forgot Password?", color = accentColor, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, password, onAuthSuccess)
                            } else {
                                if (selectedImageUri != null) {
                                    isUploading = true
                                    viewModel.uploadProfilePictureOnly(selectedImageUri!!, context) { url ->
                                        isUploading = false
                                        viewModel.signUp(username, email, password, avatarUrl = url, onSuccess = onAuthSuccess)
                                    }
                                } else {
                                    viewModel.signUp(username, email, password, onSuccess = onAuthSuccess)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoggingIn && !isUploading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isLoggingIn || isUploading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isLoginMode) "Sign In" else "Create Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { isLoginMode = !isLoginMode },
                        modifier = Modifier.testTag("toggle_auth_mode")
                    ) {
                        Text(
                            text = if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                            color = accentColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Demo Account Button
                    TextButton(
                        onClick = { viewModel.loginAsDemo(onAuthSuccess) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Try Demo Account", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(
                        onClick = { showWelcome = true }
                    ) {
                        Text("Back to Welcome", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Password Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showResetDialog = false 
                    resetEmail = ""
                    resetSuccessMessage = null
                },
                title = { Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter your email address below. We will send you a secure link to reset your account password.",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email Address", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (resetSuccessMessage != null) {
                            Text(
                                text = resetSuccessMessage!!,
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetEmail.isNotBlank()) {
                                viewModel.recoverPassword(resetEmail) {
                                    resetSuccessMessage = "Password recovery instructions sent! Please check your inbox."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send Recovery Email", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showResetDialog = false
                            resetEmail = ""
                            resetSuccessMessage = null
                        }
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF111111),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            )
        }
    }
}
