package com.pdfmaster.app

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pdfmaster.app.data.db.DocumentDao
import com.pdfmaster.app.data.db.DocumentEntity
import com.pdfmaster.app.data.model.OutputFile
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.data.prefs.ThemeMode
import com.pdfmaster.app.ui.AppBottomBar
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.AppDialog
import com.pdfmaster.app.ui.components.AppSnackbarHost
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.ProcessingDialog
import com.pdfmaster.app.ui.components.SnackType
import com.pdfmaster.app.ui.components.SnackbarController
import com.pdfmaster.app.ui.editor.SignaturePadSheet
import com.pdfmaster.app.ui.editor.SignatureStore
import com.pdfmaster.app.ui.files.FilesScreen
import com.pdfmaster.app.ui.home.HomeScreen
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.navigation.Navigator
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.onboarding.OnboardingScreen
import com.pdfmaster.app.ui.premium.PremiumGateSheet
import com.pdfmaster.app.ui.premium.PremiumScreen
import com.pdfmaster.app.ui.result.ResultScreen
import com.pdfmaster.app.ui.result.ResultStore
import com.pdfmaster.app.ui.result.ToolResult
import com.pdfmaster.app.ui.settings.SettingsScreen
import com.pdfmaster.app.ui.splash.SplashScreen
import com.pdfmaster.app.ui.theme.PdfMasterTheme
import com.pdfmaster.app.ui.tool.ToolScreen
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.ui.tools.ToolsScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/**
 * Renders the real screens with Robolectric and saves PNGs to app/screenshots.
 * Run with: ./gradlew testDebugUnitTest --tests "*ScreenshotTest*"
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var dao: DocumentDao
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var results: ResultStore
    @Inject lateinit var signatures: SignatureStore

    private var scene by mutableIntStateOf(-1)

    @Before
    fun setUp() {
        hilt.inject()
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        runBlocking {
            prefs.setOnboardingDone()
            listOf(
                DocumentEntity("content://demo/1", "Annual_Report_2026.pdf", 4_812_000, 24, now - 5 * 60_000, isFavorite = true),
                DocumentEntity("content://demo/2", "Rental_Agreement_signed.pdf", 1_240_000, 6, now - 2 * hour, isCreated = true),
                DocumentEntity("content://demo/3", "Invoice_October.pdf", 312_000, 2, now - 26 * hour),
                DocumentEntity("content://demo/4", "Resume_Design_Lead.pdf", 486_000, 2, now - 3 * 24 * hour, isFavorite = true),
                DocumentEntity("content://demo/5", "Flight_Ticket_DXB.pdf", 198_000, 1, now - 9 * 24 * hour),
            ).forEach { dao.upsert(it) }
        }
    }

    private fun settle(ms: Long) {
        var left = ms
        while (left > 0) {
            compose.mainClock.advanceTimeBy(100)
            Thread.sleep(15)
            left -= 100
        }
    }

    private fun shot(name: String, index: Int, ms: Long = 2500) {
        compose.runOnUiThread { scene = index }
        settle(ms)
        captureScreenRoboImage("screenshots/$name.png")
    }

    @Test
    fun captureScreens() {
        compose.mainClock.autoAdvance = false
        compose.setContent { Scenes(scene) }
        settle(300)

        shot("01_splash", 0, 1200)
        shot("02_onboarding", 1)
        shot("03_home", 2)
        runBlocking { prefs.setPassUntil(System.currentTimeMillis() + 4 * 3_600_000L + 37 * 60_000L + 12_000L) }
        shot("04_home_premium_pass", 3)
        runBlocking { prefs.setPassUntil(0) }
        shot("05_tools", 4)
        shot("06_files", 5)
        shot("07_premium", 6, 3000)
        shot("08_unlock_sheet", 7)
        shot("09_compress_processing", 8)
        results.current = ToolResult(
            PdfTool.COMPRESS, "PDF compressed", "Reduced by 63% — saved 3.0 MB.",
            listOf(OutputFile(Uri.parse("content://demo/out"), "Annual_Report_2026_compressed.pdf", 1_780_000, "application/pdf", 24)),
            before = 4_812_000, after = 1_780_000,
        )
        shot("10_result", 9, 900)
        shot("11_settings", 10)
        shot("12_home_dark", 11)
        shot("13_snackbar_dialog", 12)
        shot("14_signature_pad", 13)
        shot("15_merge_tool", 14)
    }

    @Composable
    private fun Scenes(scene: Int) {
        val activity = compose.activity
        val nav = rememberNavController()
        val navigator = remember(nav) { Navigator(nav) }
        val host = remember(scene) { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val snackbar = remember(host) { SnackbarController(host, scope) }
        val dark = scene == 11
        PdfMasterTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
            CompositionLocalProvider(LocalAppDeps provides activity.deps, LocalNavigator provides navigator, LocalSnackbar provides snackbar) {
                val tabRoute = when (scene) {
                    2, 3, 11, 12 -> Routes.HOME
                    4, 7 -> Routes.TOOLS
                    5 -> Routes.FILES
                    10 -> Routes.SETTINGS
                    else -> null
                }
                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { AppSnackbarHost(host) },
                    bottomBar = { if (tabRoute != null) AppBottomBar(tabRoute) {} },
                ) { padding ->
                    val tab = PaddingValues(bottom = padding.calculateBottomPadding())
                    Box(Modifier.fillMaxSize()) {
                        when (scene) {
                            0 -> NavHost(nav, startDestination = Routes.SPLASH) {
                                composable(Routes.SPLASH) { SplashScreen(nav) }
                                composable(Routes.HOME) { Box(Modifier.fillMaxSize()) }
                                composable(Routes.ONBOARDING) { Box(Modifier.fillMaxSize()) }
                            }
                            1 -> OnboardingScreen(nav)
                            2, 3, 11 -> HomeScreen(tab)
                            4 -> ToolsScreen(tab)
                            5 -> FilesScreen(tab)
                            6 -> PremiumScreen("onboarding")
                            7 -> {
                                ToolsScreen(tab)
                                PremiumGateSheet("OCR Text Recognition", onDismiss = {}, onWatchAd = {}, onUpgrade = {})
                            }
                            8 -> {
                                ToolScreen(PdfTool.COMPRESS, null)
                                ProcessingDialog("Compressing your PDF", 0.64f)
                            }
                            9 -> ResultScreen()
                            10 -> SettingsScreen(tab)
                            12 -> {
                                HomeScreen(tab)
                                LaunchedEffect(Unit) {
                                    snackbar.show("All Pro tools are unlocked and ads are off for the next 5 hours.", SnackType.PREMIUM, title = "Premium unlocked!")
                                }
                                AppDialog(
                                    title = "Delete this file?",
                                    message = "\"Invoice_October.pdf\" will be permanently deleted from your device.",
                                    confirmText = "Delete",
                                    destructive = true,
                                    image = R.drawable.img_error_generic,
                                    onConfirm = {}, onDismiss = {},
                                )
                            }
                            13 -> {
                                Box(Modifier.fillMaxSize())
                                SignaturePadSheet(signatures, onDismiss = {}, onPicked = {})
                            }
                            14 -> ToolScreen(PdfTool.MERGE, Uri.parse("content://demo/1"))
                        }
                    }
                }
            }
        }
    }
}
