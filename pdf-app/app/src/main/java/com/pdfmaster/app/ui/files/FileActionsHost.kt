package com.pdfmaster.app.ui.files

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pdfmaster.app.R
import com.pdfmaster.app.data.model.PdfFile
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.AppDialog
import com.pdfmaster.app.ui.components.FileActionsSheet
import com.pdfmaster.app.ui.components.FileSheetCallbacks
import com.pdfmaster.app.ui.components.InputDialog
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.util.FileActions
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.launch

@Stable
class FileActionsController internal constructor(
    private val show: (PdfFile) -> Unit,
    private val favorite: (PdfFile) -> Unit,
) {
    fun open(file: PdfFile) = show(file)
    fun toggleFavorite(file: PdfFile) = favorite(file)
}

/**
 * Hosts the file bottom sheet plus the rename / delete dialogs it can open.
 * [onChanged] lets lists that aren't backed by the database refresh themselves.
 */
@Composable
fun rememberFileActions(onChanged: () -> Unit = {}): FileActionsController {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sheetFile by remember { mutableStateOf<PdfFile?>(null) }
    var renameFile by remember { mutableStateOf<PdfFile?>(null) }
    var deleteFile by remember { mutableStateOf<PdfFile?>(null) }

    val toggleFavorite: (PdfFile) -> Unit = { file ->
        scope.launch {
            deps.files.setFavorite(file, !file.isFavorite)
            onChanged()
            snackbar.success(if (file.isFavorite) "Removed from favourites" else "Added to favourites")
        }
    }

    sheetFile?.let { file ->
        val owned = file.isCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        FileActionsSheet(
            file = file,
            onDismiss = { sheetFile = null },
            callbacks = FileSheetCallbacks(
                onOpen = { sheetFile = null; nav.viewer(file.uri) },
                onShare = { sheetFile = null; runCatching { FileActions.share(context, listOf(file.uri)) } },
                onFavorite = { sheetFile = null; toggleFavorite(file) },
                onPrint = { sheetFile = null; runCatching { FileActions.print(context, file.uri, file.name) } },
                onTool = { tool -> sheetFile = null; nav.tool(tool, file.uri) },
                onRename = if (owned) ({ sheetFile = null; renameFile = file }) else null,
                onRemoveRecent = if (file.date > 0 && !file.isCreated) ({
                    sheetFile = null
                    scope.launch {
                        deps.files.removeFromRecent(file)
                        onChanged()
                        snackbar.info("Removed from recent files")
                    }
                }) else null,
                onDelete = if (owned) ({ sheetFile = null; deleteFile = file }) else null,
            ),
        )
    }

    renameFile?.let { file ->
        InputDialog(
            title = "Rename file",
            label = "File name",
            initial = Format.baseName(file.name),
            confirmText = "Rename",
            onDismiss = { renameFile = null },
            onConfirm = { name ->
                renameFile = null
                scope.launch {
                    if (deps.files.rename(file, name)) {
                        onChanged()
                        snackbar.success("File renamed")
                    } else {
                        snackbar.error("This file can't be renamed.")
                    }
                }
            },
        )
    }

    deleteFile?.let { file ->
        AppDialog(
            title = "Delete this file?",
            message = "\"${file.name}\" will be permanently deleted from your device.",
            confirmText = "Delete",
            destructive = true,
            image = R.drawable.img_error_generic,
            onDismiss = { deleteFile = null },
            onConfirm = {
                deleteFile = null
                scope.launch {
                    if (deps.files.delete(file)) {
                        deps.thumbnails.invalidate(file.uri)
                        onChanged()
                        snackbar.success("File deleted")
                    } else {
                        snackbar.error("This file couldn't be deleted.")
                    }
                }
            },
        )
    }

    return remember { FileActionsController({ sheetFile = it }, toggleFavorite) }
}
