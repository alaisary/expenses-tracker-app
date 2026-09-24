package com.pennywiseai.tracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.repository.AppLockRepository
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PennyWiseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLockRepository: AppLockRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var categoryRepository: com.pennywiseai.tracker.data.repository.CategoryRepository

    @Inject
    lateinit var scheduledFolderBackupScheduler: com.pennywiseai.tracker.backup.folder.ScheduledFolderBackupScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activityReferences = 0
    private var isInForeground = false

    /**
     * Publicly accessible flag to check if the app is in the foreground.
     * Used by SmsBroadcastReceiver to determine whether to show notifications.
     */
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private var lastLocales: android.os.LocaleList? = null

    // Widgets bake translated text into their stored snapshots, so rebuild them
    // when the device or per-app language changes — otherwise they keep the
    // language they were last refreshed in until some other event refreshes them.
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val locales = newConfig.locales
        if (lastLocales != null && locales != lastLocales) {
            com.pennywiseai.tracker.widget.WidgetRefresher.refreshTransactionWidgets(this)
        }
        lastLocales = locales
    }

    override fun onCreate() {
        super.onCreate()
        // Apply the app language (Arabic by default) to the platform before any
        // UI exists, so Android 13+ applies it to the app framework-wide.
        com.pennywiseai.tracker.utils.AppLocale.applySaved(this)
        registerActivityLifecycleCallbacks(AppLockLifecycleObserver())

        // Re-arm the daily folder backup on process start when the user has it enabled,
        // so scheduled work survives reboots / app updates that clear WorkManager state.
        applicationScope.launch {
            if (userPreferencesRepository.isScheduledFolderBackupEnabled()) {
                scheduledFolderBackupScheduler.schedule()
            }
        }

        // Materialise recurring / scheduled manual (cash) transactions (#706):
        // a daily periodic run plus a one-shot catch-up now, so a device that was
        // off still creates anything that came due while it was down.
        com.pennywiseai.tracker.worker.RecurringTransactionWorker.enqueuePeriodic(this)
        com.pennywiseai.tracker.worker.RecurringTransactionWorker.enqueueOneShotCatchUp(this)

        // Backfill built-in categories added since the user's install (idempotent;
        // never overwrites or renames existing rows).
        applicationScope.launch {
            runCatching { categoryRepository.ensureDefaultCategories() }
        }

        // Mirror user category styles (color + emoji, #760) into CategoryMapping so
        // icon call sites outside ViewModels can render custom categories.
        applicationScope.launch {
            categoryRepository.getAllCategories().collectLatest { categories ->
                val styles = categories.associate { it.name to CategoryMapping.UserStyle(it.color, it.icon) }
                CategoryMapping.userStyles.keys.retainAll(styles.keys)
                CategoryMapping.userStyles.putAll(styles)
            }
        }
    }

    /**
     * Lifecycle observer to track app foreground/background state
     * This is used to trigger app lock when app returns from background
     */
    private inner class AppLockLifecycleObserver : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            activityReferences++
            if (!isInForeground) {
                // App came to foreground
                isInForeground = true
                isAppInForeground = true
                // Check if app should be locked when returning from background
                checkAndLockApp()
            }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            activityReferences--
            if (activityReferences == 0) {
                // App went to background
                isInForeground = false
                isAppInForeground = false
                // Note: We don't need to do anything here
                // The lock state will be checked when app returns to foreground
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}

        private fun checkAndLockApp() {
            applicationScope.launch {
                // The AppLockRepository will determine if app should be locked
                // based on timeout settings
                // The lock state will be observed by the AppLockViewModel
            }
        }
    }
}