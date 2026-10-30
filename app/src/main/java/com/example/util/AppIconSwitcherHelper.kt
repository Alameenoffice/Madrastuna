package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Native Android Activity-Alias Dynamic Launcher Icon Switcher.
 * Safely enables the selected occasion launcher alias and disables other aliases
 * using PackageManager.DONT_KILL_APP without interrupting the active session.
 */
object AppIconSwitcherHelper {

    private val OCCASION_ALIASES = listOf(
        ".MainActivityDefault",
        ".MainActivityRamadan",
        ".MainActivityNewYear",
        ".MainActivityHonor",
        ".MainActivityNational"
    )

    fun setAppIcon(context: Context, aliasName: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val normalizedTarget = aliasName.trim().ifBlank { ".MainActivityDefault" }

            // Ensure base MainActivity remains enabled so IDE / Emulator launches never fail
            val mainComp = ComponentName(pkg, "$pkg.MainActivity")
            if (pm.getComponentEnabledSetting(mainComp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            OCCASION_ALIASES.forEach { aliasSuffix ->
                val fullClassName = if (aliasSuffix.startsWith(".")) "$pkg$aliasSuffix" else aliasSuffix
                val comp = ComponentName(pkg, fullClassName)
                val shouldEnable = normalizedTarget != ".MainActivityDefault" && aliasSuffix == normalizedTarget
                val desiredState = if (shouldEnable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                val currentState = pm.getComponentEnabledSetting(comp)
                if (currentState != desiredState) {
                    pm.setComponentEnabledSetting(
                        comp,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore gracefully if device launcher restricts alias updates
        }
    }
}
