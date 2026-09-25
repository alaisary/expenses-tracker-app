package com.pennywiseai.tracker.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

data class FAQItem(
    val question: String,
    val answer: String
)

data class FAQCategory(
    val title: String,
    val icon: @Composable () -> Unit,
    val items: List<FAQItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val faqCategories = listOf(
            FAQCategory(
                title = stringResource(R.string.appr_faq_cat_transaction_types),
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_wallet_credit),
                        answer = stringResource(R.string.appr_faq_a_wallet_credit)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_transaction_types),
                        answer = stringResource(R.string.appr_faq_a_transaction_types)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_transfer_vs_expense),
                        answer = stringResource(R.string.appr_faq_a_transfer_vs_expense)
                    ),
                    // The two situations users ask about most: money passing through
                    // the account, and covering someone who pays you back. Both are
                    // answered by an existing control (type, exclusion, loan) that
                    // isn't obvious from the row itself.
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_pass_through),
                        answer = stringResource(R.string.appr_faq_a_pass_through)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_paid_for_someone),
                        answer = stringResource(R.string.appr_faq_a_paid_for_someone)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_exclude_transaction),
                        answer = stringResource(R.string.appr_faq_a_exclude_transaction)
                    )
                )
            ),
            FAQCategory(
                title = stringResource(R.string.appr_faq_cat_sms_parsing),
                icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_sms_not_detected),
                        answer = stringResource(R.string.appr_faq_a_sms_not_detected)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_unrecognized_sms),
                        answer = stringResource(R.string.appr_faq_a_unrecognized_sms)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_duplicates),
                        answer = stringResource(R.string.appr_faq_a_duplicates)
                    )
                )
            ),
            FAQCategory(
                title = stringResource(R.string.appr_faq_cat_privacy_data),
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_secure),
                        answer = stringResource(R.string.appr_faq_a_secure)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_backup),
                        answer = stringResource(R.string.appr_faq_a_backup)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_data_access),
                        answer = stringResource(R.string.appr_faq_a_data_access)
                    )
                )
            ),
            FAQCategory(
                title = stringResource(R.string.appr_faq_cat_account_management),
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_manual_accounts),
                        answer = stringResource(R.string.appr_faq_a_manual_accounts)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_multiple_accounts),
                        answer = stringResource(R.string.appr_faq_a_multiple_accounts)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_hide_account),
                        answer = stringResource(R.string.appr_faq_a_hide_account)
                    ),
                    FAQItem(
                        question = stringResource(R.string.appr_faq_q_hide_category),
                        answer = stringResource(R.string.appr_faq_a_hide_category)
                    )
                )
            )
        )
    
    var expandedCategories by remember { mutableStateOf(setOf<Int>()) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.appr_faq_title),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.appr_back))
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
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // FAQ Categories
            faqCategories.forEachIndexed { categoryIndex, category ->
                SectionHeaderV2(title = category.title)
                
                PennyWiseCardV2(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        category.items.forEachIndexed { itemIndex, faqItem ->
                            val isExpanded = expandedCategories.contains(categoryIndex * 100 + itemIndex)
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories = if (isExpanded) {
                                            expandedCategories - (categoryIndex * 100 + itemIndex)
                                        } else {
                                            expandedCategories + (categoryIndex * 100 + itemIndex)
                                        }
                                    }
                                    .padding(Dimensions.Padding.content)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                                    ) {
                                        if (itemIndex == 0) {
                                            Box(
                                                modifier = Modifier.size(Dimensions.Icon.medium),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                category.icon()
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(Dimensions.Icon.medium))
                                        }
                                        
                                        Text(
                                            text = faqItem.question,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) stringResource(R.string.appr_collapse) else stringResource(R.string.appr_expand),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                                    ) {
                                        Spacer(modifier = Modifier.width(24.dp))
                                        Text(
                                            text = faqItem.answer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = Spacing.sm)
                                        )
                                    }
                                }
                            }
                            
                            if (itemIndex < category.items.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                                )
                            }
                        }
                    }
                }
            }
            
            // Still need help section
            SectionHeaderV2(title = stringResource(R.string.appr_still_need_help))
            
            PennyWiseCardV2(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(com.pennywiseai.tracker.core.Constants.Links.GITHUB_URL + "/issues/new/choose")
                        )
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.appr_report_issue),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.appr_report_issue_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.medium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}