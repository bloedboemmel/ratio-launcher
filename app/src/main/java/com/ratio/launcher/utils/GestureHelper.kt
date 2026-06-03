package com.ratio.launcher.utils

import android.accessibilityservice.AccessibilityService
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object GestureHelper {

    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, RatioDeviceAdmin::class.java)

        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        } else {
            requestDeviceAdmin(context)
        }
    }

    fun requestDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(context, RatioDeviceAdmin::class.java)
            )
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for double-tap to lock screen")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isDeviceAdminEnabled(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, RatioDeviceAdmin::class.java))
    }

    fun expandNotificationShade(context: Context) {
        try {
            @Suppress("WrongConstant")
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(service)
        } catch (_: Exception) {
        }
    }
}

class RatioDeviceAdmin : DeviceAdminReceiver()
