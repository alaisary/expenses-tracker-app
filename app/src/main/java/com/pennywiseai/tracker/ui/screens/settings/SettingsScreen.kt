package com.pennywiseai.tracker.ui.screens.settings

import com.pennywiseai.tracker.ui.components.CurrencyText
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.pennywiseai.tracker.R
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.EditProfileSheet
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.amber_light
import com.pennywiseai.tracker.ui.theme.amber_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.indigo_light
import com.pennywiseai.tracker.ui.theme.indigo_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.pink_light
import com.pennywiseai.tracker.ui.theme.pink_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.grey_dark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel
import com.pennywiseai.tracker.utils.AppLocale
import com.pennywiseai.tracker.utils.CurrencyFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToTransactionGroups: () -> Unit = {},
    onNavigateToExchangeRates: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: com.pennywiseai.tracker.ui.viewmodel.AppLockViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val smsScanUseCustomDate by settingsViewModel.smsScanUseCustomDate.collectAsStateWithLifecycle(initialValue = false)
    val smsScanCustomDate by settingsViewModel.smsScanCustomDate.collectAsStateWithLifecycle(initialValue = null)
    val baseCurrency by settingsViewModel.baseCurrency.collectAsStateWithLifecycle(initialValue = "")
    val budgetCycleStartDay by settingsViewModel.budgetCycleStartDay.collectAsStateWithLifecycle(initialValue = 1)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    val exportOptionsPending by settingsViewModel.exportOptionsPending.collectAsStateWithLifecycle()
    val deleteAllTransactionsCount by settingsViewModel.deleteAllTransactionsCount.collectAsStateWithLifecycle()
    val isDeletingAllTransactions by settingsViewModel.isDeletingAllTransactions.collectAsStateWithLifecycle()
    val deleteAllTransactionsResult by settingsViewModel.deleteAllTransactionsResult.collectAsStateWithLifecycle()
    val unifiedCurrencyMode by settingsViewModel.unifiedCurrencyMode.collectAsStateWithLifecycle(initialValue = false)
    val countCreditCardAsExpense by settingsViewModel.countCreditCardAsExpense.collectAsStateWithLifecycle(initialValue = false)
    val displayCurrency by settingsViewModel.displayCurrency.collectAsStateWithLifecycle(initialValue = "")
    val availableCurrencies by settingsViewModel.availableCurrencies.collectAsStateWithLifecycle()
    val accounts by settingsViewModel.accounts.collectAsStateWithLifecycle()
    val mainAccountKey by settingsViewModel.mainAccountKey.collectAsStateWithLifecycle()
    val scheduledFolderBackupEnabled by settingsViewModel.scheduledFolderBackupEnabled.collectAsStateWithLifecycle(initialValue = false)
    val scheduledFolderBackupLastTimestamp by settingsViewModel.scheduledFolderBackupLastTimestamp.collectAsStateWithLifecycle(initialValue = null)
    val requestFolderPicker by settingsViewModel.requestFolderPicker.collectAsStateWithLifecycle()
    var showEditProfile by remember { mutableStateOf(false) }
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showSmsScanDatePicker by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showDisplayCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetCycleDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showMainAccountDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // App language. Backed by SharedPreferences (not DataStore) so
    // Activity.attachBaseContext can read it synchronously on API < 33.
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(AppLocale.getTag(context)) }
    val applyLanguage: (String) -> Unit = { tag ->
        AppLocale.setTag(context, tag)
        currentLanguage = tag
        // API 33+ applies per-app locales through LocaleManager, which
        // recreates the activities itself; below that we recreate manually so
        // the new configuration is picked up.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            (context as? android.app.Activity)?.recreate()
        }
        showLanguageDialog = false
    }
    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importBackup(it)
            }
        }
    )

    // File picker for CSV transaction import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importCsv(it)
            }
        }
    )

    // File saver for export
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.saveBackupToFile(it)
            }
        }
    )

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { settingsViewModel.onBackupFolderSelected(it) }
        }
    )

    LaunchedEffect(requestFolderPicker) {
        if (requestFolderPicker) {
            backupFolderLauncher.launch(null)
            settingsViewModel.onFolderPickerLaunched()
        }
    }

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = scrollBehaviorSmall
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.settings_title),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = { SettingsNavigationContent(onNavigateBack) },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // ── Personalization ──
            SectionHeaderV2(title = stringResource(R.string.settings_personalization_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Person,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.prof_edit_title),
                    subtitle = stringResource(R.string.prof_edit_subtitle),
                    onClick = { showEditProfile = true },
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.Palette,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle),
                    onClick = onNavigateToAppearance,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Language,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_subtitle),
                    onClick = { showLanguageDialog = true },
                    position = ListItemPosition.Bottom,
                    trailingText = languageDisplayName(currentLanguage)
                )
            }

            // ── Currency ──
            SectionHeaderV2(title = stringResource(R.string.settings_currency_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.CurrencyExchange,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_unified_currency_title),
                    subtitle = stringResource(R.string.settings_unified_currency_subtitle),
                    checked = unifiedCurrencyMode,
                    onCheckedChange = { settingsViewModel.setUnifiedCurrencyMode(it) },
                    position = ListItemPosition.Top
                )
                AnimatedVisibility(visible = unifiedCurrencyMode) {
                    SettingsNavItem(
                        icon = Icons.Default.AttachMoney,
                        iconBgColor = teal_light,
                        iconTint = teal_dark,
                        title = stringResource(R.string.settings_display_currency_title),
                        subtitle = stringResource(R.string.settings_display_currency_subtitle),
                        onClick = { showDisplayCurrencyDialog = true },
                        position = ListItemPosition.Middle,
                        trailingText = "${CurrencyFormatter.getCurrencySymbol(displayCurrency)} $displayCurrency"
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_exchange_rates_title),
                    subtitle = stringResource(R.string.settings_exchange_rates_subtitle),
                    onClick = onNavigateToExchangeRates,
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.CreditCard,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_count_card_spend_title),
                    subtitle = stringResource(R.string.settings_count_card_spend_subtitle),
                    checked = countCreditCardAsExpense,
                    onCheckedChange = { settingsViewModel.setCountCreditCardAsExpense(it) },
                    position = ListItemPosition.Middle
                )
                SettingsDropdownItem(
                    icon = Icons.Default.Flag,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_default_currency_title),
                    subtitle = stringResource(R.string.settings_default_currency_subtitle),
                    currentValue = "${CurrencyFormatter.getCurrencySymbol(baseCurrency)} $baseCurrency",
                    expanded = showCurrencyDropdown,
                    onExpandedChange = { showCurrencyDropdown = it },
                    // Last row in the group when there is no main account to show below it.
                    position = if (accounts.isEmpty()) ListItemPosition.Bottom else ListItemPosition.Middle
                ) {
                    availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                CurrencyText("${CurrencyFormatter.getCurrencySymbol(currency)} $currency")
                            },
                            onClick = {
                                settingsViewModel.updateBaseCurrency(currency)
                                showCurrencyDropdown = false
                            },
                            leadingIcon = if (currency == baseCurrency) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }

                // Main account → sets the default currency (unless explicitly chosen above).
                if (accounts.isNotEmpty()) {
                    val mainAccount = accounts.firstOrNull {
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey
                    }
                    SettingsDropdownItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBgColor = purple_light,
                        iconTint = purple_dark,
                        title = stringResource(R.string.settings_main_account_title),
                        subtitle = stringResource(R.string.settings_main_account_subtitle),
                        currentValue = mainAccount?.let { acc ->
                            val name = acc.alias?.takeIf { it.isNotBlank() } ?: acc.bankName
                            AccountBalanceEntity.accountLabel(name, acc.accountLast4)
                        } ?: stringResource(R.string.settings_not_set),
                        expanded = showMainAccountDropdown,
                        onExpandedChange = { showMainAccountDropdown = it },
                        position = ListItemPosition.Bottom
                    ) {
                        accounts.forEach { account ->
                            val name = account.alias?.takeIf { it.isNotBlank() } ?: account.bankName
                            val label = AccountBalanceEntity.accountLabel(name, account.accountLast4)
                            val key = "${account.bankName}_${account.accountLast4}"
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    settingsViewModel.setMainAccount(account)
                                    showMainAccountDropdown = false
                                },
                                leadingIcon = if (key == mainAccountKey) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // ── Budget ──
            // The cycle start day is a budgeting concept, but it changes how
            // Home / Analytics bucket transactions, so it lives up here next
            // to the other "display" knobs rather than buried in Data
            // Management with the budgets list.
            SectionHeaderV2(title = stringResource(R.string.settings_budget_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.DateRange,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_budget_cycle_title),
                    subtitle = stringResource(R.string.settings_budget_cycle_subtitle),
                    onClick = { showBudgetCycleDialog = true },
                    position = ListItemPosition.Single,
                    trailingText = ordinalSuffix(budgetCycleStartDay)
                )
            }

            // ── Security ──
            SectionHeaderV2(title = stringResource(R.string.settings_security_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_app_lock_title),
                    subtitle = if (appLockUiState.canUseBiometric) {
                        stringResource(R.string.settings_app_lock_subtitle)
                    } else {
                        appLockUiState.biometricCapability.getErrorMessage()
                    },
                    checked = appLockUiState.isLockEnabled,
                    onCheckedChange = { appLockViewModel.setAppLockEnabled(it) },
                    enabled = appLockUiState.canUseBiometric,
                    position = if (appLockUiState.isLockEnabled) ListItemPosition.Top else ListItemPosition.Single
                )
                AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.Timer,
                        iconBgColor = pink_light,
                        iconTint = pink_dark,
                        title = stringResource(R.string.settings_lock_timeout_title),
                        subtitle = when (appLockUiState.timeoutMinutes) {
                            0 -> stringResource(R.string.settings_lock_immediately)
                            1 -> stringResource(R.string.settings_lock_after_1_minute)
                            else -> stringResource(R.string.settings_lock_after_minutes, appLockUiState.timeoutMinutes)
                        },
                        onClick = { showTimeoutDialog = true },
                        position = ListItemPosition.Bottom
                    )
                }
            }

            // ── Data Management ──
            SectionHeaderV2(title = stringResource(R.string.settings_data_management_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_manage_accounts_title),
                    subtitle = stringResource(R.string.settings_manage_accounts_subtitle),
                    onClick = onNavigateToManageAccounts,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.Category,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.settings_categories_title),
                    subtitle = stringResource(R.string.settings_categories_subtitle),
                    onClick = onNavigateToCategories,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_smart_rules_title),
                    subtitle = stringResource(R.string.settings_smart_rules_subtitle),
                    onClick = onNavigateToRules,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_budgets_title),
                    subtitle = stringResource(R.string.settings_budgets_subtitle),
                    onClick = onNavigateToBudgets,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = amber_light,
                    iconTint = amber_dark,
                    title = stringResource(R.string.settings_loans_title),
                    subtitle = stringResource(R.string.settings_loans_subtitle),
                    onClick = onNavigateToLoans,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.EventRepeat,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_recurring_title),
                    subtitle = stringResource(R.string.settings_recurring_subtitle),
                    onClick = onNavigateToRecurring,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Folder,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.settings_transaction_groups_title),
                    subtitle = stringResource(R.string.settings_transaction_groups_subtitle),
                    onClick = onNavigateToTransactionGroups,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Upload,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_export_data_title),
                    subtitle = stringResource(R.string.settings_export_data_subtitle),
                    onClick = { settingsViewModel.exportBackup() },
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Backup,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.settings_folder_backup_title),
                    subtitle = if (scheduledFolderBackupEnabled) {
                        stringResource(R.string.settings_folder_backup_daily_subtitle)
                    } else {
                        stringResource(R.string.settings_folder_backup_subtitle)
                    },
                    checked = scheduledFolderBackupEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.setScheduledFolderBackupEnabled(enabled)
                    },
                    position = ListItemPosition.Middle
                )
                if (scheduledFolderBackupEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.SaveAlt,
                        iconBgColor = green_light,
                        iconTint = green_dark,
                        title = stringResource(R.string.settings_backup_now_title),
                        subtitle = scheduledFolderBackupLastTimestamp?.let { timestamp ->
                            val formatted = java.time.Instant.ofEpochMilli(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                            stringResource(R.string.settings_backup_last, formatted)
                        } ?: stringResource(R.string.settings_backup_now_subtitle),
                        onClick = { settingsViewModel.backupToFolderNow() },
                        position = ListItemPosition.Middle
                    )
                    SettingsNavItem(
                        icon = Icons.Default.FolderOpen,
                        iconBgColor = amber_light,
                        iconTint = amber_dark,
                        title = stringResource(R.string.settings_change_backup_folder_title),
                        subtitle = stringResource(R.string.settings_change_backup_folder_subtitle),
                        onClick = { settingsViewModel.requestChangeBackupFolder() },
                        position = ListItemPosition.Middle
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.Download,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.settings_import_data_title),
                    subtitle = stringResource(R.string.settings_import_data_subtitle),
                    onClick = { importLauncher.launch("*/*") },
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Download,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.settings_import_csv_title),
                    subtitle = stringResource(R.string.settings_import_csv_subtitle),
                    onClick = { csvImportLauncher.launch("*/*") },
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Sms,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_unrecognized_sms_title),
                    subtitle = stringResource(R.string.settings_unrecognized_sms_subtitle),
                    onClick = onNavigateToUnrecognizedSms,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.CalendarMonth,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_sms_scan_title),
                    subtitle = when {
                        smsScanAllTime -> stringResource(R.string.settings_sms_scan_all_subtitle)
                        smsScanUseCustomDate -> {
                            val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                            if (formattedDate != null) {
                                stringResource(R.string.settings_sms_scan_from_date, formattedDate)
                            } else {
                                stringResource(R.string.settings_sms_scan_custom_subtitle)
                            }
                        }
                        else -> stringResource(R.string.settings_sms_scan_last_months, smsScanMonths)
                    },
                    onClick = { showSmsScanDialog = true },
                    position = ListItemPosition.Middle,
                    trailingText = when {
                        smsScanAllTime -> stringResource(R.string.settings_sms_scan_all_time)
                        smsScanUseCustomDate -> smsScanCustomDate?.let { formatSmsScanCustomDateShort(it) } ?: stringResource(R.string.settings_sms_scan_custom)
                        else -> stringResource(R.string.settings_sms_scan_months_short, smsScanMonths)
                    }
                )
                SettingsNavItem(
                    icon = Icons.Default.DeleteForever,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_delete_all_title),
                    subtitle = stringResource(R.string.settings_delete_all_subtitle),
                    onClick = { settingsViewModel.requestDeleteAllTransactions() },
                    position = ListItemPosition.Bottom
                )
            }

            // ── Support & Community ──
            SectionHeaderV2(title = stringResource(R.string.settings_support_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconBgColor = pink_light,
                    iconTint = pink_dark,
                    title = stringResource(R.string.settings_help_faq_title),
                    subtitle = stringResource(R.string.settings_help_faq_subtitle),
                    onClick = onNavigateToFaq,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.BugReport,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_report_issue_title),
                    subtitle = stringResource(R.string.settings_report_issue_subtitle),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(com.pennywiseai.tracker.core.Constants.Links.GITHUB_URL + "/issues/new/choose")
                        )
                        context.startActivity(intent)
                    },
                    position = ListItemPosition.Bottom,
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )
            }

            // App Version
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.settings_app_version, com.pennywiseai.tracker.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.settings_author_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    // ── Dialogs ──

    if (showEditProfile) {
        EditProfileSheet(onDismiss = { showEditProfile = false })
    }

    // Display Currency Dialog
    if (showDisplayCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayCurrencyDialog = false },
            title = { Text(stringResource(R.string.settings_display_currency_title)) },
            text = {
                // Scrollable: the full currency list overflows the dialog's max
                // height, so without this the entries below the fold (e.g. MXN)
                // are unreachable. (#615)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    availableCurrencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currency == displayCurrency,
                                    onClick = {
                                        settingsViewModel.setDisplayCurrency(currency)
                                        showDisplayCurrencyDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == displayCurrency,
                                onClick = {
                                    settingsViewModel.setDisplayCurrency(currency)
                                    showDisplayCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            CurrencyText(
                                text = "${CurrencyFormatter.getCurrencySymbol(currency)} $currency",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisplayCurrencyDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    // Budget Cycle Start Day Dialog
    if (showBudgetCycleDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetCycleDialog = false },
            title = { Text(stringResource(R.string.settings_budget_cycle_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.settings_budget_cycle_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    (1..31).forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = day == budgetCycleStartDay,
                                    onClick = {
                                        settingsViewModel.updateBudgetCycleStartDay(day)
                                        showBudgetCycleDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = day == budgetCycleStartDay,
                                onClick = {
                                    settingsViewModel.updateBudgetCycleStartDay(day)
                                    showBudgetCycleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = ordinalSuffix(day),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBudgetCycleDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    // SMS Scan Period Dialog
    if (showSmsScanDialog) {
        AlertDialog(
            onDismissRequest = { showSmsScanDialog = false },
            title = { Text(stringResource(R.string.settings_sms_scan_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_sms_scan_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val options = listOf(-1, -2) + listOf(1, 2, 3, 6, 12, 24)
                    options.forEach { months ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = when (months) {
                                -1 -> smsScanAllTime
                                -2 -> smsScanUseCustomDate && !smsScanAllTime
                                else -> smsScanMonths == months && !smsScanAllTime && !smsScanUseCustomDate
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when (months) {
                                    -1 -> stringResource(R.string.settings_sms_scan_all_time)
                                    -2 -> {
                                        val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                                        if (formattedDate != null) {
                                            stringResource(R.string.settings_sms_scan_custom_date, formattedDate)
                                        } else {
                                            stringResource(R.string.settings_sms_scan_custom_date_none)
                                        }
                                    }
                                    1 -> stringResource(R.string.settings_sms_scan_1_month)
                                    24 -> stringResource(R.string.settings_sms_scan_2_years)
                                    else -> stringResource(R.string.settings_sms_scan_months, months)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmsScanDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    if (showSmsScanDatePicker) {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val initialSelectedDateMillis = smsScanCustomDate
            ?: java.time.LocalDate.now()
                .minusMonths(3)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )

        // Cancelling/dismissing the picker returns to the scan-period dialog rather than
        // silently dropping the user back to Settings (they came here to change the period).
        fun reopenScanDialog() {
            showSmsScanDatePicker = false
            showSmsScanDialog = true
        }

        DatePickerDialog(
            onDismissRequest = { reopenScanDialog() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            settingsViewModel.updateSmsScanCustomDate(millis)
                        }
                        showSmsScanDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.settings_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { reopenScanDialog() }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete-all-transactions confirmation. Irreversible and unbatched, so it
    // asks for the word DELETE rather than a single tap, names the exact number
    // of rows, and points at Export Data first.
    deleteAllTransactionsCount?.let { count ->
        var confirmationText by rememberSaveable(count) { mutableStateOf("") }
        val confirmed = confirmationText.trim().equals("DELETE", ignoreCase = false)

        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAllTransactions) settingsViewModel.cancelDeleteAllTransactions()
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_delete_all_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        if (count == 1) {
                            stringResource(R.string.settings_delete_single_body)
                        } else {
                            stringResource(R.string.settings_delete_many_body, count)
                        }
                    )
                    Text(
                        stringResource(R.string.settings_delete_kept_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        singleLine = true,
                        enabled = !isDeletingAllTransactions,
                        label = { Text(stringResource(R.string.settings_delete_confirm_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.deleteAllTransactions(count) },
                    enabled = confirmed && !isDeletingAllTransactions,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isDeletingAllTransactions) stringResource(R.string.settings_deleting) else stringResource(R.string.settings_delete_all_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settingsViewModel.cancelDeleteAllTransactions() },
                    enabled = !isDeletingAllTransactions
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    deleteAllTransactionsResult?.let { message ->
        AlertDialog(
            onDismissRequest = { settingsViewModel.clearDeleteAllTransactionsResult() },
            title = { Text(stringResource(R.string.settings_transactions_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.clearDeleteAllTransactionsResult() }) {
                    Text(stringResource(R.string.settings_ok))
                }
            }
        )
    }

    // Show import/export message
    importExportMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(5000)
            settingsViewModel.clearImportExportMessage()
        }

        AlertDialog(
            onDismissRequest = { settingsViewModel.clearImportExportMessage() },
            title = { Text(stringResource(R.string.settings_backup_status_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.clearImportExportMessage() }) {
                    Text(stringResource(R.string.settings_ok))
                }
            }
        )
    }

    // Export options dialog
    if (exportOptionsPending && exportedBackupFile != null) {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"

        AlertDialog(
            onDismissRequest = {
                settingsViewModel.dismissExportOptions()
                settingsViewModel.clearImportExportMessage()
            },
            title = { Text(stringResource(R.string.settings_save_backup_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_backup_created))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.settings_save_backup_prompt), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            exportSaveLauncher.launch(fileName)
                            settingsViewModel.dismissExportOptions()
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_save_to_files))
                    }

                    TextButton(
                        onClick = {
                            settingsViewModel.shareBackup()
                            settingsViewModel.dismissExportOptions()
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_share))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.dismissExportOptions()
                        settingsViewModel.clearImportExportMessage()
                    }
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    // Lock Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text(stringResource(R.string.settings_lock_timeout_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_lock_timeout_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val timeoutOptions = listOf(
                        0 to stringResource(R.string.settings_timeout_immediately),
                        1 to stringResource(R.string.settings_timeout_1_minute),
                        5 to stringResource(R.string.settings_timeout_5_minutes),
                        15 to stringResource(R.string.settings_timeout_15_minutes)
                    )

                    timeoutOptions.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text(stringResource(R.string.settings_done))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    AppLocale.SUPPORTED_TAGS.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentLanguage == tag,
                                    role = androidx.compose.ui.semantics.Role.RadioButton,
                                    onClick = { applyLanguage(tag) }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentLanguage == tag, onClick = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(languageDisplayName(tag))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.settings_done))
                }
            }
        )
    }
}

@Composable
private fun languageDisplayName(tag: String): String = when (tag) {
    AppLocale.SYSTEM -> stringResource(R.string.language_system)
    "en" -> stringResource(R.string.language_english)
    "ar" -> stringResource(R.string.language_arabic)
    else -> tag
}

// ── Reusable Settings Components ──
//
// Row chrome — tonal surface, grouped-corner shape, padding, minimum height,
// the tinted icon circle, title/subtitle typography — lives in the shared
// `GroupedList` / `GroupedRow` / `IconTile` / `RowLabels` primitives, so a
// settings row and a grouped row on any other screen are literally the same
// object. These wrappers only add the settings-specific trailing affordance.

@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    GroupedList(content = content)
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: ListItemPosition,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.Default.ChevronRight
) {
    GroupedRow(position = position, onClick = onClick) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(title = title, subtitle = subtitle)
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // The chevron is a hint, not a control — at 20dp it stops competing
        // with the leading icon for attention the way a 24dp one did.
        Icon(
            trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.inline)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    enabled: Boolean = true
) {
    GroupedRow(
        position = position,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) }
    ) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(
            title = title,
            subtitle = subtitle,
            subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    GroupedColumn(position = position) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
            RowLabels(title = title, subtitle = subtitle)
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            TextField(
                value = currentValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_currency_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    // A field nested inside an already-tonal row needs a step
                    // of contrast against it, otherwise the input boundary
                    // disappears.
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                content = dropdownContent
            )
        }
    }
}

@Composable
private fun SettingsNavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = Spacing.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back_cd),
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
        }
    }
}

/**
 * English ordinal suffix for the budget cycle start day — "1st", "2nd", "3rd",
 * "4th"… "11th", "12th", "13th" follow the standard rule that the last two
 * digits decide the suffix (the 11/12/13 teens are always "th").
 */
private fun ordinalSuffix(day: Int): String {
    val safe = day.coerceIn(1, 31)
    val suffix = when {
        safe in 11..13 -> "th"
        safe % 10 == 1 -> "st"
        safe % 10 == 2 -> "nd"
        safe % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$safe$suffix"
}

private fun formatSmsScanCustomDate(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

private fun formatSmsScanCustomDateShort(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
}
