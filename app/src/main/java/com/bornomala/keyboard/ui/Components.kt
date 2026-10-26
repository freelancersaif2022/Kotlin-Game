package com.bornomala.keyboard.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bornomala.keyboard.R
import com.bornomala.keyboard.prefs.Prefs

// ---------------------------------------------------------------- preference state

/** A Compose state that mirrors one SharedPreferences value and writes changes back. */
@Composable
fun <T> rememberPref(key: String, default: T): MutableState<T> {
    val prefs = Prefs.get(LocalContext.current)
    val state = remember(key) { mutableStateOf(readPref(prefs, key, default)) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
            if (k == key) state.value = readPref(p, key, default)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(v) {
                    state.value = v
                    writePref(prefs, key, v)
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> readPref(p: SharedPreferences, key: String, default: T): T = when (default) {
    is Boolean -> p.getBoolean(key, default) as T
    is Int -> p.getInt(key, default) as T
    is Long -> p.getLong(key, default) as T
    is String -> (p.getString(key, default) ?: default) as T
    else -> default
}

private fun <T> writePref(p: SharedPreferences, key: String, v: T) {
    val e = p.edit()
    when (v) {
        is Boolean -> e.putBoolean(key, v)
        is Int -> e.putInt(key, v)
        is Long -> e.putLong(key, v)
        is String -> e.putString(key, v)
    }
    e.apply()
}

fun prefs(context: Context): SharedPreferences = Prefs.get(context)

// ---------------------------------------------------------------- layout pieces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_key_arrow_left), contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun NavRow(icon: Int, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            painterResource(R.drawable.ic_key_arrow_right), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, state: MutableState<Boolean>, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = state.value, enabled = enabled, role = Role.Switch) { state.value = it }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = state.value, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun SliderRow(
    title: String,
    state: MutableState<Int>,
    range: IntRange,
    steps: Int = 0,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString() },
) {
    var local by remember(state.value) { mutableFloatStateOf(state.value.toFloat()) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(format(local.toInt()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { state.value = local.toInt() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
            enabled = enabled,
        )
    }
}

@Composable
fun <T> RadioGroup(options: List<Pair<T, String>>, state: MutableState<T>, descriptions: Map<T, String> = emptyMap()) {
    Column {
        for ((value, label) in options) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = state.value == value, role = Role.RadioButton) { state.value = value }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.value == value, onClick = null, modifier = Modifier.padding(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    descriptions[value]?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { content() }
}

val ScreenPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
val RowSpacing = Arrangement.spacedBy(12.dp)
