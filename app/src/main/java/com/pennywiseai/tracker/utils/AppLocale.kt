package com.pennywiseai.tracker.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Per-app language selection for the UI.
 *
 * The choice is persisted in a dedicated [android.content.SharedPreferences]
 * file rather than the app's DataStore because [wrap] runs from
 * [android.app.Activity.attachBaseContext], which is synchronous and cannot
 * await a Flow.
 *
 * On Android 13+ the selection is mirrored into the platform
 * [LocaleManager], so it also shows up under Settings > Apps > PennyWise >
 * Language and is applied by the framework (activities are recreated
 * automatically). Below API 33 the framework has no per-app locale, so [wrap]
 * re-configures each Activity's base context instead.
 *
 * The list of supported tags mirrors `res/xml/locales_config.xml`.
 */
object AppLocale {
    /** Sentinel meaning "follow the device language". */
    const val SYSTEM = "system"

    /** Language tags the UI ships translations for, in picker order. */
    val SUPPORTED_TAGS = listOf(SYSTEM, "en", "ar")

    private const val PREFS_NAME = "app_locale"
    private const val KEY_TAG = "language_tag"

    /** Language used until the user picks one. */
    private const val DEFAULT_TAG = "ar"

    /** The saved tag, or [DEFAULT_TAG] when the user has never chosen. */
    fun getTag(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TAG, DEFAULT_TAG)
        return saved?.takeIf { it in SUPPORTED_TAGS } ?: DEFAULT_TAG
    }

    /**
     * Persists [tag] and applies it. On Android 13+ this delegates to the
     * platform, which recreates the app's activities; callers should still
     * call `Activity.recreate()` on older releases.
     */
    fun setTag(context: Context, tag: String) {
        val normalized = if (tag in SUPPORTED_TAGS) tag else SYSTEM
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, normalized)
            .apply()
        applyToPlatform(context, normalized)
    }

    private fun applyToPlatform(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales =
            if (tag == SYSTEM) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(tag)
    }

    /**
     * Applies the saved (or default) language to the platform on Android 13+.
     * Call once from `Application.onCreate` so the framework locale is set even
     * before any Activity exists (notifications, widgets, work). On older
     * releases [wrap] handles it per Activity.
     */
    fun applySaved(context: Context) {
        applyToPlatform(context, getTag(context))
    }

    /**
     * Returns a context configured with the saved language. We apply it on every
     * API level — including Android 13+, where the platform [LocaleManager] also
     * holds the per-app locale. The platform value only takes effect from the
     * *next* process start, so relying on it alone leaves the very first launch
     * after install in the device language; wrapping the base context makes the
     * chosen language (Arabic by default) correct immediately.
     */
    fun wrap(base: Context): Context {
        val tag = getTag(base)
        if (tag == SYSTEM) return base
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(tag)))
        return base.createConfigurationContext(config)
    }
}
