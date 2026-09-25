package com.pennywiseai.tracker.presentation.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.ui.components.CategoryChip
import com.pennywiseai.tracker.ui.components.EmojiIconTile
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.icons.groupedForDisplay
import com.pennywiseai.tracker.ui.icons.localizedCategoryName
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val moveState by viewModel.moveState.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Show snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbarMessage()
            }
        }
    }
    
    // Categories organised under their group sections, in display order.
    val groupedCategories = groupedForDisplay(categories)

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
                title = stringResource(R.string.feat_categories_title),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feat_categories_back))
                    }
                },
                hazeState = hazeState
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.feat_categories_add_cd))
            }
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = 100.dp // Space for FAB
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            groupedCategories.forEach { (group, groupCategories) ->
                item(key = "group_${group.name}") {
                    SectionHeaderV2(title = stringResource(group.labelRes))
                }

                items(
                    items = groupCategories,
                    key = { it.id }
                ) { category ->
                    // Sub-categories sit indented under their parent (#374).
                    Box(modifier = Modifier.padding(start = if (category.parentId != null) Spacing.lg else Spacing.none)) {
                        SwipeableCategoryItem(
                            category = category,
                            canMoveUp = category.id in moveState.canMoveUp,
                            canMoveDown = category.id in moveState.canMoveDown,
                            onMoveUp = { viewModel.moveCategory(category, up = true) },
                            onMoveDown = { viewModel.moveCategory(category, up = false) },
                            onCategoryClick = { onCategoryClick(category.name) },
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) },
                            onToggleHidden = { viewModel.toggleCategoryHidden(category.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddEditDialog) {
        CategoryEditDialog(
            category = editingCategory,
            parentOptions = categories,
            onDismiss = { viewModel.hideDialog() },
            onSave = { name, color, isIncome, icon, parentId ->
                viewModel.saveCategory(name, color, isIncome, icon, parentId)
            },
            onDelete = editingCategory?.let { cat ->
                {
                    viewModel.deleteCategory(cat)
                    viewModel.hideDialog()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: CategoryEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCategoryClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHidden: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!category.isSystem) {
                        onDelete()
                        true
                    } else {
                        false // Don't allow swipe for system categories
                    }
                }
                else -> false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (!category.isSystem) {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    },
                    label = "background color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = Dimensions.Padding.content),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.feat_categories_delete_cd),
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        },
        content = {
            CategoryItem(
                category = category,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onClick = {
                    onCategoryClick()
                    if (!category.isSystem) onEdit()
                },
                onToggleHidden = onToggleHidden
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !category.isSystem
    )
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    onClick: (() -> Unit)?,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onToggleHidden: () -> Unit = {}
) {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
                // Dim a hidden category so it reads as "tucked away" in the manager.
                .alpha(if (category.isHidden) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category with colored dot
            val emoji = category.icon?.takeIf { it.isNotBlank() }
            if (emoji != null) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    EmojiIconTile(emoji = emoji)
                    Text(
                        text = localizedCategoryName(category.name),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                CategoryChip(
                    category = category,
                    showText = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // One-step reorder inside the section. Disabled rather than hidden at
            // the ends, so the controls next to a row don't shift as rows move
            // past each other. Top-level rows only — see [CategoryMoveState].
            //
            // No tint override here on purpose: IconButton dims its content colour
            // when disabled, and that dimming is the only thing telling the user
            // that this row is already at the end of its section (a move can never
            // cross a section boundary). Pinning the colour made a dead arrow look
            // exactly like a live one, so a tap that did nothing looked like a bug.
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.feat_categories_move_up_cd),
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.feat_categories_move_down_cd),
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }

            // Hide / unhide toggle — available for every category, including
            // system defaults (the whole point of #736).
            IconButton(onClick = onToggleHidden) {
                Icon(
                    imageVector = if (category.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (category.isHidden) {
                        stringResource(R.string.feat_categories_show_cd)
                    } else {
                        stringResource(R.string.feat_categories_hide_cd)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }

            // System badge
            if (category.isSystem) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.feat_categories_system_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(
                            horizontal = Spacing.sm,
                            vertical = Spacing.xs
                        )
                    )
                }
            } else {
                // Edit icon for non-system categories
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.feat_categories_edit_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(Dimensions.Icon.medium)
                        .padding(start = Spacing.sm)
                )
            }
        }
    }
}