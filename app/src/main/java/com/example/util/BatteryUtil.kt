package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class AndroidBatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val isLow: Boolean
)

object BatteryUtil {
    fun getBatteryInfo(context: Context): AndroidBatteryInfo {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                100
            }

            AndroidBatteryInfo(
                levelPercent = batteryPct,
                isCharging = isCharging,
                isLow = batteryPct <= 20 && !isCharging
            )
        } catch (e: Exception) {
            AndroidBatteryInfo(levelPercent = 100, isCharging = true, isLow = false)
        }
    }
}
