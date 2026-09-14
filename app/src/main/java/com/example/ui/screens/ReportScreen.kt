package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.remote.SupabaseReport
import com.example.ui.components.GlassSurface
import com.example.viewmodel.MythicViewModel
import java.util.UUID

@Composable
fun ReportScreen(
    viewModel: MythicViewModel,
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var siteName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var priority by remember { mutableFloatStateOf(0.5f) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedImageUri = uri
    }
    
    val currentUser by viewModel.currentUser.collectAsState()
    val recentReports by viewModel.recentReports.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRecentReports()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 40.dp)
    ) {
        item {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.DarkGray.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Report Broken Site",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Help us protect Sri Lanka's heritage by reporting sites that need urgent attention.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Red blurred neon-style warning box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF3A1111).copy(alpha = 0.5f))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFFF4D4D).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Misleading reports may lead to account suspension. Please provide accurate details.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFFD1D1),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text("Site Name", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Detailed Report / Description", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Image Picker Section
                    Text("Attach Photos", color = Color.White, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.LightGray)
                                Text("Add Photo of Broken Site", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Urgency Level: ${(priority * 10).toInt()}", color = Color.White)
                    Slider(
                        value = priority,
                        onValueChange = { priority = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF9AF04D),
                            activeTrackColor = Color(0xFF9AF04D)
                        )
                    )

                    Button(
                        onClick = {
                            if (siteName.isBlank() || reason.isBlank()) {
                                Toast.makeText(context, "Please provide site name and description.", Toast.LENGTH_SHORT).show()
                            } else {
                                // 1. Submit to Supabase
                                viewModel.submitReport(
                                    SupabaseReport(
                                        id = UUID.randomUUID().toString(),
                                        userId = currentUser?.id ?: "guest",
                                        siteName = siteName,
                                        category = "Broken Heritage Site",
                                        description = reason,
                                        imageUrl = selectedImageUri?.toString(),
                                        priority = priority,
                                        createdAt = null
                                    )
                                )
                                
                                // 2. Trigger Email Intent with optional Attachment
                                val emailIntent = if (selectedImageUri != null) {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                } else {
                                    Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:coderiders.sl@gmail.com")
                                    }
                                }.apply {
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("coderiders.sl@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "BROKEN HERITAGE SITE REPORT: $siteName")
                                    putExtra(Intent.EXTRA_TEXT, """
                                        Mythic Ceylon - Heritage Report
                                        ------------------------------
                                        Site Name: $siteName
                                        Reported By: ${currentUser?.email ?: "Guest User"}
                                        Urgency: ${(priority * 10).toInt()}/10
                                        
                                        Description:
                                        $reason
                                        
                                        [This report was generated via the Mythic Ceylon App]
                                    """.trimIndent())
                                }
                                
                                try {
                                    val chooserTitle = if (selectedImageUri != null) "Send Report Email (With Attachment)" else "Send Report Email"
                                    context.startActivity(Intent.createChooser(emailIntent, chooserTitle))
                                    viewModel.unlockBadge("site_reporter", "Heritage Guardian", "Report a broken or endangered heritage site", "silver", "🛡️")
                                    Toast.makeText(context, "Report sent! Thank you for protecting our heritage.", Toast.LENGTH_LONG).show()
                                    onBack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Email client not found. Report saved to database.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Report to Code Riders", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Recent Reports", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(recentReports) { report ->
            GlassSurface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = report.siteName, color = Color(0xFF9AF04D), fontWeight = FontWeight.Bold)
                    Text(text = report.description, color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }
    }
}
