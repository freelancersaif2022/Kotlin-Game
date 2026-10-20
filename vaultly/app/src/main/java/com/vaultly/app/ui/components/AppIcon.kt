package com.vaultly.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.vaultly.app.R

/** The Vaultly launcher icon (midnight ground + gold shield), drawn from the adaptive icon layers. */
@Composable
fun AppIcon(size: Dp, modifier: Modifier = Modifier, border: Boolean = true) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .then(if (border) Modifier.border(size * 0.015f, Color.White.copy(alpha = 0.35f), shape) else Modifier),
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // The adaptive foreground keeps its art in the middle 2/3; zoom slightly so it fills the badge.
        Image(
            painterResource(R.drawable.ic_launcher_foreground), null,
            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.25f; scaleY = 1.25f },
            contentScale = ContentScale.Fit,
        )
    }
}
