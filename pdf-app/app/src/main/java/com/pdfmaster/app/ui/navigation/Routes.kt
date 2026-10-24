package com.pdfmaster.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.pdfmaster.app.ui.tools.PdfTool

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val FILES = "files"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium?source={source}"
    const val VIEWER = "viewer?uri={uri}"
    const val EDITOR = "editor?uri={uri}&mode={mode}"
    const val TOOL = "tool/{tool}?uri={uri}"
    const val RESULT = "result"
    const val SEARCH = "search"

    val tabs = listOf(HOME, FILES, TOOLS, SETTINGS)

    fun premium(source: String) = "premium?source=$source"
    fun viewer(uri: Uri) = "viewer?uri=${Uri.encode(uri.toString())}"
    fun editor(uri: Uri, mode: String) = "editor?uri=${Uri.encode(uri.toString())}&mode=$mode"
    fun tool(tool: PdfTool, uri: Uri? = null) = "tool/${tool.name}" + (uri?.let { "?uri=${Uri.encode(it.toString())}" } ?: "")
}

/** Navigation actions available to any composable without threading the controller through. */
@Stable
class Navigator(private val nav: NavHostController) {
    fun back() = nav.popBackStack()
    fun premium(source: String) = nav.navigate(Routes.premium(source)) { launchSingleTop = true }
    fun viewer(uri: Uri) = nav.navigate(Routes.viewer(uri)) { launchSingleTop = true }
    fun editor(uri: Uri, mode: String) = nav.navigate(Routes.editor(uri, mode))
    fun tool(tool: PdfTool, uri: Uri? = null) = nav.navigate(Routes.tool(tool, uri)) { launchSingleTop = true }
    fun search() = nav.navigate(Routes.SEARCH) { launchSingleTop = true }

    fun result(from: String?) = nav.navigate(Routes.RESULT) {
        if (from != null) popUpTo(from) { inclusive = true }
    }

    fun tab(route: String) = nav.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun home() = nav.navigate(Routes.HOME) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
