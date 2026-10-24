package com.pdfmaster.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.data.model.PdfFile
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.EmptyState
import com.pdfmaster.app.ui.components.FileRow
import com.pdfmaster.app.ui.files.rememberFileActions
import com.pdfmaster.app.ui.navigation.LocalNavigator

@Composable
fun SearchScreen() {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val recent by deps.files.recent.collectAsStateWithLifecycle(emptyList())
    val created by deps.files.created.collectAsStateWithLifecycle(emptyList())
    var device by remember { mutableStateOf(emptyList<PdfFile>()) }
    val actions = rememberFileActions()
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        device = deps.files.scanDevice()
    }
    val all = remember(recent, created, device) { (recent + created + device).distinctBy { it.key } }
    val results = remember(query, all) {
        if (query.isBlank()) emptyList() else all.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by file name") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
        }
        val list = if (query.isBlank()) recent.take(10) else results
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (query.isBlank() && list.isNotEmpty()) {
                item { Text("Recent", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (query.isNotBlank() && list.isEmpty()) {
                item {
                    EmptyState(R.drawable.img_empty_search, "No matches", "We couldn't find a PDF named \"${query.trim()}\". Try a different name.", modifier = Modifier.padding(top = 32.dp))
                }
            }
            items(list, key = { it.key }) { file ->
                FileRow(
                    file,
                    onClick = { keyboard?.hide(); nav.viewer(file.uri) },
                    onMore = { actions.open(file) },
                    highlight = query.trim(),
                    modifier = Modifier.animateItem(),
                )
            }
            item { Column(Modifier.fillMaxWidth().navigationBarsPadding()) {} }
        }
    }
}
