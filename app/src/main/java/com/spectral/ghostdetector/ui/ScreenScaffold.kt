package com.spectral.ghostdetector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectral.ghostdetector.ui.components.GhostBackground
import com.spectral.ghostdetector.ui.components.GlassIconButton
import com.spectral.ghostdetector.ui.theme.GColors

/** Full-screen page with the animated background and a back button title bar. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    GhostBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Text(title.uppercase(), style = MaterialTheme.typography.headlineSmall, color = GColors.TextPrimary, modifier = Modifier.weight(1f))
                actions()
            }
            content()
        }
    }
}
