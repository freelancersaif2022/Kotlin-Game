package com.smartisland.app.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.overlay.IslandAccessibilityService

enum class ActionResult { DONE, NEEDS_ACCESSIBILITY, UNSUPPORTED, FAILED }

/** Software replacements for the hardware power key, routed through the accessibility service. */
object SystemActions {

    fun powerMenu(): ActionResult = global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

    fun lockScreen(): ActionResult =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        else ActionResult.UNSUPPORTED

    fun screenshot(): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return ActionResult.UNSUPPORTED
        val result = global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        if (result == ActionResult.DONE) {
            // Confirm on the island once the capture is done, so the alert isn't in the picture.
            main.postDelayed({ IslandEngine.showTransient(IslandActivity.Info("Screenshot", "Saved"), 1800L) }, 1100L)
        }
        return result
    }

    private val main = Handler(Looper.getMainLooper())

    fun notifications(): ActionResult = global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    fun quickSettings(): ActionResult = global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    fun recents(): ActionResult = global(AccessibilityService.GLOBAL_ACTION_RECENTS)

    private fun global(action: Int): ActionResult {
        val service = IslandAccessibilityService.instance ?: return ActionResult.NEEDS_ACCESSIBILITY
        return if (service.performGlobalAction(action)) ActionResult.DONE else ActionResult.FAILED
    }

    // Flashlight ------------------------------------------------------------------------------

    @Volatile
    var torchOn: Boolean = false

    fun setTorch(context: Context, on: Boolean): Boolean {
        val cm = context.systemService<CameraManager>()
        val id = torchCameraId(cm) ?: return false
        return runCatching { cm.setTorchMode(id, on) }.onSuccess { torchOn = on }.isSuccess
    }

    fun torchCameraId(cm: CameraManager): String? = runCatching {
        cm.cameraIdList.firstOrNull { id ->
            val c = cm.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()
}
