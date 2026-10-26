package com.zyropay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyropay.app.R
import com.zyropay.app.ui.theme.Z

@Composable
fun ZTopBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    background: Color = Z.colors.background,
    contentColor: Color = Z.colors.textPrimary,
    centerTitle: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButtonZ(R.drawable.ic_arrow_left, onClick = onBack, tint = contentColor)
            } else {
                Spacer(Modifier.width(8.dp))
            }
            if (!centerTitle) {
                Spacer(Modifier.width(4.dp))
                Text(title, style = Z.type.h3, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (centerTitle) {
            Text(
                title,
                style = Z.type.title.copy(fontSize = 17.sp),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Standard screen layout: top bar, scrollable or custom body, optional pinned bottom area
 * (e.g. primary button) that rides above the keyboard.
 */
@Composable
fun ZScreen(
    title: String = "",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    background: Color = Z.colors.background,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize()) {
            ZTopBar(title, onBack, background = background, actions = actions)
            val body = Modifier.weight(1f).fillMaxWidth()
            if (scrollable) {
                Column(body.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    content()
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                Column(body, content = content)
            }
            if (bottomBar != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(background)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    content = bottomBar,
                )
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        overlay?.invoke(this)
    }
}
