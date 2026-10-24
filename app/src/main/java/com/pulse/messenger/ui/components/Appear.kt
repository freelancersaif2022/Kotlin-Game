package com.pulse.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Scope-free [AnimatedVisibility]. Needed inside a Box that is itself nested in a Row/Column,
 * where the scoped overloads would otherwise be picked up from the outer receiver.
 */
@Composable
fun Appear(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) = AnimatedVisibility(visible = visible, modifier = modifier, enter = enter, exit = exit, content = content)
