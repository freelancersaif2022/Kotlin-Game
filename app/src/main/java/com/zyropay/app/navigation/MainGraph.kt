package com.zyropay.app.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.mainGraph(nav: NavController) {
    composable(Routes.MAIN) { Text("Main") }
}
