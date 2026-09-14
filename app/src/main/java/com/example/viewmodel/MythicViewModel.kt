package com.example.viewmodel
import kotlinx.coroutines.Dispatchers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.remote.*
import com.example.data.repository.MythicRepository
import com.example.ui.theme.MythicThemeState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.SupabaseReport
import org.json.JSONObject
import java.util.UUID


data class DailyTrivia(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

sealed class DailyTriviaState {
    object Idle : DailyTriviaState()
    object Loading : DailyTriviaState()
    data class Success(val trivia: DailyTrivia) : DailyTriviaState()
    data class Error(val message: String) : DailyTriviaState()
}

class MythicViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MythicRepository(application)

    fun unlockBadge(code: String, name: String, description: String, tier: String, icon: String?) {
        viewModelScope.launch {
            repository.unlockBadge(code, name, description, tier, icon)
        }
    }

    fun addXp(amount: Int) {
        viewModelScope.launch {
            repository.addXp(amount)
        }
    }

    // --- State Streams from Repository ---
    val currentUser = repository.currentUser
    val entitlements = repository.entitlements
    val profile = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val allProfiles = repository.allProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val badges = repository.badges.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val quests = repository.quests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val scanHistory = repository.scanHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val savedSites = repository.savedSites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val mythicMessages = repository.mythicMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userPreferences = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // --- Realtime Feed Articles ---
    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    fun changeLanguage(language: String) {
        _selectedLanguage.value = language
    }

    private val _feedArticles = MutableStateFlow<List<HeritageArticle>>(emptyList())
    val feedArticles = _feedArticles.asStateFlow()

    // --- TikTok Reels State ---
    private val _reels = MutableStateFlow<List<HeritageReel>>(emptyList())
    val reels = _reels.asStateFlow()

    fun fetchReels() {
        viewModelScope.launch {
            try {
                val response = SupabaseClient.service.getReels()
                if (response.isSuccessful && response.body() != null) {
                    val supabaseReels = response.body()!!
                    val mappedReels = supabaseReels.map { r ->
                        HeritageReel(
                            id = r.id ?: UUID.randomUUID().toString(),
                            siteName = r.siteName,
                            province = r.province,
                            description = r.description,
                            videoUrl = r.videoUrl,
                            imageUrl = r.thumbnailUrl ?: "img_sigiriya",
                            category = r.category,
                            author = "@heritage_explorer", // Mock author for now
                            authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=${r.id}",
                            likes = r.likes,
                            isLiked = false,
                            comments = emptyList(),
                            shares = r.shares
                        )
                    }
                    _reels.value = mappedReels
                } else if (_reels.value.isEmpty()) {
                    initReels() // Fallback to mocks if DB empty
                }
            } catch (e: Exception) {
                if (_reels.value.isEmpty()) initReels()
            }
        }
    }

    private val _isGeneratingArticle = MutableStateFlow(false)
    val isGeneratingArticle = _isGeneratingArticle.asStateFlow()

    private val _isGeneratingReel = MutableStateFlow(false)
    val isGeneratingReel = _isGeneratingReel.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError = _generationError.asStateFlow()

    private val _reelGenerationError = MutableStateFlow<String?>(null)
    val reelGenerationError = _reelGenerationError.asStateFlow()

    // --- UI Controls ---
    private val _isMythicTyping = MutableStateFlow(false)
    val isMythicTyping = _isMythicTyping.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanHistoryEntity?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _showScanResultDialog = MutableStateFlow(false)
    val showScanResultDialog = _showScanResultDialog.asStateFlow()

    private val _activeFeedTab = MutableStateFlow("All")
    val activeFeedTab = _activeFeedTab.asStateFlow()

    private val _activeQuestTab = MutableStateFlow("Daily")
    val activeQuestTab = _activeQuestTab.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _verificationSentEmail = MutableStateFlow<String?>(null)
    val verificationSentEmail = _verificationSentEmail.asStateFlow()

    // --- Realtime Community State ---
    private val _communityPosts = MutableStateFlow<List<SupabaseCommunityPost>>(emptyList())
    val communityPosts = _communityPosts.asStateFlow()

    // --- Quiz State ---
    private val _currentQuiz = MutableStateFlow<HeritageQuiz?>(null)
    val currentQuiz = _currentQuiz.asStateFlow()

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz = _isGeneratingQuiz.asStateFlow()

    private val _quizError = MutableStateFlow<String?>(null)
    val quizError = _quizError.asStateFlow()

    fun generateQuiz(siteName: String) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _quizError.value = null
            _currentQuiz.value = null
            
            val quiz = repository.generateQuiz(siteName)
            if (quiz != null) {
                _currentQuiz.value = quiz
            } else {
                _quizError.value = "Failed to generate quiz for $siteName. Please try again."
            }
            _isGeneratingQuiz.value = false
        }
    }

    fun clearQuiz() {
        _currentQuiz.value = null
    }

    fun fetchHeritageArticles() {
        viewModelScope.launch {
            if (SupabaseClient.isConfigured) {
                try {
                    val remoteArticles = repository.getHeritageArticles()
                    if (remoteArticles.isNotEmpty()) {
                        val mapped = remoteArticles.map { r ->
                            HeritageArticle(
                                id = r.id,
                                siteName = r.siteName,
                                province = r.province,
                                description = r.description,
                                imageUrl = r.imageUrl,
                                category = r.category,
                                unescoStatus = r.unescoStatus,
                                era = r.era,
                                facts = r.facts,
                                initialLikes = r.likes
                            )
                        }
                        val existingIds = mapped.map { it.id }.toSet()
                        val uniquePreseeded = _feedArticles.value.filter { it.id !in existingIds }
                        _feedArticles.value = mapped + uniquePreseeded
                    }
                } catch (e: Exception) {
                    Log.e("MythicViewModel", "Error fetching heritage articles from Supabase", e)
                }
            }
        }
    }

    fun fetchCommunityPosts() {
        viewModelScope.launch {
            try {
                val posts = repository.getCommunityPosts()
                _communityPosts.value = posts
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Error fetching community posts from Supabase", e)
            }
        }
    }

    fun createCommunityPost(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.submitCommunityPost(content)
                fetchCommunityPosts()
                repository.addXp(15) // Boost level with community activity
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Error submitting community post", e)
            }
        }
    }


    private val _dailyTriviaState = MutableStateFlow<DailyTriviaState>(DailyTriviaState.Idle)
    val dailyTriviaState: StateFlow<DailyTriviaState> = _dailyTriviaState.asStateFlow()

    fun fetchDailyTrivia(lat: Double, lng: Double) {
        if (_dailyTriviaState.value is DailyTriviaState.Loading) return
        _dailyTriviaState.value = DailyTriviaState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            val isGeminiConfigured = GeminiClient.isConfigured
            
            if (isGeminiConfigured) {
                try {
                    val prompt = """
                        You are a Sri Lankan history expert. The user is currently at GPS coordinates ($lat, $lng).
                        Generate a fun, highly localized multiple-choice trivia question about a historical landmark or cultural fact near this location or in Sri Lanka generally if the location is far from known sites.
                        Return ONLY a JSON object with this exact structure, nothing else:
                        {
                            "question": "The question text?",
                            "options": ["Option A", "Option B", "Option C", "Option D"],
                            "correctIndex": 2,
                            "explanation": "Brief explanation of why it is correct."
                        }
                    """.trimIndent()

                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(GeminiPart(text = prompt))
                            )
                        )
                    )

                    val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    val responseText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (responseText != null) {
                        try {
                            val jsonStr = responseText.substringAfter("{").substringBeforeLast("}")
                            val cleanJson = "{$jsonStr}"
                            val jsonObject = org.json.JSONObject(cleanJson)
                            
                            val optionsArray = jsonObject.getJSONArray("options")
                            val optionsList = mutableListOf<String>()
                            for (i in 0 until optionsArray.length()) {
                                optionsList.add(optionsArray.getString(i))
                            }
                            
                            val trivia = DailyTrivia(
                                question = jsonObject.getString("question"),
                                options = optionsList,
                                correctIndex = jsonObject.getInt("correctIndex"),
                                explanation = jsonObject.getString("explanation")
                            )
                            _dailyTriviaState.value = DailyTriviaState.Success(trivia)
                        } catch (e: Exception) {
                            Log.e("MythicViewModel", "Failed to parse trivia JSON", e)
                            fallbackTrivia()
                        }
                    } else {
                        fallbackTrivia()
                    }
                } catch (e: Exception) {
                    Log.e("MythicViewModel", "Gemini API failed for trivia", e)
                    fallbackTrivia()
                }
            } else {
                fallbackTrivia()
            }
        }
    }

    private fun fallbackTrivia() {
        val defaultTrivia = DailyTrivia(
            question = "Which ancient king built the Sigiriya Rock Fortress?",
            options = listOf("King Dutugemunu", "King Kasyapa", "King Parakramabahu", "King Devanampiyatissa"),
            correctIndex = 1,
            explanation = "King Kasyapa (477 – 495 CE) built his palace on top of the Sigiriya rock for protection against his brother."
        )
        _dailyTriviaState.value = DailyTriviaState.Success(defaultTrivia)
    }
    
    fun claimTriviaReward(faction: String) {
        viewModelScope.launch {
            // Give 50 Faction XP and add to local user stats
            repository.addXp(50)
            // Unlock the quiz master badge
            repository.unlockBadge("quiz_master", "Heritage Quiz Master", "Score 100% (perfect score) on any heritage quiz", "gold", "🎓")
            // Log it in our local mock state if we had a full faction backend, for now just give XP
        }
    }

    init {
        // Preseed articles with local high-quality drawables for offline loading and dynamic URL fallbacks
        _feedArticles.value = listOf(
            HeritageArticle(
                id = "art_sigiriya",
                siteName = "Sigiriya Rock Fortress",
                province = "Central Province",
                description = "Sigiriya is an ancient rock fortress and palace built by King Kashyapa in the 5th century AD. Rising 200 meters above the forest, it features stunning frescoes, a mirror wall with historic graffiti, and the monumental Lion Gate.",
                imageUrl = "img_sigiriya",
                category = "Ancient Cities",
                unescoStatus = "UNESCO World Heritage Site",
                era = "5th Century AD",
                facts = "Mirror Wall; Lion's Paw gate; Water gardens",
                initialLikes = 125
            ),
            HeritageArticle(
                id = "art_kandy",
                siteName = "Temple of the Tooth",
                province = "Central Province",
                description = "The Sri Dalada Maligawa in Kandy is a golden-roofed Buddhist temple complex housing the sacred tooth relic of Gautama Buddha. Located in the royal palace complex of the former Kingdom of Kandy, it remains a supreme spiritual sanctuary.",
                imageUrl = "img_kandy",
                category = "Religious",
                unescoStatus = "UNESCO World Heritage Site",
                era = "16th Century AD",
                facts = "Houses the sacred tooth relic; Located in the historic Kandy Royal Palace; Features stunning Kandyan architecture",
                initialLikes = 142
            ),
            HeritageArticle(
                id = "art_galle",
                siteName = "Galle Fort Citadel",
                province = "Southern Province",
                description = "Galle Fort is a historic fortified old town built by the Portuguese in 1588 and extensively fortified by the Dutch in the 17th century. Blending European architectural styles and South Asian traditions, it stands on a beautiful coastal peninsula.",
                imageUrl = "img_galle",
                category = "Architecture",
                unescoStatus = "UNESCO World Heritage Site",
                era = "16th - 17th Century",
                facts = "Multi-cultural architectural style; Iconic Galle Lighthouse stands on the ramparts; Still is a living community",
                initialLikes = 98
            ),
            HeritageArticle(
                id = "art_anuradhapura",
                siteName = "Sacred Anuradhapura",
                province = "North Central Province",
                description = "Anuradhapura is one of the ancient capitals of Sri Lanka, famous for its exceptionally preserved ruins of an ancient Sinhalese civilization. It is home to massive ancient stupas like Ruwanwelisaya and the sacred Jaya Sri Maha Bodhi tree.",
                imageUrl = "img_anuradhapura",
                category = "Ancient Cities",
                unescoStatus = "UNESCO World Heritage Site",
                era = "3rd Century BC",
                facts = "Home to the sacred Jaya Sri Maha Bodhi tree; Massive dome of Ruwanwelisaya; Exceptionally advanced ancient water tanks",
                initialLikes = 110
            ),
            HeritageArticle(
                id = "art_dambulla",
                siteName = "Dambulla Cave Temple",
                province = "Central Province",
                description = "Dambulla is the largest and best-preserved cave temple complex in Sri Lanka. Five major caves house over 150 stunning statues of Buddha, Sri Lankan kings, and gods, beneath exquisite, vibrant rock ceiling paintings.",
                imageUrl = "https://images.unsplash.com/photo-1608958416744-8846c071d2b0?w=800",
                category = "Religious",
                unescoStatus = "UNESCO World Heritage Site",
                era = "1st Century BC",
                facts = "Largest and best-preserved cave temple; Features over 150 statues; Exceptional rock wall murals",
                initialLikes = 85
            ),
            HeritageArticle(
                id = "art_polonnaruwa",
                siteName = "Ancient Polonnaruwa",
                province = "North Central Province",
                description = "The second oldest kingdom of Sri Lanka, Polonnaruwa features the monumental ruins of the garden city built in the 12th century. Iconic monuments include the Gal Vihara rock temple, the Royal Palace, and the Quadrangle.",
                imageUrl = "https://images.unsplash.com/photo-1578593139888-39620e59c164?w=800",
                category = "Ancient Cities",
                unescoStatus = "UNESCO World Heritage Site",
                era = "12th Century AD",
                facts = "Iconic rock-cut Buddha statues of Gal Vihara; Unique circular relic house (Vatadage); Masterful ancient Parakrama Samudra reservoir",
                initialLikes = 92
            ),
            HeritageArticle(
                id = "art_jaffna",
                siteName = "Jaffna Fort",
                province = "Northern Province",
                description = "Built by the Portuguese in 1618 and later expanded by the Dutch, the Jaffna Fort stands as an impressive star-shaped limestone fortress overlooking the Jaffna lagoon. It represents a vital monument of the northern peninsula's rich history.",
                imageUrl = "https://images.unsplash.com/photo-1620121692029-d088224ddc74?w=800",
                category = "Architecture",
                unescoStatus = "Protected Cultural Monument",
                era = "17th Century",
                facts = "Built entirely using coral and limestone; Star-shaped Dutch fortress engineering; Overlooks the Jaffna Lagoon",
                initialLikes = 74
            )
        )

        initReels()
        fetchReels()
        fetchHeritageArticles()
        fetchWikipediaArticles()
        fetchCommunityPosts()

        viewModelScope.launch {
            repository.checkSession()
            // Observe preferences to apply theme
            repository.preferences.collect { prefs ->
                if (prefs != null) {
                    MythicThemeState.themeMode = if (prefs.darkMode) ThemeMode.DARK else ThemeMode.LIGHT
                }
            }
        }
        
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                if (user != null) {
                    val p = repository.getProfile()
                    if (p != null) {
                        repository.checkAndInitializeStreak(p)
                    }
                }
            }
        }
    }

    private fun fetchWikipediaArticles() {
        viewModelScope.launch {
            val heritageTopics = listOf(
                "Sigiriya",
                "Anuradhapura",
                "Polonnaruwa",
                "Galle_Fort",
                "Temple_of_the_Tooth",
                "Dambulla_cave_temple",
                "Nine_Arch_Bridge,_Demodara",
                "Yala_National_Park",
                "Adam's_Peak",
                "Kandy_Esala_Perahera"
            )
            val fetchedArticles = mutableListOf<HeritageArticle>()
            for (topic in heritageTopics) {
                try {
                    val summary = WikipediaClient.service.getSummary(topic)
                    var photoUrl = summary.thumbnail?.source ?: summary.originalimage?.source ?: ""
                    if (photoUrl.startsWith("//")) {
                        photoUrl = "https:$photoUrl"
                    }
                    if (photoUrl.isBlank()) {
                        photoUrl = when {
                            topic.contains("Sigiriya", ignoreCase = true) -> "img_sigiriya"
                            topic.contains("Tooth", ignoreCase = true) || topic.contains("Kandy", ignoreCase = true) -> "img_kandy"
                            topic.contains("Galle", ignoreCase = true) -> "img_galle"
                            topic.contains("Anuradhapura", ignoreCase = true) -> "img_anuradhapura"
                            else -> "https://images.unsplash.com/photo-1586861635167-e5223aadc9fe?w=800"
                        }
                    }
                    val extractText = summary.extract ?: "Historic Sri Lankan heritage landmark."
                    val category = when {
                        topic.contains("Fort", ignoreCase = true) || topic.contains("Bridge", ignoreCase = true) -> "Architecture"
                        topic.contains("Temple", ignoreCase = true) || topic.contains("Tooth", ignoreCase = true) -> "Religious"
                        topic.contains("Park", ignoreCase = true) || topic.contains("Peak", ignoreCase = true) -> "Nature"
                        topic.contains("Perahera", ignoreCase = true) -> "Culture"
                        else -> "Ancient Cities"
                    }
                    val wikiPageUrl = summary.contentUrls?.desktop?.page ?: "https://en.wikipedia.org/wiki/${summary.title.replace(" ", "_")}"
                    fetchedArticles.add(
                        HeritageArticle(
                            id = "wiki_${topic.lowercase()}",
                            siteName = summary.title,
                            province = summary.description ?: "Sri Lanka Heritage",
                            description = extractText,
                            imageUrl = photoUrl,
                            category = category,
                            unescoStatus = if (topic in listOf("Sigiriya", "Anuradhapura", "Polonnaruwa", "Galle_Fort", "Temple_of_the_Tooth", "Dambulla_cave_temple")) "UNESCO World Heritage Site" else "Cultural Heritage",
                            era = "Historical",
                            facts = "Source: Wikipedia ($wikiPageUrl)",
                            initialLikes = (120..450).random()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("MythicViewModel", "Failed to fetch Wikipedia article for $topic", e)
                }
            }
            if (fetchedArticles.isNotEmpty()) {
                val currentList = _feedArticles.value.toMutableList()
                val currentIds = currentList.map { it.id }.toSet()
                val newArticles = fetchedArticles.filter { it.id !in currentIds }
                _feedArticles.value = currentList + newArticles
            }
        }
    }

    fun searchWikipediaArticle(query: String, onResult: (Boolean) -> Unit = {}) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val trimmed = query.trim()
                var summary: com.example.data.remote.WikiSummary? = null
                var searchPageThumb: String? = null

                // 1. First attempt to use Wikipedia REST search API to resolve page key/title
                try {
                    val searchRes = WikipediaClient.service.searchPage(trimmed, limit = 5)
                    val firstPage = searchRes.pages?.firstOrNull()
                    if (firstPage != null) {
                        searchPageThumb = firstPage.thumbnail?.url
                        if (firstPage.key != null) {
                            summary = WikipediaClient.service.getSummary(firstPage.key)
                        } else if (firstPage.title != null) {
                            summary = WikipediaClient.service.getSummary(firstPage.title.replace(" ", "_"))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MythicViewModel", "Wikipedia searchPage query failed for $trimmed, trying direct title fallback", e)
                }

                // 2. Direct title fallback if search didn't return a summary
                if (summary == null) {
                    val formatted = trimmed.replace(" ", "_")
                    summary = WikipediaClient.service.getSummary(formatted)
                }

                var photoUrl = summary.thumbnail?.source ?: summary.originalimage?.source ?: searchPageThumb ?: ""
                if (photoUrl.startsWith("//")) {
                    photoUrl = "https:$photoUrl"
                }
                if (photoUrl.isBlank()) {
                    val queryLower = (trimmed + " " + (summary.title ?: "") + " " + (summary.description ?: "")).lowercase()
                    photoUrl = when {
                        queryLower.contains("sigiriya") || queryLower.contains("rock") -> "img_sigiriya"
                        queryLower.contains("kandy") || queryLower.contains("tooth") || queryLower.contains("temple") -> "img_kandy"
                        queryLower.contains("galle") || queryLower.contains("fort") -> "img_galle"
                        queryLower.contains("anuradhapura") || queryLower.contains("stupa") -> "img_anuradhapura"
                        queryLower.contains("tea") || queryLower.contains("bridge") || queryLower.contains("ella") -> "https://images.unsplash.com/photo-1586861635167-e5223aadc9fe?w=800"
                        queryLower.contains("park") || queryLower.contains("yala") || queryLower.contains("safari") -> "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800"
                        queryLower.contains("cave") || queryLower.contains("dambulla") -> "https://images.unsplash.com/photo-1608958416744-8846c071d2b0?w=800"
                        else -> "https://images.unsplash.com/photo-1586861635167-e5223aadc9fe?w=800"
                    }
                }

                val wikiPageUrl = summary.contentUrls?.desktop?.page ?: "https://en.wikipedia.org/wiki/${summary.title.replace(" ", "_")}"

                val newArticle = HeritageArticle(
                    id = "wiki_${System.currentTimeMillis()}",
                    siteName = summary.title,
                    province = summary.description ?: "Sri Lanka Heritage",
                    description = summary.extract ?: "Historical details and cultural background from Wikipedia.",
                    imageUrl = photoUrl,
                    category = "Wikipedia",
                    unescoStatus = "Wikipedia Source",
                    era = "Historical",
                    facts = "Source: Wikipedia ($wikiPageUrl)",
                    initialLikes = (100..500).random()
                )

                val currentList = _feedArticles.value.toMutableList()
                currentList.removeAll { it.siteName.equals(newArticle.siteName, ignoreCase = true) }
                _feedArticles.value = listOf(newArticle) + currentList
                onResult(true)
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Error fetching Wikipedia search for $query", e)
                onResult(false)
            }
        }
    }

    // --- AUTH ACTIONS ---
    fun signUp(username: String, email: String, password: String, avatarUrl: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _authError.value = null
            _verificationSentEmail.value = null
            val error = repository.signUp(username, email, password, avatarUrl = avatarUrl)
            _isLoggingIn.value = false
            if (error == "VERIFICATION_SENT" || error == "VERIFICATION_SENT_MOCK") {
                _verificationSentEmail.value = email
            } else if (error == null) {
                onSuccess()
            } else {
                _authError.value = error
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _authError.value = null
            val error = repository.login(email, password)
            _isLoggingIn.value = false
            if (error == null) {
                onSuccess()
            } else if (error == "EMAIL_NOT_CONFIRMED") {
                _authError.value = "Please confirm your email before logging in. We have sent a verification email to your address."
            } else {
                _authError.value = error
            }
        }
    }

    fun loginAsDemo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _authError.value = null
            // Demo account details
            val error = repository.login("demo@mythic.app", "mythicdemo123")
            _isLoggingIn.value = false
            if (error == null) {
                onSuccess()
            } else {
                // If demo account doesn't exist, try to sign it up first (simplified for demo)
                signUp("DemoUser", "demo@mythic.app", "mythicdemo123") {
                    login("demo@mythic.app", "mythicdemo123", onSuccess)
                }
            }
        }
    }

    fun randomizeAvatar() {
        viewModelScope.launch {
            val p = profile.value ?: return@launch
            val randomSeed = (1000..99999).random()
            val cartoonStyles = listOf("adventurer", "bottts", "lorelei", "fun-emoji", "avataaars")
            val selectedStyle = cartoonStyles.random()
            val newUrl = "https://api.dicebear.com/7.x/$selectedStyle/png?seed=avatar_$randomSeed"
            repository.updateAvatar(p.id, newUrl)
        }
    }

    fun recoverPassword(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _authError.value = null
            val success = repository.recoverPassword(email)
            _isLoggingIn.value = false
            if (success) {
                onSent()
            } else {
                _authError.value = "Failed to send recovery email. Please check your internet connection."
            }
        }
    }

    fun clearVerificationSent() {
        _verificationSentEmail.value = null
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onSuccess()
        }
    }


    // --- ONBOARDING & PREFERENCES ---
    fun updateCustomAvatar(avatarUrl: String) {
        viewModelScope.launch {
            val p = profile.value
            val userId = p?.id ?: currentUser.value?.id ?: "guest"
            repository.updateAvatar(userId, avatarUrl)
        }
    }

    fun saveOnboardingPreferences(
        mythicColorHex: String,
        mythicMood: String,
        customAvatarUrl: String?
    ) {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: profile.value?.id ?: "guest"
            val currentPrefs = userPreferences.value
            val updatedPrefs = UserPreferencesEntity(
                userId = userId,
                darkMode = currentPrefs?.darkMode ?: true,
                notificationsEnabled = currentPrefs?.notificationsEnabled ?: true,
                language = currentPrefs?.language ?: "English",
                mythicColorHex = mythicColorHex,
                mythicMood = mythicMood,
                hasCompletedOnboarding = true
            )
            repository.insertUserPreferences(updatedPrefs)

            if (!customAvatarUrl.isNullOrBlank()) {
                repository.updateAvatar(userId, customAvatarUrl)
            }

            // Award onboarding bonus XP (+100 XP) and Pioneer Explorer badge
            val p = profile.value
            if (p != null) {
                repository.addXp(100)
                repository.unlockBadge("pioneer", "Pioneer Explorer", "Completed Mythic onboarding setup", "silver", "🛡️")
            }
        }
    }

    // --- THEME SWITCHER ---
    fun toggleTheme(darkMode: Boolean) {
        viewModelScope.launch {
            val mode = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT
            MythicThemeState.themeMode = mode
            val userId = currentUser.value?.id ?: "guest"
            repository.insertUserPreferences(
                UserPreferencesEntity(userId, darkMode, true, "en")
            )
        }
    }

    // --- SAVE / UNSAVE ---
    fun toggleSaveSite(siteName: String, province: String, imageUrl: String?) {
        viewModelScope.launch {
            repository.toggleSaveSite(siteName, province, imageUrl)
        }
    }

    // --- SUBSCRIPTION & PAYMENTS ---
    fun requestBankTransferSubscription(tier: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.createPaymentOrderRpc(tier.lowercase())
                if (result is com.example.data.repository.PaymentOrderResult.Success) {
                    val order = result.order
                    val userEmail = currentUser.value?.email ?: profile.value?.email ?: ""
                    if (userEmail.isNotEmpty()) {
                        val emailRequest = mapOf(
                            "email" to userEmail,
                            "tier" to tier,
                            "amount" to String.format("%.2f", order.amount),
                            "transaction_id" to order.paymentReference
                        )
                        SupabaseClient.service.sendPaymentEmail(emailRequest)
                    }
                    onSuccess()
                } else {
                    Log.e("MythicViewModel", "Failed to create payment order via RPC")
                }
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to request bank transfer payment order", e)
            }
        }
    }

    fun updateProfilePicture(url: String) {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: profile.value?.id ?: return@launch
            try {
                // Update local repository first
                repository.updateAvatar(userId, url)
                
                if (SupabaseClient.isConfigured) {
                    SupabaseClient.service.updateProfile(
                        query = "eq.$userId",
                        profile = mapOf("avatar_url" to url)
                    )
                }
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to update profile picture in Supabase", e)
            }
        }
    }

    fun uploadProfilePictureOnly(uri: android.net.Uri, context: android.content.Context, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val userId = UUID.randomUUID().toString() // Temporary ID for file path
                    val fileName = "temp_avatar_${userId}_${System.currentTimeMillis()}.png"
                    val url = repository.uploadFile("profiles", fileName, bytes, "image/png")
                    onComplete(url)
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to upload profile picture only", e)
                onComplete(null)
            }
        }
    }

    fun uploadAndSyncProfilePicture(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val userId = currentUser.value?.id ?: UUID.randomUUID().toString()
                    val fileName = "avatar_${userId}_${System.currentTimeMillis()}.png"
                    val url = repository.uploadFile("profiles", fileName, bytes, "image/png")
                    if (url != null) {
                        updateProfilePicture(url)
                    }
                }
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to upload and sync profile picture", e)
            }
        }
    }

    // --- TRIGGER SCAN ---
    private val _isScanningWithVision = MutableStateFlow(false)
    val isScanningWithVision = _isScanningWithVision.asStateFlow()

    private val _visionScanError = MutableStateFlow<String?>(null)
    val visionScanError = _visionScanError.asStateFlow()

    fun scanWithGeminiVision(base64Image: String, mimeType: String, defaultSiteName: String, fallbackImageUrl: String) {
        viewModelScope.launch {
            _isScanningWithVision.value = true
            _visionScanError.value = null
            
            val isGeminiConfigured = GeminiClient.isConfigured
            if (isGeminiConfigured) {
                try {
                    val prompt = """
                        Analyze this image. It is a photograph of a historical, ancient, or cultural heritage monument or site in Sri Lanka. 
                        Identify the site name, which province of Sri Lanka it is in, a brief 2-3 sentence historical profile description, and its UNESCO status (either 'UNESCO World Heritage Site' or 'Protected Cultural Monument'), its historical era (e.g. 5th Century AD, 12th Century AD, etc.), and 3 interesting facts.
                        
                        You MUST respond with a single raw JSON object in this exact format:
                        {
                          "siteName": "Name of the site",
                          "province": "Province name",
                          "description": "Historical description",
                          "unescoStatus": "UNESCO World Heritage Site or Protected Cultural Monument",
                          "era": "Era name",
                          "facts": "Fact 1; Fact 2; Fact 3"
                        }
                        
                        Do NOT wrap inside markdown code blocks, output only the raw JSON.
                    """.trimIndent()
                    
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(
                                    GeminiPart(text = prompt),
                                    GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Image))
                                )
                            )
                        )
                    )
                    
                    val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    if (response.isSuccessful && response.body() != null) {
                        val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            val jsonStart = text.indexOf("{")
                            val jsonEnd = text.lastIndexOf("}")
                            if (jsonStart != -1 && jsonEnd != -1) {
                                val jsonStr = text.substring(jsonStart, jsonEnd + 1)
                                val json = JSONObject(jsonStr)
                                val siteName = json.optString("siteName", defaultSiteName)
                                val province = json.optString("province", "Central Province")
                                val desc = json.optString("description", "A historical marvel")
                                val unesco = json.optString("unescoStatus", "Protected Cultural Monument")
                                val era = json.optString("era", "Ancient Era")
                                val facts = json.optString("facts", "Rich historical heritage")
                                
                                val scan = repository.saveCustomScan(
                                    siteName = siteName,
                                    province = province,
                                    description = desc,
                                    unescoStatus = unesco,
                                    era = era,
                                    facts = facts,
                                    imageUrl = fallbackImageUrl
                                )
                                _scanResult.value = scan
                                _showScanResultDialog.value = true
                                _isScanningWithVision.value = false
                                return@launch
                            }
                        }
                    }
                    _visionScanError.value = "Unable to analyze image. Please try again with a clearer picture."
                } catch (e: Exception) {
                    _visionScanError.value = "Error during analysis: ${e.message}"
                }
            } else {
                _visionScanError.value = "Gemini API is not configured. Please add your GEMINI_API_KEY to the Secrets panel."
            }
            
            // Fallback to offline scan simulation on error or if Gemini is not configured
            delay(2000)
            triggerScan(defaultSiteName, fallbackImageUrl)
            _isScanningWithVision.value = false
        }
    }

    fun triggerScan(siteName: String, imageUrl: String?) {
        viewModelScope.launch {
            val result = repository.scanHeritage(siteName, imageUrl)
            _scanResult.value = result
            _showScanResultDialog.value = true
        }
    }

    fun dismissScanResult() {
        _showScanResultDialog.value = false
        _scanResult.value = null
    }

    // --- DAILY USAGE CAP & LIMIT TRACKING ---
    private val _dailyUsageCount = MutableStateFlow(0)
    val dailyUsageCount = _dailyUsageCount.asStateFlow()

    private val _showUsageCapDialog = MutableStateFlow(false)
    val showUsageCapDialog = _showUsageCapDialog.asStateFlow()

    private var lastUsageDateString = ""

    fun checkAndIncrementDailyUsage(onAllowed: () -> Unit, onCapReached: () -> Unit) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (lastUsageDateString != today) {
            lastUsageDateString = today
            _dailyUsageCount.value = 0
        }

        viewModelScope.launch {
            val currentEntitlements = repository.entitlements.value
            val isProOrMax = currentEntitlements?.entitlements?.unlimitedLumo == true

            if (isProOrMax) {
                onAllowed()
            } else {
                if (_dailyUsageCount.value >= 5) {
                    _showUsageCapDialog.value = true
                    onCapReached()
                } else {
                    _dailyUsageCount.value += 1
                    onAllowed()
                }
            }
        }
    }

    fun dismissUsageCapDialog() {
        _showUsageCapDialog.value = false
    }

    // --- MYTHIC MESSAGING ---
    fun sendMythicMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isMythicTyping.value = true
            repository.sendMythicMessage(content)
            _isMythicTyping.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // --- SETTERS ---
    fun setFeedTab(tab: String) {
        _activeFeedTab.value = tab
    }

    fun setQuestTab(tab: String) {
        _activeQuestTab.value = tab
    }

    fun triggerArticleRead() {
        viewModelScope.launch {
            repository.incrementQuestProgress("read")
        }
    }

    fun triggerArticleLike(articleId: String, newLikeCount: Int) {
        viewModelScope.launch {
            repository.incrementQuestProgress("like")
            // Update local memory flow
            _feedArticles.value = _feedArticles.value.map { art ->
                if (art.id == articleId) art.copy(initialLikes = newLikeCount) else art
            }
            try {
                repository.updateArticleLikes(articleId, newLikeCount)
                val userId = currentUser.value?.id ?: "guest"
                repository.saveArticleReaction(
                    SupabaseArticleReaction(
                        articleId = articleId,
                        userId = userId,
                        reactionType = "like",
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to sync reaction to Supabase", e)
            }
        }
    }

    private val _recentReports = MutableStateFlow<List<SupabaseReport>>(emptyList())
    val recentReports: StateFlow<List<SupabaseReport>> = _recentReports

    // --- REPORTING ---
    fun submitReport(report: SupabaseReport) {
        viewModelScope.launch {
            repository.submitReport(report)
            fetchRecentReports()
        }
    }

    fun fetchRecentReports() {
        viewModelScope.launch {
            _recentReports.value = repository.getRecentReports()
        }
    }

    // --- REAL-TIME AI ARTICLE GENERATION ---
    fun generateRealtimeArticle(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isGeneratingArticle.value = true
            _generationError.value = null
            
            // Fallback to Gemini
            val isGeminiConfigured = GeminiClient.isConfigured
            var newArticle: HeritageArticle? = null
            
            if (isGeminiConfigured) {
                try {
                    val prompt = """
                        Generate a detailed, authentic historical profile for a Sri Lankan ancient heritage site, monument, temple, fort, or historical topic matching search term: "$query".
                        Your response MUST be a single raw JSON object with the exact following fields:
                        {
                          "siteName": "Name of the site",
                          "province": "Which Sri Lankan province (e.g., Central, Southern, North Central, Northern, Western, Sabaragamuwa, Uva, North Western, Eastern)",
                          "description": "A rich 3-4 sentence historical and archaeological description",
                          "category": "One of: 'Architecture', 'Ancient Cities', 'Religious'",
                          "unescoStatus": "UNESCO World Heritage Site or Protected Cultural Monument",
                          "era": "The historical era or century (e.g., 3rd Century BC, 5th Century AD, 12th Century AD, etc.)",
                          "facts": "Three highly interesting, verified historical facts about the site, separated strictly by semicolons (;) without numbering"
                        }
                        Important: Do NOT wrap the JSON inside markdown code blocks (e.g., ```json ... ```) or any other formatting. Output ONLY the raw JSON string.
                    """.trimIndent()
                    
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = prompt))
                            )
                        )
                    )
                    
                    val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    val responseBody = response.body()
                    val responseText = responseBody?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (!responseText.isNullOrBlank()) {
                        var cleanJson = responseText.trim()
                        if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        }
                        
                        val json = JSONObject(cleanJson)
                        val siteName = json.getString("siteName")
                        val province = json.getString("province")
                        val description = json.getString("description")
                        val category = json.getString("category")
                        val unescoStatus = json.optString("unescoStatus", "Protected Cultural Monument")
                        val era = json.optString("era", "Ancient Era")
                        val facts = json.optString("facts", "Rich historical significance; Ancient Sri Lankan design; Cultural landmark")
                        
                        val artId = "art_dynamic_" + System.currentTimeMillis()
                        val imageUrl = "https://images.unsplash.com/featured/?srilanka,heritage,${siteName.replace(" ", "")}"
                        
                        newArticle = HeritageArticle(
                            id = artId,
                            siteName = siteName,
                            province = province,
                            description = description,
                            imageUrl = imageUrl,
                            category = if (category in listOf("Architecture", "Ancient Cities", "Religious")) category else "Ancient Cities",
                            unescoStatus = unescoStatus,
                            era = era,
                            facts = facts,
                            initialLikes = (30..150).random()
                        )
                    }
                } catch (e: Exception) {
                    // Fallback to local offline template on error
                }
            }
            
            if (newArticle == null) {
                newArticle = getOfflineGeneratedArticle(query)
            }
            
            // Prepend new article to the top of the feedArticles list!
            _feedArticles.value = listOf(newArticle!!) + _feedArticles.value
            _isGeneratingArticle.value = false
            
            // Save to Supabase
            try {
                repository.saveHeritageArticle(
                    SupabaseArticle(
                        id = newArticle!!.id,
                        siteName = newArticle!!.siteName,
                        province = newArticle!!.province,
                        description = newArticle!!.description,
                        imageUrl = newArticle!!.imageUrl,
                        category = newArticle!!.category,
                        unescoStatus = newArticle!!.unescoStatus,
                        era = newArticle!!.era,
                        facts = newArticle!!.facts,
                        likes = newArticle!!.initialLikes
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to save generated article to Supabase", e)
            }
            
            // Increment quest progress for generation/reading!
            repository.incrementQuestProgress("read")
        }
    }

    private fun getOfflineGeneratedArticle(query: String): HeritageArticle {
        val q = query.lowercase().trim()
        return when {
            q.contains("mihintale") -> HeritageArticle(
                id = "art_mihintale",
                siteName = "Mihintale Sanctuary",
                province = "North Central Province",
                description = "Mihintale is a mountain peak near Anuradhapura, believed to be the site of a historic meeting between the Buddhist monk Mahinda and King Devanampiyatissa, which inaugurated the presence of Buddhism in Sri Lanka. It is a revered pilgrimage site with ancient stupas and rock carvings.",
                imageUrl = "https://images.unsplash.com/featured/?srilanka,mihintale",
                category = "Religious",
                unescoStatus = "Protected Cultural Sanctuary",
                era = "3rd Century BC",
                facts = "Considered the cradle of Buddhism in Sri Lanka; Houses the Aradhana Gala rock; Home to one of the world's oldest medical baths",
                initialLikes = 87
            )
            q.contains("pidurangala") -> HeritageArticle(
                id = "art_pidurangala",
                siteName = "Pidurangala Rock",
                province = "Central Province",
                description = "Pidurangala is a massive rock formation situated a few kilometers north of Sigiriya. It was home to a Buddhist monastery dating back to the 5th century AD, which was heavily supported by King Kashyapa during his reign. It offers a panoramic 360-degree view of Sigiriya Rock.",
                imageUrl = "https://images.unsplash.com/featured/?srilanka,pidurangala",
                category = "Architecture",
                unescoStatus = "Protected Monument",
                era = "5th Century AD",
                facts = "Used as an ancient Buddhist monastery; Houses a massive reclining Buddha statue carved under a cave; Provides the best vantage point for Sigiriya",
                initialLikes = 104
            )
            q.contains("ritigala") -> HeritageArticle(
                id = "art_ritigala",
                siteName = "Ritigala Forest Monastery",
                province = "North Central Province",
                description = "Ritigala is an ancient Buddhist monastery and mountain range in northern Sri Lanka. The ruins are nestled deep in a strictly protected natural reserve and showcase exceptional stone masonry, ancient paths, and monastic bathing pools with no religious carvings, indicating a unique aesthetic.",
                imageUrl = "https://images.unsplash.com/featured/?srilanka,ritigala",
                category = "Ancient Cities",
                unescoStatus = "Strict Nature Reserve & Protected Monument",
                era = "1st Century BC",
                facts = "Exceptional double-platform stone bridges; Features unique medicinal herb forests; Mentioned in the epic Ramayana legend",
                initialLikes = 93
            )
            q.contains("yapahuwa") -> HeritageArticle(
                id = "art_yapahuwa",
                siteName = "Yapahuwa Rock Fortress",
                province = "North West Province",
                description = "Yapahuwa was one of the ephemeral medieval capitals of Sri Lanka in the late 13th century. Built on a massive 90-meter granite rock, it served as a palace-fortress housing the Sacred Tooth Relic. It is famous for its stunning, steep ornamental stone stairway with intricate carvings.",
                imageUrl = "https://images.unsplash.com/featured/?srilanka,yapahuwa",
                category = "Architecture",
                unescoStatus = "Protected Cultural Citadel",
                era = "13th Century AD",
                facts = "Home to the iconic Chinese-style Yapahuwa Lion carvings; Protected the Tooth Relic during a South Indian invasion; Boasts a steep rock-cut ornamental staircase",
                initialLikes = 79
            )
            else -> {
                val capitalized = query.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                HeritageArticle(
                    id = "art_dynamic_gen_" + System.currentTimeMillis(),
                    siteName = capitalized,
                    province = "Sri Lanka",
                    description = "A magnificent historical site in Sri Lanka filled with centuries of deep cultural heritage, ancient craftsmanship, and sacred archaeological discoveries. It stands as a testament to the island's glorious engineering and spiritual longevity.",
                    imageUrl = "https://images.unsplash.com/featured/?srilanka,heritage,history",
                    category = "Ancient Cities",
                    unescoStatus = "Protected Cultural Monument",
                    era = "Ancient Era",
                    facts = "Rich archaeological discoveries; Showcases exceptional historic architectural engineering; Embedded deep in the island's local folklore",
                    initialLikes = (25..95).random()
                )
            }
        }
    }

    fun initReels() {
        if (_reels.value.isNotEmpty()) return
        _reels.value = listOf(
            HeritageReel(
                id = "reel_sigiriya",
                siteName = "Sigiriya Fortress",
                province = "Central Province",
                description = "Ascending the colossal 5th-century ancient fortress in Sri Lanka. Absolutely mesmerizing views from the summit! 🦁👑 #sigiriya #srilanka #heritage",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                imageUrl = "img_sigiriya",
                category = "Ancient Cities",
                author = "@heritage_guardian",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=guardian",
                likes = 342,
                isLiked = false,
                comments = listOf(
                    ReelComment("c1", "@lanka_trekker", "https://api.dicebear.com/7.x/adventurer/png?seed=trekker", "This is absolutely breathtaking! Sigiriya is indeed the 8th wonder of the world.", "2h ago"),
                    ReelComment("c2", "@ruins_lover", "https://api.dicebear.com/7.x/adventurer/png?seed=ruins", "I climbed it last year, the Lion's paws are giant!", "1h ago")
                ),
                shares = 88
            ),
            HeritageReel(
                id = "reel_ella",
                siteName = "Nine Arch Bridge",
                province = "Uva Province",
                description = "Watching the historic colonial train pass over the magnificent Demodara Nine Arch Bridge in the lush green hills of Ella. 🚂🌲 #ella #train #scenic",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                imageUrl = "img_kandy",
                category = "Architecture",
                author = "@travel_lanka",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=traveller",
                likes = 521,
                isLiked = false,
                comments = listOf(
                    ReelComment("c3", "@rail_fan", "https://api.dicebear.com/7.x/adventurer/png?seed=rail", "The blue train of Sri Lanka is so iconic. Love Ella!", "3h ago")
                ),
                shares = 154
            ),
            HeritageReel(
                id = "reel_galle",
                siteName = "Galle Dutch Fort",
                province = "Southern Province",
                description = "Golden hour sunset over the historic Galle Dutch Fort ramparts and beautiful coastal reef waves. 🌅🏰 #gallefort #history #coastline",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                imageUrl = "img_galle",
                category = "Ancient Cities",
                author = "@fort_explorer",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=fort",
                likes = 289,
                isLiked = false,
                comments = listOf(
                    ReelComment("c4", "@ocean_breeze", "https://api.dicebear.com/7.x/adventurer/png?seed=breeze", "Walks on the Galle Fort walls are so magical in evenings.", "30m ago")
                ),
                shares = 42
            ),
            HeritageReel(
                id = "reel_waterfall",
                siteName = "Laxapana Waterfalls",
                province = "Central Province",
                description = "Uncovering the breathtaking, pristine waterfalls hidden deep in the lush tropical forests of Sri Lanka. 🌿💦 #waterfalls #nature #paradise",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                imageUrl = "img_anuradhapura",
                category = "Nature",
                author = "@wild_lanka",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=wild",
                likes = 412,
                isLiked = false,
                comments = listOf(
                    ReelComment("c5", "@nature_girl", "https://api.dicebear.com/7.x/adventurer/png?seed=girl", "Sri Lanka is so unbelievably green and rich in nature!", "5m ago")
                ),
                shares = 91
            )
        )
    }

    fun toggleLikeReel(reelId: String) {
        _reels.value = _reels.value.map { reel ->
            if (reel.id == reelId) {
                val newLiked = !reel.isLiked
                reel.copy(
                    isLiked = newLiked,
                    likes = if (newLiked) reel.likes + 1 else reel.likes - 1
                )
            } else reel
        }
    }

    fun addCommentToReel(reelId: String, text: String) {
        if (text.isBlank()) return
        val userProfile = profile.value
        val username = userProfile?.username ?: "explorer"
        val avatar = userProfile?.avatarUrl ?: "https://api.dicebear.com/7.x/adventurer/png?seed=explorer"
        val newComment = ReelComment(
            id = UUID.randomUUID().toString(),
            username = "@$username",
            avatarUrl = avatar,
            text = text,
            timestamp = "Just now"
        )
        _reels.value = _reels.value.map { reel ->
            if (reel.id == reelId) {
                reel.copy(comments = reel.comments + newComment)
            } else reel
        }
    }

    fun shareReel(reelId: String) {
        _reels.value = _reels.value.map { reel ->
            if (reel.id == reelId) {
                reel.copy(shares = reel.shares + 1)
            } else reel
        }
    }

    private var isFetchingNextReel = false

    fun generateNextReel() {
        if (isFetchingNextReel) return
        isFetchingNextReel = true
        
        viewModelScope.launch {
            val isGeminiConfigured = GeminiClient.isConfigured
            var newReel: HeritageReel? = null
            
            if (isGeminiConfigured) {
                try {
                    val prompt = """
                        Generate a brand new, highly engaging TikTok/Reel short script presentation details for a unique, lesser-known Sri Lankan ancient heritage site, temple, fort, waterfall, beach, or scenic historical spot.
                        Your response MUST be a single raw JSON object with the exact following fields:
                        {
                          "siteName": "Name of the site",
                          "province": "The province of Sri Lanka (e.g. Central, Southern, North Central, Northern, Western, Sabaragamuwa, Uva, North Western, Eastern)",
                          "description": "An engaging TikTok style description with appropriate trending hashtags and emojis (max 150 characters)",
                          "category": "One of: 'Architecture', 'Ancient Cities', 'Religious', 'Nature'",
                          "author": "An engaging expert handle (e.g., '@lanka_explorer')"
                        }
                        Important: Do NOT wrap the JSON inside markdown code blocks (e.g., ```json ... ```) or any other formatting. Output ONLY the raw JSON string.
                    """.trimIndent()
                    
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = prompt))
                            )
                        )
                    )
                    
                    val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    val responseBody = response.body()
                    val responseText = responseBody?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (!responseText.isNullOrBlank()) {
                        var cleanJson = responseText.trim()
                        if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        }
                        
                        val json = JSONObject(cleanJson)
                        val siteName = json.getString("siteName")
                        val province = json.getString("province")
                        val description = json.getString("description")
                        val category = json.getString("category")
                        val author = json.getString("author")
                        
                        val videoUrls = listOf(
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
                        )
                        val selectedVideo = videoUrls[_reels.value.size % videoUrls.size]
                        
                        newReel = HeritageReel(
                            id = "reel_dynamic_" + System.currentTimeMillis(),
                            siteName = siteName,
                            province = province,
                            description = description,
                            videoUrl = selectedVideo,
                            imageUrl = "https://images.unsplash.com/featured/?srilanka,heritage,${siteName.replace(" ", "")}",
                            category = if (category in listOf("Architecture", "Ancient Cities", "Religious", "Nature")) category else "Ancient Cities",
                            author = author,
                            authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=${author.replace("@", "")}",
                            likes = (100..900).random(),
                            isLiked = false,
                            comments = emptyList(),
                            shares = (10..200).random()
                        )
                    }
                } catch (e: Exception) {
                    // Fallback
                }
            }
            
            if (newReel == null) {
                newReel = getOfflineGeneratedReel(null, _reels.value.size)
            }
            
            _reels.value = _reels.value + newReel
            isFetchingNextReel = false
        }
    }

    fun generateNewReel(query: String?, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isGeneratingReel.value = true
            _reelGenerationError.value = null
            
            var newReel: HeritageReel? = null
            val isGeminiConfigured = GeminiClient.isConfigured
            
            if (isGeminiConfigured) {
                try {
                    val prompt = """
                        Generate an engaging short video Reel script details for a Sri Lankan ancient heritage site, temple, fortress, scenic historical landmark matching: "${query ?: "a random amazing heritage spot"}".
                        Your response MUST be a single raw JSON object with the exact following fields:
                        {
                          "siteName": "Name of the site",
                          "province": "The province of Sri Lanka",
                          "description": "An engaging TikTok style description with hashtags and emojis (max 150 characters)",
                          "category": "One of: 'Architecture', 'Ancient Cities', 'Religious', 'Nature'",
                          "author": "An engaging expert handle (e.g. '@lanka_explorer')"
                        }
                        Important: Do NOT wrap the JSON inside markdown code blocks or any other formatting. Output ONLY the raw JSON string.
                    """.trimIndent()
                    
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = prompt))
                            )
                        )
                    )
                    
                    val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    val responseBody = response.body()
                    val responseText = responseBody?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (!responseText.isNullOrBlank()) {
                        var cleanJson = responseText.trim()
                        if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        }
                        
                        val json = JSONObject(cleanJson)
                        val siteName = json.getString("siteName")
                        val province = json.getString("province")
                        val description = json.getString("description")
                        val category = json.getString("category")
                        val author = json.getString("author")
                        
                        val videoUrls = listOf(
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
                        )
                        val selectedVideo = videoUrls.random()
                        
                        newReel = HeritageReel(
                            id = "reel_dynamic_user_" + System.currentTimeMillis(),
                            siteName = siteName,
                            province = province,
                            description = description,
                            videoUrl = selectedVideo,
                            imageUrl = "https://images.unsplash.com/featured/?srilanka,heritage,${siteName.replace(" ", "")}",
                            category = if (category in listOf("Architecture", "Ancient Cities", "Religious", "Nature")) category else "Ancient Cities",
                            author = author,
                            authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=${author.replace("@", "")}",
                            likes = (100..500).random(),
                            isLiked = false,
                            comments = emptyList(),
                            shares = (5..80).random()
                        )
                    }
                } catch (e: Exception) {
                    _reelGenerationError.value = "Gemini generation failed: ${e.message}"
                }
            }
            
            if (newReel == null) {
                newReel = getOfflineGeneratedReel(query, _reels.value.size)
            }
            
            _reels.value = listOf(newReel) + _reels.value
            _isGeneratingReel.value = false
            
            // Save newly created Reel to Supabase
            try {
                repository.saveReel(
                    SupabaseReel(
                        id = newReel.id,
                        userId = currentUser.value?.id ?: "guest",
                        siteName = newReel.siteName,
                        province = newReel.province,
                        description = newReel.description,
                        videoUrl = newReel.videoUrl,
                        thumbnailUrl = newReel.imageUrl,
                        category = newReel.category,
                        likes = newReel.likes,
                        shares = newReel.shares,
                        views = 120
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicViewModel", "Failed to save dynamic reel to Supabase", e)
            }

            onComplete?.invoke()
        }
    }

    private fun getOfflineGeneratedReel(query: String?, index: Int): HeritageReel {
        val q = query?.lowercase()?.trim() ?: ""
        val videoUrls = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
        )
        val selectedVideo = videoUrls[index % videoUrls.size]
        
        return when {
            q.contains("dambulla") -> HeritageReel(
                id = "reel_offline_dambulla_" + System.currentTimeMillis(),
                siteName = "Dambulla Cave Temple",
                province = "Central Province",
                description = "Exploring the largest and best-preserved cave temple complex in Sri Lanka. Golden Buddha statues and magnificent murals! 🛕✨ #dambulla #buddhism #cave",
                videoUrl = selectedVideo,
                imageUrl = "https://images.unsplash.com/featured/?srilanka,dambulla",
                category = "Religious",
                author = "@cave_explorer",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=cave_explorer",
                likes = 428,
                isLiked = false,
                comments = listOf(ReelComment("c_d1", "@monk_life", "https://api.dicebear.com/7.x/adventurer/png?seed=monk", "So peaceful", "1h ago")),
                shares = 54
            )
            q.contains("ruwanwelisaya") || q.contains("anuradhapura") -> HeritageReel(
                id = "reel_offline_ruwanwelisaya_" + System.currentTimeMillis(),
                siteName = "Ruwanwelisaya Stupa",
                province = "North Central Province",
                description = "Marveling at the grand white dome of Ruwanwelisaya, built by King Dutugemunu. A timeless relic of faith and engineering. 🏛️🤍 #anuradhapura #stupa #ancient",
                videoUrl = selectedVideo,
                imageUrl = "https://images.unsplash.com/featured/?srilanka,stupa",
                category = "Religious",
                author = "@sacred_lanka",
                authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=sacred",
                likes = 612,
                isLiked = false,
                comments = listOf(),
                shares = 120
            )
            else -> {
                val topics = listOf(
                    Triple("Polonnaruwa Vatadage", "North Central Province", "Stepping into the sacred stone walkways of the ancient Vatadage. The stone carvings here are unmatched! 🏛️💫 #polonnaruwa #ancient #carving"),
                    Triple("Buduruvagala Rock", "Uva Province", "Uncovering the colossal 10th-century Mahayana Buddhist statues carved directly into the giant rock face. 🗿🌿 #buduruvagala #mystery #carvings"),
                    Triple("Pidurangala Rock", "Central Province", "The absolute best place to watch the sunrise overlooking Sigiriya's Lion Rock. Peak trekking goals! 🌅🧗‍♂️ #pidurangala #trekking #sunrise"),
                    Triple("Yala Safari", "Southern Province", "Spotting majestic leopards and elephants roaming free in the untamed wilderness. 🐆🐘 #yala #wildlife #safari")
                )
                val selectedTopic = topics[index % topics.size]
                HeritageReel(
                    id = "reel_offline_gen_${index}_" + System.currentTimeMillis(),
                    siteName = selectedTopic.first,
                    province = selectedTopic.second,
                    description = selectedTopic.third,
                    videoUrl = selectedVideo,
                    imageUrl = "https://images.unsplash.com/featured/?srilanka,${selectedTopic.first.replace(" ", "")}",
                    category = "Ancient Cities",
                    author = "@lanka_guide",
                    authorAvatarUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=lankaguide",
                    likes = (150..500).random(),
                    isLiked = false,
                    comments = listOf(),
                    shares = (20..100).random()
                )
            }
        }
    }
}

data class HeritageReel(
    val id: String,
    val siteName: String,
    val province: String,
    val description: String,
    val videoUrl: String,
    val imageUrl: String,
    val category: String,
    val author: String,
    val authorAvatarUrl: String,
    val likes: Int,
    val isLiked: Boolean,
    val comments: List<ReelComment>,
    val shares: Int
)

data class ReelComment(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val text: String,
    val timestamp: String
)
