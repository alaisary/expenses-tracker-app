package com.pennywiseai.tracker.ui.screens.rules

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.domain.model.rule.*
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.FinancialAccountIdentity
import com.pennywiseai.tracker.ui.components.SegmentedPillRow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.viewmodel.RulesViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.UUID

/**
 * Transaction-type options for rule condition/action pickers: stored enum name → display label
 * resource. Labels mirror the [com.pennywiseai.tracker.data.database.entity.TransactionType]
 * names (e.g. EXPENSE → "Expense") so what the user picks matches what the rule stores and applies.
 */
internal val RULE_TRANSACTION_TYPE_OPTIONS: List<Pair<String, Int>> = listOf(
    "INCOME" to R.string.filter_type_income,
    "EXPENSE" to R.string.filter_type_expense,
    "CREDIT" to R.string.filter_type_credit,
    "TRANSFER" to R.string.filter_type_transfer,
    "INVESTMENT" to R.string.filter_type_investment
)

/** User-facing label for a stored transaction-type value, falling back to the raw value. */
internal fun ruleTransactionTypeLabel(value: String, context: Context): String =
    RULE_TRANSACTION_TYPE_OPTIONS.firstOrNull { it.first.equals(value, ignoreCase = true) }
        ?.let { context.getString(it.second) }
        ?: value

/** The operator a field should reset to when it becomes the condition's field. */
private fun TransactionField.defaultConditionOperator(): ConditionOperator = when (this) {
    TransactionField.AMOUNT -> ConditionOperator.LESS_THAN
    TransactionField.TRANSACTION_TIME -> ConditionOperator.LESS_THAN
    TransactionField.TRANSACTION_HOUR -> ConditionOperator.EQUALS
    TransactionField.TRANSACTION_DAY_OF_WEEK -> ConditionOperator.EQUALS
    TransactionField.TRANSACTION_DAY_OF_MONTH -> ConditionOperator.EQUALS
    TransactionField.TRANSACTION_DATE -> ConditionOperator.EQUALS
    TransactionField.ACCOUNT -> ConditionOperator.EQUALS
    TransactionField.TYPE -> ConditionOperator.EQUALS
    else -> ConditionOperator.CONTAINS
}

/**
 * Operators offered for a condition field, paired with their display label.
 *
 * The set and its order come from [supportedOperators] so the picker and the
 * rule importer can't drift apart; only the wording lives here, since the same
 * operator reads differently per field ("<" for an amount, "before" for a time).
 */
private fun conditionOperatorsForField(
    field: TransactionField
): List<Pair<ConditionOperator, Int>> =
    supportedOperators(field).map { operator -> operator to operator.labelResFor(field) }

@StringRes
private fun ConditionOperator.labelResFor(field: TransactionField): Int = when (field) {
    TransactionField.AMOUNT -> when (this) {
        ConditionOperator.LESS_THAN -> R.string.filter_condition_less_than
        ConditionOperator.GREATER_THAN -> R.string.filter_condition_greater_than
        else -> R.string.filter_condition_equals
    }
    TransactionField.TRANSACTION_TIME -> when (this) {
        ConditionOperator.LESS_THAN -> R.string.filter_condition_before
        ConditionOperator.GREATER_THAN -> R.string.filter_condition_after
        ConditionOperator.GREATER_THAN_OR_EQUAL -> R.string.filter_condition_at_or_after
        ConditionOperator.LESS_THAN_OR_EQUAL -> R.string.filter_condition_at_or_before
        else -> R.string.filter_condition_exactly_at
    }
    TransactionField.TRANSACTION_HOUR,
    TransactionField.TRANSACTION_DAY_OF_MONTH,
    TransactionField.TRANSACTION_DATE -> when (this) {
        ConditionOperator.LESS_THAN -> R.string.filter_condition_before
        ConditionOperator.GREATER_THAN -> R.string.filter_condition_after
        ConditionOperator.IN -> R.string.filter_condition_is_any_of
        else -> R.string.filter_condition_is
    }
    TransactionField.TYPE,
    TransactionField.TRANSACTION_DAY_OF_WEEK,
    TransactionField.ACCOUNT -> when (this) {
        ConditionOperator.NOT_EQUALS -> R.string.filter_condition_is_not
        ConditionOperator.IN -> R.string.filter_condition_is_any_of
        else -> R.string.filter_condition_is
    }
    else -> when (this) {
        ConditionOperator.EQUALS -> R.string.filter_condition_equals_word
        ConditionOperator.STARTS_WITH -> R.string.filter_condition_starts_with
        else -> R.string.filter_condition_contains
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    onNavigateBack: () -> Unit,
    onSaveRule: (TransactionRule) -> Unit,
    existingRule: TransactionRule? = null,
    // True only when editing a saved rule. A duplicate passes a prefilled [existingRule]
    // (carrying a fresh id) but isEditing = false, so it saves as a brand-new rule.
    // Defaults to false: a non-null prefill must NOT imply edit, or a duplicate that
    // omits this flag would overwrite its source. Callers state edit intent explicitly.
    isEditing: Boolean = false,
    allAccounts: List<RulesViewModel.AccountInfo> = emptyList()
) {
    var ruleName by remember(existingRule) { mutableStateOf(existingRule?.name ?: "") }
    var description by remember(existingRule) { mutableStateOf(existingRule?.description ?: "") }

    // Initialize conditions list from existing rule or use single default condition
    var conditions by remember(existingRule) {
        mutableStateOf(existingRule?.conditions?.toMutableList() ?: mutableListOf(
            RuleCondition(
                field = TransactionField.AMOUNT,
                operator = ConditionOperator.LESS_THAN,
                value = ""
            )
        ))
    }

    // Initialize actions list from existing rule or use a single default action
    var actions by remember(existingRule) {
        mutableStateOf(
            existingRule?.actions?.takeIf { it.isNotEmpty() }
                ?: listOf(
                    RuleAction(
                        field = TransactionField.CATEGORY,
                        actionType = ActionType.SET,
                        value = ""
                    )
                )
        )
    }

    // Holds a pending switch-to-BLOCK while we confirm discarding the other actions.
    var pendingBlockAction by remember { mutableStateOf<RuleAction?>(null) }

    // Preset names resolved here — the preset lambdas below are not @Composable,
    // so stringResource cannot be called from inside them.
    val presetNameBlockOtp = stringResource(R.string.rule_preset_name_block_otp)
    val presetNameBlockSmall = stringResource(R.string.rule_preset_name_block_small)
    val presetNameSmallFood = stringResource(R.string.rule_preset_name_small_food)
    val presetNameStandardizeMerchant = stringResource(R.string.rule_preset_name_standardize_merchant)
    val presetNameMarkIncome = stringResource(R.string.rule_preset_name_mark_income)
    val presetNameDailyInvestment = stringResource(R.string.rule_preset_daily_investment)

    // Common presets for quick setup
    val commonPresets = listOf(
        stringResource(R.string.rule_preset_block_otps) to {
            ruleName = presetNameBlockOtp
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.CONTAINS,
                    value = "OTP"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.BLOCK,
                    value = ""
                )
            )
        },
        stringResource(R.string.rule_preset_block_small_amounts) to {
            ruleName = presetNameBlockSmall
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.AMOUNT,
                    operator = ConditionOperator.LESS_THAN,
                    value = "10"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.BLOCK,
                    value = ""
                )
            )
        },
        stringResource(R.string.rule_preset_small_amounts_food) to {
            ruleName = presetNameSmallFood
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.AMOUNT,
                    operator = ConditionOperator.LESS_THAN,
                    value = "200"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Food & Dining"
                )
            )
        },
        stringResource(R.string.rule_preset_standardize_merchant) to {
            ruleName = presetNameStandardizeMerchant
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.MERCHANT,
                    operator = ConditionOperator.CONTAINS,
                    value = "AMZN"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.MERCHANT,
                    actionType = ActionType.SET,
                    value = "Amazon"
                )
            )
        },
        stringResource(R.string.rule_preset_mark_as_income) to {
            ruleName = presetNameMarkIncome
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.CONTAINS,
                    value = "credited"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.TYPE,
                    actionType = ActionType.SET,
                    value = "INCOME"
                )
            )
        },
        stringResource(R.string.rule_preset_daily_investment) to {
            ruleName = presetNameDailyInvestment
            conditions = mutableListOf(
                RuleCondition(
                    field = TransactionField.TRANSACTION_TIME,
                    operator = ConditionOperator.GREATER_THAN_OR_EQUAL,
                    value = "09:00"
                ),
                RuleCondition(
                    field = TransactionField.TRANSACTION_TIME,
                    operator = ConditionOperator.LESS_THAN,
                    value = "09:30"
                )
            )
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Investments"
                )
            )
        }
    )

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = if (isEditing) stringResource(R.string.rule_edit_rule_title) else stringResource(R.string.rule_create_rule_title),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.rule_close))
                    }
                },
                actionContent = {
                    TextButton(
                        onClick = {
                            // Validate: rule name + all conditions have values + all actions are valid
                            val areConditionsValid = conditions.isNotEmpty() &&
                                conditions.all { it.validate() }
                            val isActionValid = actions.isNotEmpty() && actions.all { it.validate() }
                            val isValid = ruleName.isNotBlank() && areConditionsValid && isActionValid

                            if (isValid) {
                                val rule = TransactionRule(
                                    id = existingRule?.id ?: UUID.randomUUID().toString(),
                                    name = ruleName,
                                    description = description.takeIf { it.isNotBlank() },
                                    priority = existingRule?.priority ?: 100,
                                    conditions = conditions.toList(),
                                    actions = actions,
                                    isActive = existingRule?.isActive ?: true,
                                    isSystemTemplate = existingRule?.isSystemTemplate ?: false,
                                    createdAt = existingRule?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSaveRule(rule)
                            }
                        },
                        enabled = ruleName.isNotBlank() &&
                                 conditions.isNotEmpty() &&
                                 conditions.all { it.validate() } &&
                                 actions.isNotEmpty() &&
                                 actions.all { it.validate() }
                    ) {
                        Text(stringResource(R.string.rule_save))
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(Dimensions.Padding.content)
                .imePadding()
                .overScrollVertical()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Quick presets
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.rule_quick_templates),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonPresets.forEach { (label, action) ->
                            ElevatedAssistChip(
                                onClick = action,
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Rule name and description
            TextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text(stringResource(R.string.rule_rule_name)) },
                placeholder = { Text(stringResource(R.string.rule_rule_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.rule_description_optional)) },
                placeholder = { Text(stringResource(R.string.rule_description_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // Conditions section (supports multiple)
            Card {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.rule_when),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = {
                                conditions = (conditions + RuleCondition(
                                    field = TransactionField.AMOUNT,
                                    operator = ConditionOperator.LESS_THAN,
                                    value = ""
                                )).toMutableList()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(stringResource(R.string.rule_add_condition))
                        }
                    }

                    // Display all conditions
                    conditions.forEachIndexed { index, condition ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                // Header with logical-operator toggle and delete button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (index == 0) {
                                        Text(
                                            text = stringResource(R.string.rule_condition),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        LogicalOperatorToggle(
                                            selected = condition.logicalOperator,
                                            onSelect = { newOp ->
                                                conditions = conditions.toMutableList().apply {
                                                    set(index, condition.copy(logicalOperator = newOp))
                                                }
                                            }
                                        )
                                    }
                                    if (conditions.size > 1) {
                                        IconButton(
                                            onClick = {
                                                conditions = conditions.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.rule_remove_condition),
                                                modifier = Modifier.size(Dimensions.Icon.small),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Field selector
                                ConditionFieldSelector(
                                    condition = condition,
                                    onConditionChange = { newCondition ->
                                        conditions = conditions.toMutableList().apply {
                                            set(index, newCondition)
                                        }
                                    },
                                    allAccounts = allAccounts
                                )
                            }
                        }
                    }
                }
            }

            // Action section (supports multiple)
            Card {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.rule_then),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        // BLOCK drops the transaction, so it's terminal — no further
                        // actions can run alongside it. Hide "Add Action" while one exists.
                        if (actions.none { it.actionType == ActionType.BLOCK }) {
                            TextButton(
                                onClick = {
                                    actions = actions + RuleAction(
                                        field = TransactionField.CATEGORY,
                                        actionType = ActionType.SET,
                                        value = ""
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(stringResource(R.string.rule_add_action))
                            }
                        }
                    }

                    // Display all actions
                    actions.forEachIndexed { index, action ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                // Header with delete button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (actions.size > 1) stringResource(R.string.rule_action_numbered, index + 1) else stringResource(R.string.rule_action),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (actions.size > 1) {
                                        IconButton(
                                            onClick = {
                                                actions = actions.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.rule_remove_action),
                                                modifier = Modifier.size(Dimensions.Icon.small),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Per-action editor
                                ActionEditor(
                                    action = action,
                                    onActionChange = { updated ->
                                        // BLOCK is terminal — it drops the transaction, so the other
                                        // actions can't run. If the user switches to BLOCK while other
                                        // actions exist, confirm before discarding them (no silent
                                        // data loss); otherwise apply the change directly.
                                        if (updated.actionType == ActionType.BLOCK && actions.size > 1) {
                                            pendingBlockAction = updated
                                        } else {
                                            actions = actions.toMutableList().apply { set(index, updated) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Preview
            val showPreview = ruleName.isNotBlank() &&
                             conditions.isNotEmpty() &&
                             conditions.all { it.validate() } &&
                             actions.isNotEmpty() &&
                             actions.all { it.validate() }
            if (showPreview) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.rule_rule_preview),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        val context = LocalContext.current
                        val previewWhen = stringResource(R.string.rule_preview_when)
                        val previewAnd = stringResource(R.string.rule_preview_and)
                        val previewCommaAnd = stringResource(R.string.rule_preview_comma_and)
                        val previewMatches = stringResource(R.string.rule_preview_op_matches)
                        val previewBlockTransaction = stringResource(R.string.rule_preview_block_transaction)
                        val previewAddTag = stringResource(R.string.rule_preview_add_tag)
                        val previewRemoveTag = stringResource(R.string.rule_preview_remove_tag)
                        val previewSetField = stringResource(R.string.rule_preview_set_field)
                        val previewFieldLabels = mapOf(
                            TransactionField.AMOUNT to stringResource(R.string.rule_preview_field_amount),
                            TransactionField.TYPE to stringResource(R.string.rule_preview_field_type),
                            TransactionField.CATEGORY to stringResource(R.string.rule_preview_field_category),
                            TransactionField.MERCHANT to stringResource(R.string.rule_preview_field_merchant),
                            TransactionField.NARRATION to stringResource(R.string.rule_preview_field_description),
                            TransactionField.SMS_TEXT to stringResource(R.string.rule_preview_field_sms_text),
                            TransactionField.BANK_NAME to stringResource(R.string.rule_preview_field_bank),
                            TransactionField.TRANSACTION_TIME to stringResource(R.string.rule_preview_field_time),
                            TransactionField.TRANSACTION_HOUR to stringResource(R.string.rule_preview_field_hour),
                            TransactionField.TRANSACTION_DAY_OF_WEEK to stringResource(R.string.rule_preview_field_day_of_week),
                            TransactionField.TRANSACTION_DAY_OF_MONTH to stringResource(R.string.rule_preview_field_day_of_month),
                            TransactionField.TRANSACTION_DATE to stringResource(R.string.rule_preview_field_date),
                            TransactionField.ACCOUNT to stringResource(R.string.rule_preview_field_account),
                            TransactionField.TAGS to stringResource(R.string.rule_preview_field_tags)
                        )
                        val previewOperatorLabels = mapOf(
                            ConditionOperator.LESS_THAN to stringResource(R.string.rule_preview_op_before),
                            ConditionOperator.GREATER_THAN to stringResource(R.string.rule_preview_op_after),
                            ConditionOperator.LESS_THAN_OR_EQUAL to stringResource(R.string.rule_preview_op_at_or_before),
                            ConditionOperator.GREATER_THAN_OR_EQUAL to stringResource(R.string.rule_preview_op_at_or_after),
                            ConditionOperator.EQUALS to stringResource(R.string.rule_preview_op_is),
                            ConditionOperator.CONTAINS to stringResource(R.string.rule_preview_op_contains),
                            ConditionOperator.STARTS_WITH to stringResource(R.string.rule_preview_op_starts_with),
                            ConditionOperator.IN to stringResource(R.string.rule_preview_op_is_any_of),
                            ConditionOperator.NOT_EQUALS to stringResource(R.string.rule_preview_op_is_not)
                        )
                        val previewDayNames = mapOf(
                            "1" to stringResource(R.string.rule_day_mon),
                            "2" to stringResource(R.string.rule_day_tue),
                            "3" to stringResource(R.string.rule_day_wed),
                            "4" to stringResource(R.string.rule_day_thu),
                            "5" to stringResource(R.string.rule_day_fri),
                            "6" to stringResource(R.string.rule_day_sat),
                            "7" to stringResource(R.string.rule_day_sun)
                        )
                        val previewActionFieldLabels = mapOf(
                            TransactionField.CATEGORY to stringResource(R.string.rule_preview_set_category),
                            TransactionField.MERCHANT to stringResource(R.string.rule_preview_set_merchant),
                            TransactionField.TYPE to stringResource(R.string.rule_preview_set_type),
                            TransactionField.NARRATION to stringResource(R.string.rule_preview_set_description)
                        )
                        Text(
                            text = buildString {
                                append(previewWhen)
                                conditions.forEachIndexed { index, condition ->
                                    if (index > 0) append(previewAnd)
                                    append(previewFieldLabels.getValue(condition.field))
                                    append(" ")
                                    append(previewOperatorLabels[condition.operator] ?: previewMatches)
                                    append(" ")
                                    when {
                                        condition.field == TransactionField.TYPE -> {
                                            append(ruleTransactionTypeLabel(condition.value, context))
                                        }
                                        condition.field == TransactionField.TRANSACTION_DAY_OF_WEEK -> {
                                            append(condition.value.split(",").joinToString(", ") { previewDayNames[it.trim()] ?: it })
                                        }
                                        condition.field == TransactionField.ACCOUNT -> {
                                            val parts = condition.value.split("||")
                                            if (parts.size == 2) {
                                                append(AccountBalanceEntity.accountLabel(parts[0], parts[1]))
                                            } else {
                                                append(condition.value)
                                            }
                                        }
                                        else -> append(condition.value)
                                    }
                                }
                                append(", ")
                                actions.forEachIndexed { actionIndex, action ->
                                    if (actionIndex > 0) append(previewCommaAnd)
                                    if (action.actionType == ActionType.BLOCK) {
                                        append(previewBlockTransaction)
                                    } else if (action.field == TransactionField.TAGS) {
                                        append(if (action.actionType == ActionType.ADD_TAG) previewAddTag else previewRemoveTag)
                                        append(action.value)
                                    } else {
                                        append(previewActionFieldLabels[action.field] ?: previewSetField)
                                        // Show user-friendly labels for transaction types in actions too
                                        if (action.field == TransactionField.TYPE) {
                                            append(ruleTransactionTypeLabel(action.value, context))
                                        } else {
                                            append(action.value)
                                        }
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    // Confirm before BLOCK discards the other (possibly filled-in) actions.
    if (pendingBlockAction != null) {
        AlertDialog(
            onDismissRequest = { pendingBlockAction = null },
            title = { Text(stringResource(R.string.rule_block_transaction_title)) },
            text = {
                Text(stringResource(R.string.rule_block_transaction_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    actions = listOf(pendingBlockAction!!)
                    pendingBlockAction = null
                }) { Text(stringResource(R.string.rule_block_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlockAction = null }) { Text(stringResource(R.string.rule_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionFieldSelector(
    condition: RuleCondition,
    onConditionChange: (RuleCondition) -> Unit,
    allAccounts: List<RulesViewModel.AccountInfo> = emptyList()
) {
    var fieldDropdownExpanded by remember { mutableStateOf(false) }

    // Field selector
    ExposedDropdownMenuBox(
        expanded = fieldDropdownExpanded,
        onExpandedChange = { fieldDropdownExpanded = !fieldDropdownExpanded }
    ) {
        val fieldOptions = listOf(
            TransactionField.AMOUNT to stringResource(R.string.rule_field_amount),
            TransactionField.TYPE to stringResource(R.string.rule_field_transaction_type),
            TransactionField.CATEGORY to stringResource(R.string.rule_field_category),
            TransactionField.MERCHANT to stringResource(R.string.rule_field_merchant),
            TransactionField.SMS_TEXT to stringResource(R.string.rule_field_sms_text),
            TransactionField.BANK_NAME to stringResource(R.string.rule_field_bank_name),
            TransactionField.TRANSACTION_TIME to stringResource(R.string.rule_field_time_of_day),
            TransactionField.TRANSACTION_HOUR to stringResource(R.string.rule_field_hour),
            TransactionField.TRANSACTION_DAY_OF_WEEK to stringResource(R.string.rule_field_day_of_week),
            TransactionField.TRANSACTION_DAY_OF_MONTH to stringResource(R.string.rule_field_day_of_month),
            TransactionField.TRANSACTION_DATE to stringResource(R.string.rule_field_date),
            TransactionField.ACCOUNT to stringResource(R.string.rule_field_account)
        )
        TextField(
            value = fieldOptions.firstOrNull { it.first == condition.field }?.second ?: stringResource(R.string.rule_field_amount),
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.rule_field)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = fieldDropdownExpanded,
            onDismissRequest = { fieldDropdownExpanded = false }
        ) {
            fieldOptions.forEach { (field, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        // Reset value AND operator: the previous operator may be invalid for the
                        // new field (e.g. keeping "<" from Amount when switching to Transaction
                        // Type, which only supports is / is not).
                        onConditionChange(
                            condition.copy(
                                field = field,
                                value = "",
                                operator = field.defaultConditionOperator()
                            )
                        )
                        fieldDropdownExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.sm))

    // Operator selector
    val operators = conditionOperatorsForField(condition.field)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        operators.forEach { (op, labelRes) ->
            FilterChip(
                selected = condition.operator == op,
                onClick = { onConditionChange(condition.copy(operator = op)) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.sm))

    // Value input
    when (condition.field) {
        TransactionField.TYPE -> {
            Text(
                text = stringResource(R.string.rule_select_transaction_type),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                RULE_TRANSACTION_TYPE_OPTIONS.forEach { (type, displayLabel) ->
                    FilterChip(
                        selected = condition.value.equals(type, ignoreCase = true),
                        onClick = { onConditionChange(condition.copy(value = type)) },
                        label = {
                            Text(stringResource(displayLabel), style = MaterialTheme.typography.bodySmall)
                        }
                    )
                }
            }
        }

        TransactionField.TRANSACTION_DAY_OF_WEEK -> {
            val days = listOf(
                "1" to stringResource(R.string.rule_day_mon),
                "2" to stringResource(R.string.rule_day_tue),
                "3" to stringResource(R.string.rule_day_wed),
                "4" to stringResource(R.string.rule_day_thu),
                "5" to stringResource(R.string.rule_day_fri),
                "6" to stringResource(R.string.rule_day_sat),
                "7" to stringResource(R.string.rule_day_sun)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (condition.operator == ConditionOperator.IN ||
                    condition.operator == ConditionOperator.NOT_IN
                ) {
                    val selectedDays = condition.value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    days.forEach { (value, label) ->
                        FilterChip(
                            selected = value in selectedDays,
                            onClick = {
                                val newSet = if (value in selectedDays) selectedDays - value else selectedDays + value
                                onConditionChange(condition.copy(value = newSet.sorted().joinToString(",")))
                            },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                } else {
                    days.forEach { (value, label) ->
                        FilterChip(
                            selected = condition.value == value,
                            onClick = { onConditionChange(condition.copy(value = value)) },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }

        TransactionField.TRANSACTION_DAY_OF_MONTH -> {
            TextField(
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                label = { Text(stringResource(R.string.rule_day_of_month_label)) },
                placeholder = { Text(stringResource(R.string.rule_day_of_month_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        TransactionField.TRANSACTION_TIME -> {
            var showTimePicker by remember { mutableStateOf(false) }
            val initialHour = condition.value.split(":").getOrNull(0)?.toIntOrNull() ?: 9
            val initialMinute = condition.value.split(":").getOrNull(1)?.toIntOrNull() ?: 0
            val timePickerState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute
            )

            TextField(
                value = condition.value,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.rule_time_label)) },
                placeholder = { Text(stringResource(R.string.rule_time_placeholder)) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.rule_pick_time))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val formatted = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            onConditionChange(condition.copy(value = formatted))
                            showTimePicker = false
                        }) { Text(stringResource(R.string.rule_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.rule_cancel)) }
                    },
                    text = { TimePicker(state = timePickerState) }
                )
            }
        }

        TransactionField.TRANSACTION_HOUR -> {
            TextField(
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                label = { Text(stringResource(R.string.rule_hour_label)) },
                placeholder = { Text(stringResource(R.string.rule_hour_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        TransactionField.TRANSACTION_DATE -> {
            TextField(
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                label = { Text(stringResource(R.string.rule_date_label)) },
                placeholder = { Text(stringResource(R.string.rule_date_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        TransactionField.ACCOUNT -> {
            // Account dropdown picker
            var accountDropdownExpanded by remember { mutableStateOf(false) }

            val selectedAccount = allAccounts.firstOrNull {
                "${it.bankName}||${it.accountLast4}" == condition.value
            }

            val displayText = selectedAccount?.let {
                it.displayName
            } ?: if (condition.value.isNotBlank()) {
                // Show raw value if account no longer exists
                condition.value
            } else {
                stringResource(R.string.rule_select_account)
            }

            Column {
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    TextField(
                        value = displayText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.rule_field_account)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        allAccounts.forEach { account ->
                            val key = "${account.bankName}||${account.accountLast4}"
                            val accountTypeLabel = when {
                                account.isCreditCard -> stringResource(R.string.rule_credit)
                                account.accountType != null -> account.accountType
                                else -> ""
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        FinancialAccountIdentity(
                                            bankName = account.bankName,
                                            accountLast4 = account.accountLast4
                                        )
                                        if (accountTypeLabel.isNotBlank()) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(accountTypeLabel, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onConditionChange(condition.copy(value = key))
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (allAccounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rule_no_accounts),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
            }
        }

        else -> {
            TextField(
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                label = { Text(stringResource(R.string.rule_value)) },
                placeholder = {
                    Text(
                        when(condition.field) {
                            TransactionField.AMOUNT -> stringResource(R.string.rule_placeholder_amount)
                            TransactionField.MERCHANT -> stringResource(R.string.rule_placeholder_merchant)
                            TransactionField.SMS_TEXT -> stringResource(R.string.rule_placeholder_sms)
                            TransactionField.CATEGORY -> stringResource(R.string.rule_placeholder_category)
                            TransactionField.BANK_NAME -> stringResource(R.string.rule_placeholder_bank)
                            else -> stringResource(R.string.rule_enter_value)
                        }
                    )
                },
                keyboardOptions = if (condition.field == TransactionField.AMOUNT) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogicalOperatorToggle(
    selected: LogicalOperator,
    onSelect: (LogicalOperator) -> Unit
) {
    val options = listOf(LogicalOperator.AND, LogicalOperator.OR)
    SegmentedPillRow(
        options = options.map { it.name },
        selectedIndex = options.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> onSelect(options[index]) },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionEditor(
    action: RuleAction,
    onActionChange: (RuleAction) -> Unit
) {
    var actionTypeDropdownExpanded by remember { mutableStateOf(false) }
    var actionFieldDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Action type selector
        ExposedDropdownMenuBox(
            expanded = actionTypeDropdownExpanded,
            onExpandedChange = { actionTypeDropdownExpanded = !actionTypeDropdownExpanded }
        ) {
            TextField(
                value = when(action.actionType) {
                    ActionType.BLOCK -> stringResource(R.string.rule_action_block_transaction)
                    ActionType.SET -> stringResource(R.string.rule_action_set_field)
                    ActionType.APPEND -> stringResource(R.string.rule_action_append_field)
                    ActionType.PREPEND -> stringResource(R.string.rule_action_prepend_field)
                    ActionType.CLEAR -> stringResource(R.string.rule_action_clear_field)
                    ActionType.ADD_TAG -> stringResource(R.string.rule_action_add_tag)
                    ActionType.REMOVE_TAG -> stringResource(R.string.rule_action_remove_tag)
                },
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.rule_action_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionTypeDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = actionTypeDropdownExpanded,
                onDismissRequest = { actionTypeDropdownExpanded = false }
            ) {
                val fieldTypes = if (action.field == TransactionField.TAGS) {
                    listOf(ActionType.ADD_TAG to stringResource(R.string.rule_action_add_tag), ActionType.REMOVE_TAG to stringResource(R.string.rule_action_remove_tag))
                } else {
                    listOf(ActionType.SET to stringResource(R.string.rule_action_set_field), ActionType.CLEAR to stringResource(R.string.rule_action_clear_field))
                }
                (listOf(ActionType.BLOCK to stringResource(R.string.rule_action_block_transaction)) + fieldTypes).forEach { (type, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onActionChange(
                                action.copy(
                                    actionType = type,
                                    value = if (type == ActionType.BLOCK) "" else action.value
                                )
                            )
                            actionTypeDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Show message for BLOCK action or field selector for others
        if (action.actionType == ActionType.BLOCK) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.rule_block_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            // Action field selector for non-BLOCK actions
            ExposedDropdownMenuBox(
                expanded = actionFieldDropdownExpanded,
                onExpandedChange = { actionFieldDropdownExpanded = !actionFieldDropdownExpanded }
            ) {
                TextField(
                    value = when(action.field) {
                        TransactionField.CATEGORY -> stringResource(R.string.rule_action_set_category)
                        TransactionField.MERCHANT -> stringResource(R.string.rule_action_set_merchant)
                        TransactionField.TYPE -> stringResource(R.string.rule_action_set_type)
                        TransactionField.NARRATION -> stringResource(R.string.rule_action_set_description)
                        TransactionField.BANK_NAME -> stringResource(R.string.rule_action_set_account)
                        TransactionField.TAGS -> stringResource(R.string.rule_action_tags)
                        else -> stringResource(R.string.rule_action_set_field)
                    },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.rule_action)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionFieldDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = actionFieldDropdownExpanded,
                    onDismissRequest = { actionFieldDropdownExpanded = false }
                ) {
                    listOf(
                        TransactionField.CATEGORY to stringResource(R.string.rule_action_set_category),
                        TransactionField.MERCHANT to stringResource(R.string.rule_action_set_merchant),
                        TransactionField.TYPE to stringResource(R.string.rule_action_set_type),
                        TransactionField.NARRATION to stringResource(R.string.rule_action_set_description),
                        TransactionField.BANK_NAME to stringResource(R.string.rule_action_set_account),
                        TransactionField.TAGS to stringResource(R.string.rule_action_tags)
                    ).forEach { (field, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                // Keep the action type one the engine can carry out on the
                                // new field (SET on TAGS, or ADD_TAG on CATEGORY, would be dead).
                                val types = supportedActionTypes(field)
                                val actionType = if (action.actionType in types) action.actionType else types.first()
                                onActionChange(action.copy(field = field, actionType = actionType, value = ""))
                                actionFieldDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Dynamic value input based on selected action field
            when (action.field) {
                TransactionField.CATEGORY -> {
                    // Category chips and input
                    val commonCategories = listOf(
                        "Food & Dining", "Transportation", "Shopping",
                        "Bills & Utilities", "Entertainment", "Healthcare",
                        "Investments", "Others"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonCategories.forEach { category ->
                            FilterChip(
                                selected = action.value == category,
                                onClick = { onActionChange(action.copy(value = category)) },
                                label = { Text(category, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_category_name)) },
                        placeholder = { Text(stringResource(R.string.rule_category_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                TransactionField.TYPE -> {
                    // Transaction type chips with user-friendly labels
                    Text(
                        text = stringResource(R.string.rule_select_transaction_type),
                        style = MaterialTheme.typography.bodySmall
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RULE_TRANSACTION_TYPE_OPTIONS.forEach { (type, displayLabel) ->
                            FilterChip(
                                selected = action.value.equals(type, ignoreCase = true),
                                onClick = { onActionChange(action.copy(value = type)) },
                                label = {
                                    Text(
                                        stringResource(displayLabel),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            )
                        }
                    }
                }

                TransactionField.MERCHANT -> {
                    // Merchant name input with common suggestions
                    val commonMerchants = listOf(
                        "Amazon", "Swiggy", "Zomato", "Uber",
                        "Netflix", "Google", "Flipkart"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonMerchants.forEach { merchant ->
                            ElevatedAssistChip(
                                onClick = { onActionChange(action.copy(value = merchant)) },
                                label = { Text(merchant, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_merchant_name)) },
                        placeholder = { Text(stringResource(R.string.rule_merchant_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                TransactionField.NARRATION -> {
                    // Description/Narration input
                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_description)) },
                        placeholder = { Text(stringResource(R.string.rule_narration_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                TransactionField.BANK_NAME -> {
                    // Account / bank the transaction belongs to.
                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_account_bank_name)) },
                        placeholder = { Text(stringResource(R.string.rule_placeholder_bank)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                TransactionField.TAGS -> {
                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_tag_name)) },
                        placeholder = { Text(stringResource(R.string.rule_placeholder_merchant)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                else -> {
                    // Generic text input for other fields
                    TextField(
                        value = action.value,
                        onValueChange = { onActionChange(action.copy(value = it)) },
                        label = { Text(stringResource(R.string.rule_value)) },
                        placeholder = { Text(stringResource(R.string.rule_enter_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}