package com.lexora.english

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.lexora.english.content.Course
import com.lexora.english.content.DailyQuiz
import com.lexora.english.content.Projects
import com.lexora.english.data.AppNotification
import com.lexora.english.data.AppState
import com.lexora.english.data.PracticeResult
import com.lexora.english.data.Progress
import com.lexora.english.data.UserProfile
import com.lexora.english.ui.Nav
import com.lexora.english.ui.practice.PracticeSessionScreen
import com.lexora.english.ui.screens.CertificateScreen
import com.lexora.english.ui.screens.DailyQuizScreen
import com.lexora.english.ui.screens.LessonScreen
import com.lexora.english.ui.screens.MainScreen
import com.lexora.english.ui.screens.ModuleQuizScreen
import com.lexora.english.ui.screens.ModuleScreen
import com.lexora.english.ui.screens.NotificationsScreen
import com.lexora.english.ui.screens.OnboardingScreen
import com.lexora.english.ui.screens.ProfileSetupScreen
import com.lexora.english.ui.screens.ProjectScreen
import com.lexora.english.ui.screens.HomeScreen
import com.lexora.english.ui.screens.LearnScreen
import com.lexora.english.ui.screens.PracticeScreen
import com.lexora.english.ui.screens.SettingsScreen
import com.lexora.english.ui.screens.SplashScreen
import com.lexora.english.ui.theme.Lex
import com.lexora.english.ui.theme.LexoraTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

class TestApp : Application()

/** Renders every main screen so layout can be reviewed without a device. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = TestApp::class)
class ScreenshotTest {

    private val today = LocalDate.now()

    private val profile = UserProfile(
        name = "Alexandra Montgomery", avatarIndex = 1, level = "Intermediate",
        goal = "Career", dailyMinutes = 15, createdAt = 1_760_000_000_000,
    )

    private val notifications = listOf(
        AppNotification(1, "Lesson complete: Present Perfect", "+20 XP · Tenses: 3 of 4 lessons complete.", "lesson", System.currentTimeMillis() - 60_000),
        AppNotification(2, "5 new daily questions are ready", "Today's challenge: grammar, vocabulary and idioms. Keep your streak alive!", "daily", System.currentTimeMillis() - 3_600_000, read = true),
        AppNotification(3, "Module 3 passed with 83%", "Great work on Nouns & Pronouns! Module 4: Verbs & Actions is now unlocked.", "module", System.currentTimeMillis() - 90_000_000, read = true),
        AppNotification(4, "5-day streak! 🔥", "You have learned 5 days in a row. Keep the fire burning!", "streak", System.currentTimeMillis() - 100_000_000, read = true),
    )

    private fun midProgress(): Progress {
        val lessons = Course.modules.take(4).flatMap { it.lessons }.map { it.id } + Course.modules[4].lessons.take(2).map { it.id }
        val minutes = listOf(14L, 22, 9, 0, 17, 25, 11)
        val qs = DailyQuiz.questionsFor(today)
        return Progress(
            completedLessons = lessons.toSet(),
            passedModules = mapOf("m1" to 100, "m2" to 83, "m3" to 83, "m4" to 100),
            xp = 1240,
            studySeconds = (0..6).associate { today.minusDays(it.toLong()).toString() to minutes[it] * 60 },
            streak = 5, bestStreak = 5, lastActiveDay = today.toString(),
            dailyQuizDate = today.toString(),
            dailyAnswers = listOf(qs[0].answer, (qs[1].answer + 1) % qs[1].options.size, -1, -1, -1),
            practiceHistory = listOf(
                PracticeResult("test", "Grammar Test · 3m 12s", 8, 10, System.currentTimeMillis() - 5_000_000),
                PracticeResult("write", "Writing: My Daily Routine", 86, 100, System.currentTimeMillis() - 9_000_000),
            ),
            knownWords = setOf("achieve", "adapt", "benefit"),
        )
    }

    private fun doneProgress(): Progress = midProgress().copy(
        completedLessons = Course.modules.flatMap { it.lessons }.map { it.id }.toSet(),
        passedModules = Course.modules.associate { it.id to 92 },
        projectTasks = Projects.all.associate { it.id to Projects.TASKS },
        completedProjects = Projects.all.associate { it.id to System.currentTimeMillis() },
        courseCompletedAt = System.currentTimeMillis(),
        certificateIssuedAt = 1_793_000_000_000,
        certificateId = "LX-2026-482913",
    )

    private val newUser = AppState(onboardingDone = true, profile = profile, tutorialDone = true, notifications = notifications.take(1))
    private val midUser = AppState(onboardingDone = true, profile = profile, tutorialDone = true, progress = midProgress(), notifications = notifications)
    private val doneUser = AppState(onboardingDone = true, profile = profile, tutorialDone = true, progress = doneProgress(), notifications = notifications)

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .dispatcher(Dispatchers.Unconfined)
                .allowHardware(false)
                .crossfade(false)
                .build()
        )
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable (Nav) -> Unit) {
        captureRoboImage("build/outputs/roborazzi/$name${if (dark) "_dark" else ""}.png") {
            LexoraTheme(if (dark) "dark" else "light", 1f) {
                val nav = Nav(rememberNavController())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Lex.colors.background)
                ) { content(nav) }
            }
        }
    }

    @Test fun splash() = shot("01_splash") { SplashScreen {} }
    @Test fun onboarding() = shot("02_onboarding") { OnboardingScreen {} }
    @Test fun setup() = shot("03_profile_setup") { ProfileSetupScreen(null, {}, null) }
    @Test fun tutorial() = shot("04_home_tutorial") { MainScreen(newUser.copy(tutorialDone = false), it) }
    @Test fun homeNew() = shot("05_home_new") { MainScreen(newUser, it) }
    @Test fun homeMid() = shot("06_home_progress") { MainScreen(midUser, it) }
    @Test fun homeMidDark() = shot("06_home_progress", dark = true) { MainScreen(midUser, it) }
    @Test fun homeDone() = shot("07_home_certified") { MainScreen(doneUser, it) }
    @Test fun learn() = shot("08_learn") { LearnScreen(midUser, it, rememberLazyListState()) }
    @Test fun learnDark() = shot("08_learn", dark = true) { LearnScreen(midUser, it, rememberLazyListState()) }
    @Test fun practice() = shot("09_practice") { PracticeScreen(midUser, it, rememberLazyListState()) }
    @Test fun settings() = shot("10_settings") { SettingsScreen(midUser, it, rememberLazyListState()) }
    @Test fun settingsDark() = shot("10_settings", dark = true) { SettingsScreen(midUser, it, rememberLazyListState()) }
    @Test fun module() = shot("11_module") { ModuleScreen("m5", midUser, it) }
    @Test fun lesson() = shot("12_lesson") { LessonScreen("m5_l3", midUser, it) }
    @Test fun lessonDark() = shot("12_lesson", dark = true) { LessonScreen("m5_l3", midUser, it) }
    @Test fun moduleQuiz() = shot("13_module_quiz") { Column { ModuleQuizScreen("m4", it) } }
    @Test fun daily() = shot("14_daily_quiz") { DailyQuizScreen(midUser, it) }
    @Test fun projectLocked() = shot("15_project_locked") { ProjectScreen("p1", midUser, it) }
    @Test fun project() = shot("16_project_brief") { ProjectScreen("p3", doneUser, it) }
    @Test fun certificate() = shot("17_certificate") { CertificateScreen(doneUser, it) }
    @Test fun certificateLocked() = shot("18_certificate_locked") { CertificateScreen(midUser, it) }
    @Test fun notificationsScreen() = shot("19_notifications") { NotificationsScreen(midUser, it) }
    @Test fun notificationsDark() = shot("19_notifications", dark = true) { NotificationsScreen(midUser, it) }
    @Test fun writing() = shot("20_practice_writing") { PracticeSessionScreen("write", midUser, it) }
    @Test fun builder() = shot("21_practice_builder") { PracticeSessionScreen("build", midUser, it) }
    @Test fun cards() = shot("22_practice_flashcards") { PracticeSessionScreen("cards", midUser, it) }
    @Test fun speak() = shot("23_practice_speaking") { PracticeSessionScreen("speak", midUser, it) }
}
