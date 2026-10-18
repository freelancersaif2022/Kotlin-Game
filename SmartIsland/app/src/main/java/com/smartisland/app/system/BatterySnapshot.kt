package com.smartisland.app.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatterySnapshot(
    val level: Int,
    val isCharging: Boolean,
    val plugged: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    val currentMa: Int,
    val health: Int,
) {
    val watts: Float get() = if (isCharging) voltageMv / 1000f * currentMa / 1000f else 0f

    /** 15 W and above is what most vendors label as fast charging. */
    val isFast: Boolean get() = isCharging && (currentMa >= 1500 || watts >= 15f)

    val source: String
        get() = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

    val healthLabel: String
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            else -> "Normal"
        }

    companion object {
        fun from(context: Context, intent: Intent? = null): BatterySnapshot {
            val i = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val bm = context.systemService<BatteryManager>()
            val level = i?.let {
                val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0 && s > 0) l * 100 / s else null
            } ?: bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
            // Vendors report CURRENT_NOW in either µA or mA and with either sign.
            val raw = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
            val currentMa = if (raw == Int.MIN_VALUE || raw > 100_000_000) 0 else if (raw > 20_000) raw / 1000 else raw
            return BatterySnapshot(
                level = level.coerceIn(0, 100),
                isCharging = charging,
                plugged = plugged,
                temperatureC = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
                voltageMv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0,
                currentMa = currentMa,
                health = i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0,
            )
        }
    }
}
