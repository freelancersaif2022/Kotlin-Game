package com.calorielens.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

fun MealEntity.timeLabel(): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(timeFormat)

@Composable
fun MealPhoto(path: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(path))
            .crossfade(250)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(AppTheme.colors.cardElevated),
    )
}

@Composable
fun MealCard(meal: MealEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(scaleDown = 0.98f, onClick = onClick)
            .clip(shape)
            .background(AppTheme.colors.card)
            .border(1.dp, AppTheme.colors.stroke, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MealPhoto(
            meal.imagePath,
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                meal.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${meal.mealType.label} · ${meal.timeLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.muted,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MacroDot(Brand.Protein, "${meal.protein.toInt()}g P")
                Spacer(Modifier.width(10.dp))
                MacroDot(Brand.Carbs, "${meal.carbs.toInt()}g C")
                Spacer(Modifier.width(10.dp))
                MacroDot(Brand.Fat, "${meal.fat.toInt()}g F")
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Brand.Ember, modifier = Modifier.size(16.dp))
                Text(
                    "${meal.calories}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text("kcal", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.muted)
        }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
fun MacroDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
    }
}
