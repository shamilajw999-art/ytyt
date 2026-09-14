package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassSurface
import com.example.ui.components.SubscriptionEmblem
import com.example.ui.components.EmblemSize
import com.example.viewmodel.MythicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MythicViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val profile by viewModel.profile.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()

    // Preferences & Settings State
    var notificationsEnabled by remember { mutableStateOf(true) }
    var locationTrackingEnabled by remember { mutableStateOf(true) }
    var soundEffectsEnabled by remember { mutableStateOf(true) }
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var showLanguageMenu by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf("14.2 MB") }

    // Theme values
    val currentTheme = MaterialTheme.colorScheme
    val textPrimary = currentTheme.onBackground
    val textSecondary = currentTheme.onSurfaceVariant
    val accentColor = currentTheme.primary
    val isDark = currentTheme.background != Color.White

    val languages = listOf("English", "සිංහල (Sinhala)", "தமிழ் (Tamil)")

    // Localization Helper
    fun translate(text: String): String {
        val isSinhala = selectedLanguage == "සිංහල (Sinhala)"
        val isTamil = selectedLanguage == "தமிழ் (Tamil)"
        return when (text) {
            "Settings & Archive" -> if (isSinhala) "සැකසුම් සහ සංරක්ෂිතය" else if (isTamil) "அமைப்புகள் மற்றும் காப்பகம்" else "Settings & Archive"
            "General Settings" -> if (isSinhala) "පොදු සැකසුම්" else if (isTamil) "பொது அமைப்புகள்" else "General Settings"
            "Push Notifications" -> if (isSinhala) "පුෂ් දැනුම්දීම්" else if (isTamil) "புஷ் அறிவிப்புகள்" else "Push Notifications"
            "Alerts for quests, new scans & community updates" -> if (isSinhala) "ක්වෙස්ට්ස්, නව පරිලෝකන සහ ප්‍රජා යාවත්කාලීන කිරීම් සඳහා ඇඟවීම්" else if (isTamil) "தேடல்கள், புதிய ஸ்கேன்கள் மற்றும் சமூக புதுப்பிப்புகளுக்கான விழிப்பூட்டல்கள்" else "Alerts for quests, new scans & community updates"
            "Location Services" -> if (isSinhala) "ස්ථාන සේවා" else if (isTamil) "இருப்பிட சேவைகள்" else "Location Services"
            "Used to detect nearby heritage sites & monuments" -> if (isSinhala) "අසල ඇති උරුම ස්ථාන සහ ස්මාරක සෙවීමට භාවිතා කරයි" else if (isTamil) "அருகிலுள்ள பாரம்பரிய தளங்கள் மற்றும் நினைவுச்சின்னங்களைக் கண்டறியப் பயன்படுகிறது" else "Used to detect nearby heritage sites & monuments"
            "Sound Effects & Haptics" -> if (isSinhala) "ශබ්ද ප්‍රයෝග සහ ස්පර්ශ ප්‍රතිචාර" else if (isTamil) "ஒலி விளைவுகள் மற்றும் ஹாப்டிக்ஸ்" else "Sound Effects & Haptics"
            "Immersive feedback when unlocking achievements" -> if (isSinhala) "ජයග්‍රහණ අගුළු හරින විට ලැබෙන සජීවී ප්‍රතිචාර" else if (isTamil) "சாதனைகளைத் திறக்கும்போது அதிவேகக் கருத்து" else "Immersive feedback when unlocking achievements"
            "Language" -> if (isSinhala) "භාෂාව" else if (isTamil) "மொழி" else "Language"
            "App language localization" -> if (isSinhala) "යෙදුමේ භාෂාව වෙනස් කිරීම" else if (isTamil) "பயன்பாட்டு மொழி உள்ளூர்மயமாக்கல்" else "App language localization"
            "App Local Cache" -> if (isSinhala) "යෙදුම් හැඹිලිය" else if (isTamil) "உள்ளூர் கேச்" else "App Local Cache"
            "App Updates Timeline" -> if (isSinhala) "යෙදුම් යාවත්කාලීන කාලරාමුව" else if (isTamil) "பயன்பாட்டு புதுப்பிப்புகள் காலவரிசை" else "App Updates Timeline"
            "Clear" -> if (isSinhala) "මකන්න" else if (isTamil) "அழி" else "Clear"
            "Language switched to " -> if (isSinhala) "භාෂාව වෙනස් කරන ලදි: " else if (isTamil) "மொழி மாற்றப்பட்டது: " else "Language switched to "
            else -> text
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(textPrimary.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }
            Text(
                text = translate("Settings & Archive"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- ACCOUNT & SUBSCRIPTION STATUS ---
        profile?.let { p ->
            val userPlan = if (entitlements?.status?.lowercase() == "active") (entitlements?.plan ?: "free") else "free"
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.fullName.take(1).uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = p.fullName,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            SubscriptionEmblem(tier = userPlan, size = EmblemSize.MEDIUM)
                        }
                        Text(
                            text = "@${p.username} • ${p.email}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- SECTION 1: GENERAL SETTINGS ---
        Text(
            text = translate("General Settings"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Notifications Switch
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    title = translate("Push Notifications"),
                    subtitle = translate("Alerts for quests, new scans & community updates"),
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    tint = textPrimary,
                    subtitleColor = textSecondary,
                    accentColor = accentColor
                )

                Divider(color = textSecondary.copy(alpha = 0.15f))

                // Location Access Switch
                SettingToggleRow(
                    icon = Icons.Default.LocationOn,
                    title = translate("Location Services"),
                    subtitle = translate("Used to detect nearby heritage sites & monuments"),
                    checked = locationTrackingEnabled,
                    onCheckedChange = { locationTrackingEnabled = it },
                    tint = textPrimary,
                    subtitleColor = textSecondary,
                    accentColor = accentColor
                )

                Divider(color = textSecondary.copy(alpha = 0.15f))

                // Sound Effects Switch
                SettingToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = translate("Sound Effects & Haptics"),
                    subtitle = translate("Immersive feedback when unlocking achievements"),
                    checked = soundEffectsEnabled,
                    onCheckedChange = { soundEffectsEnabled = it },
                    tint = textPrimary,
                    subtitleColor = textSecondary,
                    accentColor = accentColor
                )

                Divider(color = textSecondary.copy(alpha = 0.15f))

                // Language Selector Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = textPrimary)
                        Column {
                            Text(translate("Language"), color = textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(translate("App language localization"), color = textSecondary, fontSize = 11.sp)
                        }
                    }
                    Box {
                        Button(
                             onClick = { showLanguageMenu = true },
                             colors = ButtonDefaults.buttonColors(
                                 containerColor = textPrimary.copy(alpha = 0.08f),
                                 contentColor = textPrimary
                             ),
                             contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                             shape = RoundedCornerShape(8.dp)
                        ) {
                             Text(selectedLanguage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                             Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                             expanded = showLanguageMenu,
                             onDismissRequest = { showLanguageMenu = false },
                             modifier = Modifier.background(currentTheme.surface)
                        ) {
                             languages.forEach { lang ->
                                 DropdownMenuItem(
                                     text = { Text(lang, color = textPrimary) },
                                     onClick = {
                                         viewModel.changeLanguage(lang)
                                         showLanguageMenu = false
                                         Toast.makeText(context, translate("Language switched to ") + lang, Toast.LENGTH_SHORT).show()
                                     }
                                 )
                             }
                        }
                    }
                }

                Divider(color = textSecondary.copy(alpha = 0.15f))

                // Clear Cache Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = textPrimary)
                        Column {
                            Text(translate("App Local Cache"), color = textPrimary, fontWeight = FontWeight.SemiBold)
                            val cachedDesc = if (selectedLanguage == "සිංහල (Sinhala)") "ගබඩා කර ඇති පින්තූර සහ නොබැඳි සිතියම් අංග: $cacheSize" else if (selectedLanguage == "தமிழ் (Tamil)") "சேமிக்கப்பட்ட படங்கள் & வரைபட கூறுகள்: $cacheSize" else "Stored images & offline map elements: $cacheSize"
                            Text(cachedDesc, color = textSecondary, fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = {
                            if (cacheSize != "0.0 MB") {
                                cacheSize = "0.0 MB"
                                val successMsg = if (selectedLanguage == "සිංහල (Sinhala)") "යෙදුම් හැඹිලිය සාර්ථකව මකන ලදී" else if (selectedLanguage == "தமிழ் (Tamil)") "பயன்பாட்டு கேச் வெற்றிகரமாக அழிக்கப்பட்டது" else "App cache successfully cleared"
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                val emptyMsg = if (selectedLanguage == "සිංහල (Sinhala)") "හැඹිලිය දැනටමත් හිස්ය" else if (selectedLanguage == "தமிழ் (Tamil)") "கேச் ஏற்கனவே காலியாக உள்ளது" else "Cache is already empty"
                                Toast.makeText(context, emptyMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cacheSize != "0.0 MB") accentColor.copy(alpha = 0.15f) else textPrimary.copy(alpha = 0.05f),
                            contentColor = if (cacheSize != "0.0 MB") accentColor else textSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(translate("Clear"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: SUPPORT & CONTACT ---
        Text(
            text = "Support & Contact Us",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "For payment verification, subscription inquiries, or app technical support, reach out directly to our Mythic Heritage Support Team.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Divider(color = textSecondary.copy(alpha = 0.15f))

                // Phone Support Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textPrimary.copy(alpha = 0.05f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0760577518"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Phone", "0760577518"))
                                Toast.makeText(context, "Support Phone copied: 0760577518", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86FC5C).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color(0xFF86FC5C), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Support Phone Line", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("0760577518", color = Color(0xFF86FC5C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Text("Call / Tap", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Email Support Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textPrimary.copy(alpha = 0.05f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:coderiders.mythic@gmail.com"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Email", "coderiders.mythic@gmail.com"))
                                Toast.makeText(context, "Support Email copied: coderiders.mythic@gmail.com", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Support Email", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("coderiders.mythic@gmail.com", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Text("Email / Tap", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3: TIMELINE ARCHIVE ---
        Text(
            text = translate("App Updates Timeline"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimelineItem(
                    version = "v2016.1 (Active)",
                    date = "September 2026",
                    title = "Production Release v2016.1",
                    description = "Full production release with automatic bank transfer reference code generation, instant Admin notification & approval flow, and Mythic Support Center.",
                    isCurrent = true,
                    accentColor = accentColor,
                    textColor = textPrimary,
                    subColor = textSecondary
                )
                TimelineItem(
                    version = "v1.2.0",
                    date = "June 2026",
                    title = "Visual Polish & Theme Adaptability",
                    description = "Added continuous streaming video Reels with GTV integration, dynamic light/dark gradient themes, clickable social hub, and settings cache configurations.",
                    isCurrent = false,
                    accentColor = accentColor,
                    textColor = textPrimary,
                    subColor = textSecondary
                )
                TimelineItem(
                    version = "v1.1.0",
                    date = "May 2026",
                    title = "Gemini AI vision & Mythic Chat",
                    description = "Integrated Google Gemini Pro Vision API to dynamically identify monuments from photographs and seeded the Mythic companion chatbot for interactive historical dialogues.",
                    isCurrent = false,
                    accentColor = accentColor,
                    textColor = textPrimary,
                    subColor = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3: CONNECT WITH US (SOCIAL LINKS) ---
        Text(
            text = "Connect With Us",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialButton(
                name = "Instagram",
                emoji = "📸",
                url = "https://www.instagram.com/mythic.app",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
            SocialButton(
                name = "Twitter / X",
                emoji = "🐦",
                url = "https://x.com/mythic_app1",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialButton(
                name = "Discord",
                emoji = "💬",
                url = "https://discord.gg/2dusvsUz5v",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
            SocialButton(
                name = "YouTube",
                emoji = "📺",
                url = "https://www.youtube.com/@mythic_app",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialButton(
                name = "TikTok",
                emoji = "🎵",
                url = "https://www.tiktok.com/@mythic.app",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
            SocialButton(
                name = "Reddit",
                emoji = "🤖",
                url = "https://www.reddit.com/user/Mythic_app",
                modifier = Modifier.weight(1f),
                uriHandler = uriHandler,
                accentColor = accentColor,
                textColor = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- SECTION 3.5: SUBSCRIPTIONS & PAYOUT CONFIGURATION GUIDE ---
        Text(
            text = "💳 Subscriptions & Payout Settings Guide",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💰", fontSize = 18.sp)
                    }
                    Column {
                        Text("Google Play & Stripe Payout Setup", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Instructions to configure banking & receive monthly revenue", color = textSecondary, fontSize = 12.sp)
                    }
                }

                Divider(color = textSecondary.copy(alpha = 0.15f))

                val setupSteps = listOf(
                    Triple("1. Link Merchant Account", "Go to Google Play Console ➔ Setup ➔ Merchant Account. Click 'Create Merchant Profile' and link your business or personal details.", "🏦"),
                    Triple("2. Add Bank Account & Routing", "Under Google Payments Center ➔ Subscriptions ➔ Payment Methods, add your IBAN / SWIFT / Account Number. Verify the small micro-deposit ($0.01 - $0.99).", "💳"),
                    Triple("3. Submit Tax Forms (W-8BEN / W-9)", "Complete the Tax Information questionnaire in Payments Center to ensure compliance and avoid 30% US withholding tax.", "📑"),
                    Triple("4. Configure Subscription SKUs", "Create products in Play Console: 'mythic_explorer_monthly' ($4.99/mo) and 'mythic_guild_annual' ($39.99/yr).", "🏷️"),
                    Triple("5. Payout Schedule & Thresholds", "Google Play automatically pays out earned subscription revenue on the 15th of every month for balances exceeding $100.", "🗓️"),
                    Triple("6. Enable Pub/Sub Webhooks", "Set up Google Cloud Pub/Sub Real-Time Developer Notifications (RTDN) to auto-grant subscriber perks on payment renewal.", "⚡")
                )

                setupSteps.forEach { (title, detail, emoji) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
                        Column {
                            Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(detail, color = textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- SECTION 4: APP CREDITS & THANKS ---
        Text(
            text = "App Credits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreditRow(role = "Leader", name = "Senuja", textColor = textPrimary, subColor = textSecondary, subscriptionTier = "pro")
                CreditRow(role = "Sub Leader", name = "Meshark", textColor = textPrimary, subColor = textSecondary, subscriptionTier = "pro")
                CreditRow(role = "Head Design", name = "Abilash", textColor = textPrimary, subColor = textSecondary, subscriptionTier = "max")
                CreditRow(role = "Head Coder", name = "Maleesha", textColor = textPrimary, subColor = textSecondary, subscriptionTier = "max")
                CreditRow(role = "Head Implementor", name = "Gavinda", textColor = textPrimary, subColor = textSecondary, subscriptionTier = "max")

                Divider(color = textSecondary.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                Text("Special Thanks", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp)
                Text("Sasan Vidunitha, Anuki, Senusha", color = textSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(4.dp))

                Text("Special Thanks to Teachers", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp)
                Text("Sachindra Teacher, Oshandi Teacher, Savindra Teacher", color = textSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(4.dp))

                Text("Our Principal", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp)
                Text("Dr. Anushke Perera", color = textSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color,
    subtitleColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column {
                Text(title, color = tint, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = subtitleColor, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f),
                uncheckedThumbColor = subtitleColor.copy(alpha = 0.8f),
                uncheckedTrackColor = subtitleColor.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun TimelineItem(
    version: String,
    date: String,
    title: String,
    description: String,
    isCurrent: Boolean,
    accentColor: Color,
    textColor: Color,
    subColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left timeline line & circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isCurrent) accentColor else subColor.copy(alpha = 0.5f))
                    .border(
                        width = if (isCurrent) 2.dp else 0.dp,
                        color = if (isCurrent) textColor else Color.Transparent,
                        shape = RoundedCornerShape(100.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(80.dp)
                    .background(subColor.copy(alpha = 0.15f))
            )
        }

        // Right Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = version,
                    color = if (isCurrent) accentColor else textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = date,
                    color = subColor,
                    fontSize = 11.sp
                )
            }
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = description,
                color = subColor,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun SocialButton(
    name: String,
    emoji: String,
    url: String,
    modifier: Modifier = Modifier,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    accentColor: Color,
    textColor: Color
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    // Ignore gracefully if browser not available
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = textColor.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = textColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                text = name,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CreditRow(
    role: String,
    name: String,
    textColor: Color,
    subColor: Color,
    subscriptionTier: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(role, color = subColor, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(name, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            if (subscriptionTier != null) {
                SubscriptionEmblem(tier = subscriptionTier, size = EmblemSize.SMALL)
            }
        }
    }
}
