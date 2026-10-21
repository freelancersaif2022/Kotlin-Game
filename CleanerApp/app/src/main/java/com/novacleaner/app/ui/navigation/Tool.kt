package com.novacleaner.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tool(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val colors: List<Color>,
) {
    JUNK("junk", "Junk Clean", "Cache, temp & residual files", Icons.Rounded.CleaningServices, listOf(Color(0xFF34E3A8), Color(0xFF00A57C))),
    LARGE_FILES("large_files", "Large Files", "Find what eats your space", Icons.Rounded.FolderOpen, listOf(Color(0xFFFFC14D), Color(0xFFFF8A3D))),
    DUPLICATES("duplicates", "Duplicate Photos", "Identical & similar shots", Icons.Rounded.PhotoLibrary, listOf(Color(0xFFB18CFF), Color(0xFF7C4DFF))),
    SCREENSHOTS("screenshots", "Screenshots", "Review old screenshots", Icons.Rounded.Screenshot, listOf(Color(0xFFFF8FB1), Color(0xFFFF4F7B))),
    APPS("apps", "App Manager", "Size, install date, last used", Icons.Rounded.Apps, listOf(Color(0xFF4FE0C5), Color(0xFF00957F))),
    DATA_USAGE("data_usage", "Data Usage", "Mobile & Wi-Fi per app", Icons.Rounded.DataUsage, listOf(Color(0xFFFFB86B), Color(0xFFF2672E))),
    SPEED("speed", "Network Speed", "Live traffic monitor", Icons.Rounded.Speed, listOf(Color(0xFFF07BE0), Color(0xFFB83CC2))),
    BATTERY("battery", "Battery Info", "Health, temperature & more", Icons.Rounded.BatteryChargingFull, listOf(Color(0xFFC6F36B), Color(0xFF5DBB2E))),
    APK("apk_files", "APK Cleaner", "Leftover install packages", Icons.Rounded.Android, listOf(Color(0xFF9BE870), Color(0xFF2EA86B))),
}
