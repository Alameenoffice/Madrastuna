package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale

/**
 * Native Android Activity-Alias Dynamic Launcher Icon Switcher.
 * Safely enables the selected occasion launcher alias and disables all other 4 aliases
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

    /**
     * Resolves a theme key or alias name (e.g., "ramadan", "RAMADAN_EID", ".MainActivityRamadan", "default", "OFFICIAL")
     * to the corresponding <activity-alias> suffix in AndroidManifest.xml.
     */
    fun resolveAliasSuffix(themeKeyOrAlias: String): String {
        val normalized = themeKeyOrAlias.trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("ramadan") -> ".MainActivityRamadan"
            normalized.contains("newyear") || normalized.contains("new_year") -> ".MainActivityNewYear"
            normalized.contains("honor") || normalized.contains("excellence") -> ".MainActivityHonor"
            normalized.contains("national") -> ".MainActivityNational"
            else -> ".MainActivityDefault"
        }
    }

    /**
     * Enables the matching <activity-alias> on the Android Home Screen Launcher
     * and disables all other aliases using PackageManager.DONT_KILL_APP.
     */
    fun changeLauncherIcon(context: Context, themeKey: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val targetAliasSuffix = resolveAliasSuffix(themeKey)

            // Keep underlying MainActivity enabled so direct component intents never fail
            val mainComp = ComponentName(pkg, "$pkg.MainActivity")
            if (pm.getComponentEnabledSetting(mainComp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // First enable the target alias, then disable the remaining aliases so the launcher never sees 0 icons
            val targetComp = ComponentName(pkg, "$pkg$targetAliasSuffix")
            if (pm.getComponentEnabledSetting(targetComp) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            OCCASION_ALIASES.forEach { aliasSuffix ->
                if (aliasSuffix != targetAliasSuffix) {
                    val comp = ComponentName(pkg, "$pkg$aliasSuffix")
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore gracefully if device launcher restricts alias updates
        }
    }

    fun setAppIcon(context: Context, aliasOrThemeKey: String) {
        changeLauncherIcon(context, aliasOrThemeKey)
    }
}
