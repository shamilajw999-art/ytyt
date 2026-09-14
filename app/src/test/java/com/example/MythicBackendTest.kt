package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProfileEntity
import com.example.data.repository.MythicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MythicBackendTest {

    private lateinit var context: Context
    private lateinit var repository: MythicRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = MythicRepository(context)
    }

    @Test
    fun testSignUpAndEmailVerification() = runBlocking {
        // Clear previous state first
        repository.clearProfile()

        // 1. Test SignUp
        val email = "testuser@mythic.lk"
        val result = repository.signUp("testuser", email, "password123")
        
        // Assert signup triggered verification email request (since Supabase is mock/configured)
        assertTrue(result == "VERIFICATION_SENT" || result == "VERIFICATION_SENT_MOCK")

        // 2. Verify Profile is created locally in provisional state
        val cachedProfile = repository.getProfile()
        assertNotNull(cachedProfile)
        assertEquals("testuser", cachedProfile?.username)
        assertEquals(email, cachedProfile?.email)
    }

    @Test
    fun testLoginAndLogout() = runBlocking {
        repository.clearProfile()

        // 1. Test Login with mock dynamic registration
        val loginResult = repository.login("testuser@mythic.lk", "password123")
        assertNull(loginResult) // Null means success

        val currentUser = repository.currentUser.value
        assertNotNull(currentUser)
        assertEquals("testuser@mythic.lk", currentUser?.email)

        // 2. Test Logout
        repository.logout()
        assertNull(repository.currentUser.value)
        assertNull(repository.getProfile())
    }

    @Test
    fun testXpProgressionSaving() = runBlocking {
        repository.clearProfile()

        // Setup profile first
        val profile = ProfileEntity(
            id = "user_xp_test",
            username = "xp_explorer",
            email = "xp@mythic.lk",
            fullName = "XP Explorer",
            avatarUrl = "",
            level = 1,
            xp = 100,
            streak = 1,
            scansCount = 0,
            badgesCount = 0
        )
        repository.insertProfile(profile)

        // Add 500 XP
        repository.addXp(500)

        // Assert level & xp progression saved locally
        val updatedProfile = repository.getProfile()
        assertNotNull(updatedProfile)
        assertEquals(600, updatedProfile?.xp)
        assertEquals(2, updatedProfile?.level) // 600 / 600 + 1 = 2
    }

    @Test
    fun testBadgeUnlocking() = runBlocking {
        repository.clearBadges()

        // Unlock a bronze explorer badge
        repository.unlockBadge("explorer_test", "Test Explorer", "Unlocked for testing", "bronze", "🏆")

        // Assert badge is saved
        val currentBadges = repository.badges.first()
        assertEquals(1, currentBadges.size)
        assertEquals("explorer_test", currentBadges[0].code)
        assertEquals("Test Explorer", currentBadges[0].name)
    }

    @Test
    fun testProfileUpdates() = runBlocking {
        repository.clearProfile()

        val initialProfile = ProfileEntity(
            id = "profile_test_id",
            username = "initial_name",
            email = "test@mythic.lk",
            fullName = "Initial Name",
            avatarUrl = "",
            level = 1,
            xp = 0,
            streak = 1,
            scansCount = 0,
            badgesCount = 0
        )
        repository.insertProfile(initialProfile)

        // Update profile
        val updatedProfile = initialProfile.copy(fullName = "Updated Name", avatarUrl = "updated_avatar_url")
        repository.insertProfile(updatedProfile)

        // Verify changes are saved
        val cached = repository.getProfile()
        assertNotNull(cached)
        assertEquals("Updated Name", cached?.fullName)
        assertEquals("updated_avatar_url", cached?.avatarUrl)
    }
}
