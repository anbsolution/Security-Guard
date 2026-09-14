package com.securityguard.app.core.settings

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("security_guard_prefs", Context.MODE_PRIVATE)
    var setupComplete: Boolean
        get() = prefs.getBoolean("setup_complete", false)
        set(value) = prefs.edit().putBoolean("setup_complete", value).apply()
    var onboardingStep: Int
        get() = prefs.getInt("onboarding_step", 0)
        set(value) = prefs.edit().putInt("onboarding_step", value).apply()
    var appLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", true)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()
    var autoLockMinutes: Int
        get() = prefs.getInt("auto_lock_minutes", 0)
        set(value) = prefs.edit().putInt("auto_lock_minutes", value).apply()
}
