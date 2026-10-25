package com.twinspace.app.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Runs inside the space whenever it starts (boot, or the user resuming it) and after app updates,
 * and re-applies the space's setup so the personal side can always reach it.
 */
class SpaceRepairReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SpaceSetup.finish(context, null)
    }
}
