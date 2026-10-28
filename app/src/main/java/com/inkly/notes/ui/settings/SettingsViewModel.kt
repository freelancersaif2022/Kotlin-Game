package com.inkly.notes.ui.settings

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.inkly.notes.appContainer
import com.inkly.notes.data.ThemeMode
import com.inkly.notes.drive.BackupScheduler
import com.inkly.notes.drive.DriveApi
import com.inkly.notes.drive.DriveAuth
import com.inkly.notes.drive.DriveException
import com.inkly.notes.drive.DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class Busy { Connecting, BackingUp, Checking, Restoring, Disconnecting }

sealed interface SettingsEvent {
    data class Resolve(val intentSender: IntentSender) : SettingsEvent
    data class Message(val text: String) : SettingsEvent
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer
    val prefs = container.prefs

    var busy by mutableStateOf<Busy?>(null); private set
    /** A backup awaiting the user's confirmation before restoring. */
    var restoreOffer by mutableStateOf<DriveFile?>(null); private set
    var offerAfterConnect by mutableStateOf(false); private set

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private enum class Op { CONNECT, BACKUP, RESTORE }
    private var pending: Op? = null
    private var token: String? = null

    fun setTheme(mode: ThemeMode) = prefs.setTheme(mode)

    fun setAutoBackup(enabled: Boolean) {
        prefs.setAutoBackup(enabled)
        val ctx = getApplication<Application>()
        if (enabled && prefs.state.value.driveConnected) BackupScheduler.schedule(ctx) else BackupScheduler.cancel(ctx)
        message(if (enabled) "Daily backup turned on" else "Daily backup turned off")
    }

    fun connect() = start(Op.CONNECT)
    fun backupNow() = start(Op.BACKUP)
    fun restore() = start(Op.RESTORE)

    private fun start(op: Op) {
        if (busy != null) return
        viewModelScope.launch {
            busy = if (op == Op.CONNECT) Busy.Connecting else Busy.Checking
            try {
                val result = DriveAuth.authorize(getApplication())
                if (result.hasResolution()) {
                    pending = op
                    busy = null
                    val pi = result.pendingIntent ?: error("Google sign-in is unavailable")
                    _events.send(SettingsEvent.Resolve(pi.intentSender))
                } else {
                    proceed(op, result)
                }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun onAuthResult(resultCode: Int, data: Intent?) {
        val op = pending ?: return
        pending = null
        if (resultCode != Activity.RESULT_OK) {
            message("Google sign-in was cancelled")
            return
        }
        viewModelScope.launch {
            busy = if (op == Op.CONNECT) Busy.Connecting else Busy.Checking
            try {
                proceed(op, DriveAuth.resultFromIntent(getApplication(), data))
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private suspend fun proceed(op: Op, result: AuthorizationResult) {
        val t = result.accessToken ?: error("Google didn't return an access token")
        token = t
        when (op) {
            Op.CONNECT -> {
                busy = Busy.Connecting
                val (name, email) = withContext(Dispatchers.IO) { DriveApi(t).aboutUser() }
                prefs.setConnected(name, email)
                if (prefs.state.value.autoBackup) BackupScheduler.schedule(getApplication())
                val latest = container.backup.latestBackup(t)
                busy = null
                if (latest != null) {
                    offerAfterConnect = true
                    restoreOffer = latest
                } else {
                    message("Connected to Google Drive")
                    busy = Busy.BackingUp
                    container.backup.backup(t)
                    busy = null
                    message("Connected • first backup complete")
                }
            }
            Op.BACKUP -> {
                busy = Busy.BackingUp
                val file = container.backup.backup(t)
                busy = null
                message("Backup complete • ${file.noteCount} ${if (file.noteCount == 1) "note" else "notes"} saved")
            }
            Op.RESTORE -> {
                busy = Busy.Checking
                val latest = container.backup.latestBackup(t)
                busy = null
                if (latest == null) message("No backup found in Google Drive yet")
                else {
                    offerAfterConnect = false
                    restoreOffer = latest
                }
            }
        }
    }

    fun confirmRestore() {
        val file = restoreOffer ?: return
        val t = token ?: return
        restoreOffer = null
        viewModelScope.launch {
            busy = Busy.Restoring
            try {
                val r = container.backup.restore(t, file)
                busy = null
                message(
                    when {
                        r.added + r.updated == 0 -> "Everything is already up to date"
                        else -> "Restored • ${r.added} added, ${r.updated} updated"
                    }
                )
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun dismissRestore() {
        restoreOffer = null
        if (offerAfterConnect) message("Connected to Google Drive")
    }

    fun disconnect() {
        viewModelScope.launch {
            busy = Busy.Disconnecting
            val ctx = getApplication<Application>()
            BackupScheduler.cancel(ctx)
            runCatching { DriveAuth.disconnect(ctx) }
            token = null
            prefs.setDisconnected()
            busy = null
            message("Google Drive disconnected")
        }
    }

    private fun fail(e: Throwable) {
        busy = null
        if (e is DriveException && e.code == 401) prefs.setNeedsReauth(true)
        message(friendly(e))
    }

    private fun message(text: String) {
        viewModelScope.launch { _events.send(SettingsEvent.Message(text)) }
    }

    private fun friendly(e: Throwable): String = when (e) {
        is ApiException -> when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Google sign-in isn't set up for this build yet (OAuth client / SHA-1). See README."
            CommonStatusCodes.NETWORK_ERROR -> "No internet connection. Please try again."
            CommonStatusCodes.CANCELED -> "Google sign-in was cancelled"
            else -> "Google sign-in failed (code ${e.statusCode})"
        }
        is DriveException -> when (e.code) {
            401 -> "Google session expired. Please reconnect Google Drive."
            403 -> "Google Drive refused access: ${e.message}"
            else -> e.message ?: "Google Drive error"
        }
        is UnknownHostException, is ConnectException, is SocketTimeoutException ->
            "No internet connection. Check your network and try again."
        is IOException -> e.message ?: "Network error"
        else -> e.message ?: "Something went wrong"
    }
}
