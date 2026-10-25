package com.saif.imagetopdf.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FailedScreen(message: String, onBack: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(CircleShape).background(Palette.Red.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(AppIcons.Close, null, tint = Palette.Red, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(22.dp))
        Text("Couldn't convert this image", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Palette.Text)
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp), color = Palette.Muted)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Try another image", AppIcons.Upload, Modifier.fillMaxWidth(), onBack)
    }
}
