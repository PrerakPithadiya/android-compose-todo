package com.example.todo_list.manager

import com.example.todo_list.model.AvatarPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileManagerTest {

    @Test
    fun `test avatar preset returns valid URL when custom avatar is null`() {
        UserProfileManager.saveProfile(
            name = "Test User",
            avatarPresetId = 2,
            customAvatarUri = null
        )

        assertEquals(2, UserProfileManager.profile.avatarPresetId)
        assertNull(UserProfileManager.profile.customAvatarUri)
        assertEquals(AvatarPreset.getById(2).avatarUrl, UserProfileManager.getAvatarUrl())
    }

    @Test
    fun `test custom avatar uri overrides avatar preset`() {
        val customUri = "content://media/external/images/media/42"
        UserProfileManager.saveProfile(
            name = "Test User",
            avatarPresetId = 3,
            customAvatarUri = customUri
        )

        assertEquals(customUri, UserProfileManager.getAvatarUrl())
    }

    @Test
    fun `test removing custom avatar falls back to preset avatar url`() {
        UserProfileManager.saveProfile(
            name = "Test User",
            avatarPresetId = 4,
            customAvatarUri = "content://media/external/images/media/99"
        )
        UserProfileManager.setAvatarPreset(4)

        assertEquals(AvatarPreset.getById(4).avatarUrl, UserProfileManager.getAvatarUrl())
        assertNull(UserProfileManager.profile.customAvatarUri)
    }

    @Test
    fun `test saveProfile updates all profile attributes atomically`() {
        UserProfileManager.saveProfile(
            name = "Jane Architect",
            username = "@janearch",
            email = "jane@example.com",
            phone = "+1 555-1234",
            bio = "Minimalist designer & builder",
            avatarPresetId = 5,
            customAvatarUri = null
        )

        val profile = UserProfileManager.profile
        assertEquals("Jane Architect", profile.name)
        assertEquals("@janearch", profile.username)
        assertEquals("jane@example.com", profile.email)
        assertEquals("+1 555-1234", profile.phone)
        assertEquals("Minimalist designer & builder", profile.bio)
        assertEquals(5, profile.avatarPresetId)
        assertNull(profile.customAvatarUri)
        assertEquals(AvatarPreset.getById(5).avatarUrl, UserProfileManager.getAvatarUrl())
    }
}
