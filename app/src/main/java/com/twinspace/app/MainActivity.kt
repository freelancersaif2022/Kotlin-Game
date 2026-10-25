package com.twinspace.app

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.twinspace.app.core.SpaceContract
import com.twinspace.app.ui.TwinSpaceApp
import com.twinspace.app.ui.theme.TwinSpaceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val gatewayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onGatewayResult(it.resultCode, it.data)
    }
    private val provisionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onProvisionResult(it.resultCode)
    }
    private val credentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) vm.onAuthenticated()
    }
    private var authInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep clone names out of the recents preview while the app lock is on.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.settings.collect { s ->
                    if (s.lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        handleIntent(intent)

        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0"
        setContent {
            TwinSpaceTheme {
                TwinSpaceApp(
                    vm = vm,
                    versionName = versionName,
                    onStartForResult = ::startForResult,
                    onAuthenticate = ::authenticate,
                    onOpenSystemSettings = ::openSystemSettings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** A shortcut to a frozen clone sends the user here to unfreeze it. */
    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_UNFREEZE)?.let(vm::promptUnfreeze)
        intent?.removeExtra(EXTRA_UNFREEZE)
    }

    override fun onStart() {
        super.onStart()
        vm.onStart()
    }

    override fun onStop() {
        super.onStop()
        vm.onStop()
    }

    private fun startForResult(intent: Intent, kind: UiEvent.Kind) {
        try {
            when (kind) {
                UiEvent.Kind.GATEWAY -> gatewayLauncher.launch(intent)
                UiEvent.Kind.PROVISION -> provisionLauncher.launch(intent)
            }
        } catch (e: ActivityNotFoundException) {
            fail(
                kind,
                if (kind == UiEvent.Kind.GATEWAY) {
                    "Your space is still starting up. Pause and resume it once from the home screen, then try again."
                } else {
                    "This device doesn't support separate spaces."
                },
            )
        } catch (e: SecurityException) {
            fail(kind, "Android blocked the request.")
        }
    }

    private fun fail(kind: UiEvent.Kind, message: String) = when (kind) {
        UiEvent.Kind.GATEWAY -> vm.onGatewayResult(RESULT_CANCELED, Intent().putExtra(SpaceContract.EXTRA_ERROR, message))
        UiEvent.Kind.PROVISION -> vm.onProvisionResult(RESULT_CANCELED)
    }

    private fun openSystemSettings() {
        val intents = listOf(Intent(Settings.ACTION_SYNC_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
    }

    /** Fingerprint / face with screen-lock fallback on Android 10+, screen lock on Android 9. */
    private fun authenticate() {
        if (authInProgress) return
        val keyguard = getSystemService(KeyguardManager::class.java)!!
        if (!keyguard.isDeviceSecure) {
            // The screen lock was removed; there is nothing to verify against.
            vm.onAuthenticated()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = BiometricPrompt.Builder(this)
                .setTitle("Unlock Twin Space")
                .setSubtitle("Verify it's you to continue")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            authInProgress = true
            builder.build().authenticate(
                CancellationSignal(),
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        authInProgress = false
                        vm.onAuthenticated()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        authInProgress = false
                    }
                },
            )
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Twin Space", "Verify it's you to continue")
            if (intent == null) vm.onAuthenticated() else credentialLauncher.launch(intent)
        }
    }

    companion object {
        const val EXTRA_UNFREEZE = "unfreeze"
    }
}
