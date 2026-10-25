package com.twinspace.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.twinspace.app.core.SpaceStatus
import com.twinspace.app.ui.components.ActionRow
import com.twinspace.app.ui.components.SectionCard
import com.twinspace.app.ui.components.SectionLabel
import com.twinspace.app.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    status: SpaceStatus,
    spaceName: String,
    cloneCount: Int,
    lockEnabled: Boolean,
    versionName: String,
    snackbar: SnackbarHostState,
    onToggleLock: (Boolean) -> Unit,
    onRenameSpace: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onHowItWorks: () -> Unit,
    onDeleteSpace: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val paused = status == SpaceStatus.PAUSED

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            SectionLabel("Privacy")
            SectionCard {
                ActionRow(
                    Icons.Rounded.Fingerprint,
                    "App lock",
                    subtitle = "Ask for fingerprint, face or screen lock to open Twin Space",
                    onClick = { onToggleLock(!lockEnabled) },
                    trailing = { Switch(checked = lockEnabled, onCheckedChange = onToggleLock) },
                )
            }

            SectionLabel("Space")
            SectionCard {
                ActionRow(Icons.Rounded.Badge, "Space name", subtitle = spaceName, tint = colors.tertiary, onClick = onRenameSpace)
                HorizontalDivider(color = colors.outlineVariant)
                ActionRow(
                    if (paused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle,
                    if (paused) "Resume space" else "Pause space",
                    subtitle = if (paused) "Wake your clones up" else "Put every clone to sleep. Nothing is deleted.",
                    tint = extra.warning,
                    onClick = onTogglePause,
                )
                HorizontalDivider(color = colors.outlineVariant)
                ActionRow(
                    Icons.Rounded.WorkOutline,
                    "Android space settings",
                    subtitle = "Accounts, notifications and the Work apps switch",
                    tint = colors.secondary,
                    onClick = onOpenSystemSettings,
                )
            }

            SectionLabel("About")
            SectionCard {
                ActionRow(
                    Icons.Rounded.Verified,
                    "Free forever",
                    subtitle = "No ads, no in-app purchases and no tracking. Your data never leaves your phone.",
                    tint = extra.success,
                )
                HorizontalDivider(color = colors.outlineVariant)
                ActionRow(Icons.AutoMirrored.Rounded.HelpOutline, "How Twin Space works", subtitle = "Version $versionName", onClick = onHowItWorks)
            }

            SectionLabel("Danger zone")
            SectionCard {
                ActionRow(
                    Icons.Rounded.DeleteForever,
                    "Remove space",
                    subtitle = "Deletes the space and all $cloneCount clone${if (cloneCount == 1) "" else "s"} with their data. Your original apps stay.",
                    tint = colors.error,
                    titleColor = colors.error,
                    onClick = onDeleteSpace,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
