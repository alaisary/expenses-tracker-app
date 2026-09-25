package com.pennywiseai.tracker.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.EditProfileViewModel

private val OPTION_TILE_SIZE = 72.dp
private val SELECTION_RING_WIDTH = 3.dp

/**
 * Name, avatar and tile colour for the profile — the same three choices
 * onboarding asks for, editable afterwards.
 *
 * Onboarding's answers used to be final: the only writes to those preferences
 * were in `completeOnboarding`, so a typo in the name or a photo you outgrew
 * stayed until a reinstall. This is the same picker, pointed at the same
 * preferences.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileSheet(
    onDismiss: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.selectProfileImage(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = stringResource(R.string.prof_edit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            TextField(
                value = uiState.userName,
                onValueChange = viewModel::updateUserName,
                label = { Text(stringResource(R.string.onb_profile_name_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.onb_profile_avatar_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(AvatarHelper.avatarDrawables) { index, drawableRes ->
                    val isSelected = uiState.photoUri == null && uiState.selectedAvatarIndex == index
                    Box(
                        modifier = Modifier
                            .size(OPTION_TILE_SIZE)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    SELECTION_RING_WIDTH,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { viewModel.selectAvatar(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = stringResource(
                                R.string.onb_avatar_content_description,
                                index + 1
                            ),
                            modifier = Modifier.size(Dimensions.Icon.avatar),
                            colorFilter = ColorFilter.tint(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                item {
                    // Same gallery slot as onboarding: an avatar is a preset OR a
                    // photo, never both, so one of the two is always the current pick.
                    val photoSelected = uiState.photoUri != null
                    Box(
                        modifier = Modifier
                            .size(OPTION_TILE_SIZE)
                            .clip(CircleShape)
                            .background(
                                if (photoSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (photoSelected) Modifier.border(
                                    SELECTION_RING_WIDTH,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.onb_photo_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimensions.Icon.large)
                            )
                        } else {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.onb_profile_background_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarHelper.avatarBackgroundColors.forEachIndexed { index, colorInt ->
                    val isSelected = uiState.selectedBackgroundColor == index
                    Box(
                        modifier = Modifier
                            .size(Dimensions.Icon.avatar)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .then(
                                if (isSelected) Modifier.border(
                                    SELECTION_RING_WIDTH,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { viewModel.selectBackgroundColor(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.onb_selected),
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(Dimensions.Icon.medium)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Button(
                onClick = { viewModel.save(onDismiss) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = if (uiState.saving) {
                        stringResource(R.string.prof_edit_saving)
                    } else {
                        stringResource(R.string.prof_edit_save)
                    }
                )
            }
        }
    }
}
