package com.spectral.ghostdetector.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.spectral.ghostdetector.GhostApp

@Composable
fun rememberApp(): GhostApp = LocalContext.current.applicationContext as GhostApp

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
