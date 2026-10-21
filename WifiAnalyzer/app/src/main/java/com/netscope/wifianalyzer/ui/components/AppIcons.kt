package com.netscope.wifianalyzer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Custom brand icons that are not part of the Material set. */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { b.addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
        return b.build()
    }

    /** Premium crown with jewel tips. */
    val Crown: ImageVector by lazy {
        icon(
            "Crown",
            "M3.2,9.2 L7.6,12.6 L12,6.2 L16.4,12.6 L20.8,9.2 L19.1,17.6 L4.9,17.6 Z",
            "M5,19.1 L19,19.1 Q19.8,19.1 19.8,19.9 L19.8,20.3 Q19.8,21.1 19,21.1 L5,21.1 Q4.2,21.1 4.2,20.3 L4.2,19.9 Q4.2,19.1 5,19.1 Z",
            "M1.6,7.9 a1.6,1.6 0 1,0 3.2,0 a1.6,1.6 0 1,0 -3.2,0 Z",
            "M10.3,4.4 a1.7,1.7 0 1,0 3.4,0 a1.7,1.7 0 1,0 -3.4,0 Z",
            "M19.2,7.9 a1.6,1.6 0 1,0 3.2,0 a1.6,1.6 0 1,0 -3.2,0 Z",
        )
    }
}
