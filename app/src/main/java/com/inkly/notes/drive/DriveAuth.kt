package com.inkly.notes.drive

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Google authorization for the Drive "appDataFolder" (a private, hidden folder only Inkly can see).
 * Uses the Google Identity AuthorizationClient: the first call shows Google's consent UI, later calls
 * (including from background workers) return a fresh access token silently.
 */
object DriveAuth {
    const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    private fun request(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE_APPDATA)))
        .build()

    suspend fun authorize(context: Context): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(request()).await()

    fun resultFromIntent(context: Context, data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    /** Returns an access token without UI, or null when the user must grant access again. */
    suspend fun silentToken(context: Context): String? {
        val result = authorize(context)
        return if (result.hasResolution()) null else result.accessToken
    }

    /**
     * Runs [block] with a token. If Drive rejects the token as expired (401) the cached token is
     * cleared, a new one is fetched silently and the call is retried once.
     */
    suspend fun <T> withToken(context: Context, token: String, block: suspend (String) -> T): T =
        try {
            block(token)
        } catch (e: DriveException) {
            if (e.code != 401) throw e
            withContext(Dispatchers.IO) { runCatching { GoogleAuthUtil.clearToken(context, token) } }
            val fresh = silentToken(context) ?: throw e
            block(fresh)
        }

    /** Revokes Drive access and clears the remembered Google account. */
    suspend fun disconnect(context: Context) {
        val token = runCatching { silentToken(context) }.getOrNull()
        withContext(Dispatchers.IO) {
            if (token != null) {
                runCatching { GoogleAuthUtil.clearToken(context, token) }
                runCatching { revoke(token) }
            }
        }
        @Suppress("DEPRECATION")
        runCatching { GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().await() }
    }

    /** SHA-1 of the certificate this APK is signed with – what Google Cloud's Android OAuth client must list. */
    @Suppress("DEPRECATION")
    fun signingSha1(context: Context): String = runCatching {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val cert = signatures?.firstOrNull() ?: return@runCatching "Unavailable"
        MessageDigest.getInstance("SHA-1").digest(cert.toByteArray())
            .joinToString(":") { "%02X".format(it) }
    }.getOrDefault("Unavailable")

    private fun revoke(token: String) {
        val body = "token=" + URLEncoder.encode(token, "UTF-8")
        val conn = URL("https://oauth2.googleapis.com/revoke").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
