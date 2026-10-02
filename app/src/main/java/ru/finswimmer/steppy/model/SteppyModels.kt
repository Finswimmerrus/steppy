package ru.finswimmer.steppy.model

enum class DayType(val label: String) { ACTIVE("Активный"), POWER("Усиленный"), FREE("Свободный") }
enum class DayStatus { COMPLETED, TODAY, UPCOMING, PENDING_SYNC, UNDER_REVIEW }
enum class MainTab { TODAY, MY_GAMES, SEARCH, CREATE, PROFILE }

data class ChallengeDay(
    val weekday: String,
    val dayNumber: Int,
    val type: DayType,
    val target: Long?,
    val steps: Long,
    val status: DayStatus,
)

data class GoalSnapshot(
    val activeGoal: Long,
    val powerGoal: Long,
    val algorithmVersion: String,
    val calculatedAt: String,
)

data class FeedEvent(val initials: String, val title: String, val detail: String, val accent: Long)

data class SteppyUiState(
    val onboarded: Boolean = false,
    val tab: MainTab = MainTab.TODAY,
    val steps: Long = 6_420,
    val isSyncing: Boolean = false,
    val healthConnected: Boolean = false,
    val healthAvailable: Boolean = true,
    val lastSync: String = "сегодня, 14:32",
    val challengeTitle: String = "Осенний ритм",
    val week: Int = 2,
    val totalWeeks: Int = 4,
    val snapshot: GoalSnapshot = GoalSnapshot(8_000, 10_000, "demo-v1", "до вступления"),
)
