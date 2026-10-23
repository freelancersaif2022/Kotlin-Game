package com.saif.pymaster

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import org.robolectric.shadows.ShadowLooper
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.saif.pymaster.cert.CertificateData
import com.saif.pymaster.cert.CertificateRenderer
import com.saif.pymaster.data.AppNotification
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.DailyQuizState
import com.saif.pymaster.data.NotificationType
import com.saif.pymaster.data.Profile
import com.saif.pymaster.data.Progress
import com.saif.pymaster.data.Rules
import com.saif.pymaster.data.Settings
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.TerminalViewModel
import com.saif.pymaster.ui.screens.CertificateScreen
import com.saif.pymaster.ui.screens.CourseScreen
import com.saif.pymaster.ui.screens.DailyQuizScreen
import com.saif.pymaster.ui.screens.HomeScreen
import com.saif.pymaster.ui.screens.LessonScreen
import com.saif.pymaster.ui.screens.ModuleQuizScreen
import com.saif.pymaster.ui.screens.ModuleScreen
import com.saif.pymaster.ui.screens.NotificationsScreen
import com.saif.pymaster.ui.screens.OnboardingScreen
import com.saif.pymaster.ui.screens.ProfileSetupScreen
import com.saif.pymaster.ui.screens.ProjectDetailScreen
import com.saif.pymaster.ui.screens.ProjectsScreen
import com.saif.pymaster.ui.screens.SettingsScreen
import com.saif.pymaster.ui.screens.SplashScreen
import com.saif.pymaster.ui.screens.TerminalScreen
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.ui.theme.PyMasterTheme
import com.saif.pymaster.util.LocalHaptics
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the main screens to PNG files in app/build/screenshots so layouts
 * can be reviewed without a device. Run: ./gradlew testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = Application::class)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val outDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }

    @Before
    fun setUp() {
        Content.init(app)
    }

    private fun save(name: String, bitmap: Bitmap) {
        FileOutputStream(File(outDir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shoot(name: String, advanceMs: Long = 3000, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PyMasterTheme {
                CompositionLocalProvider(LocalHaptics provides null) {
                    Box(Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
                        Column { content() }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(advanceMs)
        ShadowLooper.idleMainLooper()
        compose.mainClock.advanceTimeBy(100)
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.activity.runOnUiThread { view.draw(Canvas(bitmap)) }
        save(name, bitmap)
    }

    private fun progressState(): AppState {
        val modules = Content.modules
        val done = modules.take(3)
        val lessons = done.flatMap { m -> m.lessons.map { it.id } } + modules[3].lessons.take(2).map { it.id }
        val today = Rules.today()
        val study = (0..6).associate { (today - it).toString() to listOf(1500L, 900L, 2400L, 0L, 1200L, 3000L, 600L)[it] }
        return AppState(
            onboarded = true,
            profile = Profile(name = "Saif Ahmed", avatarIndex = 0, level = "Beginner", goal = "Get a developer job", dailyGoalMinutes = 30, createdAt = 1_760_000_000_000L),
            settings = Settings(),
            progress = Progress(
                completedLessons = lessons.toSet(),
                passedQuizzes = done.map { it.id }.toSet(),
                quizScores = done.associate { it.id to 5 },
                xp = 470,
                streak = 6,
                bestStreak = 9,
                lastActiveDay = today,
                studySeconds = study,
                totalStudySeconds = 9 * 3600L + 25 * 60L,
                codeRuns = 37,
                dailyQuiz = DailyQuizState(),
            ),
            notifications = listOf(
                AppNotification(5, "New daily quiz is ready ⚡", "4 fresh Python questions are waiting for you. Keep your streak alive!", NotificationType.QUIZ, System.currentTimeMillis() - 60_000),
                AppNotification(4, "Session complete ✅", "You finished \"if ... else\". +10 XP", NotificationType.LESSON, System.currentTimeMillis() - 3_600_000),
                AppNotification(3, "Module 3 complete 🎉", "You mastered \"Operators\" with 5/6. +50 XP", NotificationType.MODULE, System.currentTimeMillis() - 7_200_000, read = true),
                AppNotification(2, "Module quiz unlocked 🧠", "All lessons of \"Operators\" are done.", NotificationType.QUIZ_UNLOCK, System.currentTimeMillis() - 2 * 86_400_000L, read = true),
                AppNotification(1, "Welcome to PyMaster, Saif Ahmed 👋", "Your profile is saved on this phone.", NotificationType.INFO, System.currentTimeMillis() - 3 * 86_400_000L, read = true),
            ),
        )
    }

    private fun completedState(): AppState {
        val s = progressState()
        return s.copy(
            progress = s.progress.copy(
                completedLessons = Content.allLessons.map { it.id }.toSet(),
                passedQuizzes = Content.modules.map { it.id }.toSet(),
                completedProjects = Content.projects.take(4).map { it.id }.toSet(),
                projectSteps = mapOf(Content.projects[4].id to setOf(0, 1)),
                courseCompletedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun certificateState(): AppState {
        val s = completedState()
        return s.copy(
            progress = s.progress.copy(
                completedProjects = Content.projects.map { it.id }.toSet(),
                certificateId = "PYM-2026-7K3QX9ZA",
                certificateIssuedAt = System.currentTimeMillis(),
            )
        )
    }

    @Test fun s01_splash() = shoot("01_splash", 1600) { SplashScreen(null) {} }

    @Test fun s02_onboarding() = shoot("02_onboarding") { OnboardingScreen {} }

    @Test fun s03_setup() = shoot("03_profile_setup") { ProfileSetupScreen(initial = null, onSave = {}) }

    @Test fun s04_home() = shoot("04_home") { HomeScreen(progressState(), Navigator(rememberNavController())) }

    @Test fun s05_home_new() = shoot("05_home_new_user") {
        val s = progressState()
        HomeScreen(s.copy(progress = Progress(), notifications = emptyList()), Navigator(rememberNavController()))
    }

    @Test fun s06_course() = shoot("06_course") { CourseScreen(progressState(), Navigator(rememberNavController())) }

    @Test fun s07_module() = shoot("07_module") { ModuleScreen(progressState(), "m04", Navigator(rememberNavController())) }

    @Test fun s08_lesson() = shoot("08_lesson") {
        LessonScreen(progressState(), "m02l3", MainViewModel(app), Navigator(rememberNavController())) {}
    }

    @Test fun s09_quiz() = shoot("09_module_quiz") {
        ModuleQuizScreen(progressState(), "m04", MainViewModel(app), Navigator(rememberNavController()))
    }

    @Test fun s10_daily() = shoot("10_daily_quiz") { DailyQuizScreen(progressState(), MainViewModel(app), Navigator(rememberNavController())) }

    @Test fun s11_terminal() = shoot("11_terminal") { TerminalScreen(progressState(), TerminalViewModel(app)) }

    @Test fun s12_settings() = shoot("12_settings") { SettingsScreen(progressState(), MainViewModel(app), Navigator(rememberNavController())) }

    @Test fun s13_notifications() = shoot("13_notifications") { NotificationsScreen(progressState(), MainViewModel(app), Navigator(rememberNavController())) }

    @Test fun s14_projects_locked() = shoot("14_projects_locked") { ProjectsScreen(progressState(), Navigator(rememberNavController())) }

    @Test fun s15_projects() = shoot("15_projects") { ProjectsScreen(completedState(), Navigator(rememberNavController())) }

    @Test fun s16_project_detail() = shoot("16_project_detail") {
        ProjectDetailScreen(completedState(), Content.projects[4].id, MainViewModel(app), Navigator(rememberNavController())) {}
    }

    @Test fun s17_certificate_locked() = shoot("17_certificate_locked") { CertificateScreen(progressState(), Navigator(rememberNavController())) }

    @Test fun s18_certificate_screen() = shoot("18_certificate_screen", 3000) { CertificateScreen(certificateState(), Navigator(rememberNavController())) }

    @Test fun s19_certificate_image() {
        val data = CertificateData("Muhammad Saifullah Rahman", "23 October 2026", "PYM-2026-7K3QX9ZA", Content.modules.size, Content.totalLessons, Content.projects.size)
        save("19_certificate", CertificateRenderer.render(app, data, highRes = false))
        save("19b_certificate_short_name", CertificateRenderer.render(app, data.copy(name = "Saif Ahmed"), highRes = false))
    }
}
