package com.twinspace.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class SpaceAdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        SpaceSetup.finish(context, intent)
    }
}
