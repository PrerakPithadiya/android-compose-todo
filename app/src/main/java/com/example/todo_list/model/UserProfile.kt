package com.example.todo_list.model

/**
 * User Profile data class representing the authenticated/active user identity.
 */
data class UserProfile(
    val name: String = "Prerak Pithadiya",
    val username: String = "@prerak",
    val email: String = "prerak@taskflow.app",
    val phone: String = "+1 (555) 382-9012",
    val bio: String = "Productivity Architect & Android Developer • Building minimal, powerful tools ⚡",
    val avatarPresetId: Int = 0,
    val customAvatarUri: String? = null,
    val focusStatus: String = "🎯 Deep Work",
    val userTier: String = "TaskFlow Pro",
    val memberSince: String = "August 2026",
    val dailyTaskGoal: Int = 5,
    val morningDigestTime: String = "08:00 AM",
    val autoCloudSync: Boolean = true
)

/**
 * Focus Statuses available for quick mood / work status display.
 */
enum class FocusStatus(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long
) {
    DEEP_WORK("deep_work", "Deep Work", "🎯", 0xFF007AFF),
    IN_A_FLOW("in_a_flow", "In the Flow", "⚡", 0xFFFF9500),
    SHIPPING("shipping", "Shipping Code", "🚀", 0xFFAF52DE),
    COFFEE_BREAK("coffee_break", "Coffee Break", "☕", 0xFF8E8E93),
    ON_HOLIDAY("on_holiday", "On Holiday", "🏖️", 0xFF34C759);

    companion object {
        fun fromLabel(label: String): FocusStatus =
            entries.find { it.label.equals(label, ignoreCase = true) || label.contains(it.emoji) } ?: DEEP_WORK
    }
}

/**
 * Curated preset Memojis / Stylized Avatars for Apple HIG aesthetic.
 */
data class AvatarPreset(
    val id: Int,
    val name: String,
    val emoji: String,
    val avatarUrl: String,
    val gradientStartHex: Long,
    val gradientEndHex: Long
) {
    companion object {
        val PRESETS = listOf(
            AvatarPreset(
                id = 0,
                name = "Apple Architect",
                emoji = "👨‍💻",
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCoBuvdtAGCYMF3-7fgZnTDoXh_snzGoLq1GpOuiTnpGcrs_BUFGcSioVrt4viicVuGC9ZFj9lsxuXEX_szLOWSsdMaaKlMDlKGSJJcfKhyUthyTuHEgIgtHGDUcLm3JMsUS7KQWaovaJxSOD24Pd9PJg9MrXyDZLwQHIBQ2P-6aUTKoBr9ebkyIWiEcuxV_wt95LmVK_nmu9kMlfXd-pio3PZqk-2_pF6npzfw8BXNvJ6lIacTKXE",
                gradientStartHex = 0xFF007AFF,
                gradientEndHex = 0xFF5856D6
            ),
            AvatarPreset(
                id = 1,
                name = "Zen Master",
                emoji = "🧘",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFF34C759,
                gradientEndHex = 0xFF30B0C7
            ),
            AvatarPreset(
                id = 2,
                name = "Cyber Focus",
                emoji = "🎧",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFFAF52DE,
                gradientEndHex = 0xFFFF2D55
            ),
            AvatarPreset(
                id = 3,
                name = "Minimalist",
                emoji = "⚡",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFFFF9500,
                gradientEndHex = 0xFFFFCC00
            ),
            AvatarPreset(
                id = 4,
                name = "Rocket Explorer",
                emoji = "🚀",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFF007AFF,
                gradientEndHex = 0xFF30D158
            ),
            AvatarPreset(
                id = 5,
                name = "Creative Genius",
                emoji = "🎨",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFFFF2D55,
                gradientEndHex = 0xFFAF52DE
            ),
            AvatarPreset(
                id = 6,
                name = "Visionary",
                emoji = "👓",
                avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFF5856D6,
                gradientEndHex = 0xFF007AFF
            ),
            AvatarPreset(
                id = 7,
                name = "Speed Demon",
                emoji = "🏎️",
                avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&auto=format&fit=crop&q=80",
                gradientStartHex = 0xFFFF3B30,
                gradientEndHex = 0xFFFF9500
            )
        )

        fun getById(id: Int): AvatarPreset =
            PRESETS.find { it.id == id } ?: PRESETS[0]
    }
}

/**
 * Tiers of Achievement Badges with Apple Watch metal styling.
 */
enum class AchievementTier(
    val title: String,
    val colorHex: Long,
    val containerColorHex: Long
) {
    BRONZE("Bronze", 0xFFCD7F32, 0x2BCD7F32),
    SILVER("Silver", 0xFFC0C0C0, 0x2BC0C0C0),
    GOLD("Gold", 0xFFFFD700, 0x33FFD700),
    DIAMOND("Diamond", 0xFF5856D6, 0x335856D6),
    PLATINUM("Platinum", 0xFF007AFF, 0x33007AFF)
}

/**
 * Achievement badge data model representing Apple Fitness / Game Center style medals.
 */
data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val tier: AchievementTier,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val maxProgress: Int,
    val unlockedDate: String? = null,
    val xpReward: Int = 100
) {
    companion object {
        val DEFAULT_ACHIEVEMENTS = listOf(
            AchievementBadge(
                id = "first_step",
                title = "First Step",
                description = "Create and complete your first task on TaskFlow.",
                emoji = "🌟",
                tier = AchievementTier.BRONZE,
                isUnlocked = true,
                currentProgress = 1,
                maxProgress = 1,
                unlockedDate = "Aug 15, 2026",
                xpReward = 50
            ),
            AchievementBadge(
                id = "streak_fire",
                title = "Week on Fire",
                description = "Maintain a continuous 7-day task completion streak.",
                emoji = "🔥",
                tier = AchievementTier.GOLD,
                isUnlocked = true,
                currentProgress = 7,
                maxProgress = 7,
                unlockedDate = "Aug 22, 2026",
                xpReward = 250
            ),
            AchievementBadge(
                id = "hyper_focus",
                title = "Hyper Focus",
                description = "Complete 8 or more tasks in a single calendar day.",
                emoji = "⚡",
                tier = AchievementTier.PLATINUM,
                isUnlocked = true,
                currentProgress = 8,
                maxProgress = 8,
                unlockedDate = "Aug 25, 2026",
                xpReward = 300
            ),
            AchievementBadge(
                id = "early_bird",
                title = "Early Bird",
                description = "Check off 5 morning tasks before 9:00 AM.",
                emoji = "🌅",
                tier = AchievementTier.SILVER,
                isUnlocked = true,
                currentProgress = 5,
                maxProgress = 5,
                unlockedDate = "Aug 26, 2026",
                xpReward = 150
            ),
            AchievementBadge(
                id = "master_organizer",
                title = "Master Organizer",
                description = "Structure your workflow across 4 or more custom categories.",
                emoji = "📚",
                tier = AchievementTier.SILVER,
                isUnlocked = true,
                currentProgress = 4,
                maxProgress = 4,
                unlockedDate = "Aug 20, 2026",
                xpReward = 150
            ),
            AchievementBadge(
                id = "fort_knox",
                title = "Fort Knox",
                description = "Secure your TaskFlow vault with App Lock & Biometrics.",
                emoji = "🛡️",
                tier = AchievementTier.DIAMOND,
                isUnlocked = true,
                currentProgress = 1,
                maxProgress = 1,
                unlockedDate = "Aug 26, 2026",
                xpReward = 200
            ),
            AchievementBadge(
                id = "perfectionist",
                title = "Perfectionist",
                description = "Achieve 100% completion on all scheduled tasks for 5 consecutive days.",
                emoji = "🎯",
                tier = AchievementTier.GOLD,
                isUnlocked = false,
                currentProgress = 3,
                maxProgress = 5,
                unlockedDate = null,
                xpReward = 400
            ),
            AchievementBadge(
                id = "century_club",
                title = "Century Club",
                description = "Reach 100 lifetime completed tasks in TaskFlow.",
                emoji = "👑",
                tier = AchievementTier.DIAMOND,
                isUnlocked = false,
                currentProgress = 48,
                maxProgress = 100,
                unlockedDate = null,
                xpReward = 500
            )
        )
    }
}
