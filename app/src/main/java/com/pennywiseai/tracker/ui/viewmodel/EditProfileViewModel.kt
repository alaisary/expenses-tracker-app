package com.pennywiseai.tracker.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.ui.components.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the profile editor is showing. The two "nothing selected" defaults are -1
 * rather than 0: index 0 is a real avatar and a real colour, so 0 as a sentinel
 * would silently pre-select the first option of a profile that never chose one.
 */
data class EditProfileUiState(
    val userName: String = "",
    /** Index into [AvatarHelper.avatarDrawables], or -1 when a photo is in use. */
    val selectedAvatarIndex: Int = -1,
    /** The picked photo, when the profile uses one instead of a preset avatar. */
    val photoUri: Uri? = null,
    /** Index into [AvatarHelper.avatarBackgroundColors], or -1 for an unlisted colour. */
    val selectedBackgroundColor: Int = -1,
    /** False until the saved values arrive, so the sheet never opens on blanks. */
    val loaded: Boolean = false,
    val saving: Boolean = false
) {
    /** A profile with no name would render as a blank greeting everywhere. */
    val canSave: Boolean get() = loaded && !saving && userName.isNotBlank()
}

/**
 * Backs the profile editor — the name, avatar and tile colour that onboarding
 * asks for once and, until this existed, nothing could change afterwards.
 *
 * Writes through the same three preference setters onboarding uses, so the two
 * surfaces can't disagree about what a profile is.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            val avatarIndex = AvatarHelper.avatarIndexOf(prefs.profileImageUri) ?: -1
            _uiState.update {
                it.copy(
                    userName = prefs.userName,
                    selectedAvatarIndex = avatarIndex,
                    photoUri = if (avatarIndex == -1) prefs.profileImageUri?.toUri() else null,
                    selectedBackgroundColor =
                        AvatarHelper.avatarBackgroundColors.indexOf(prefs.profileBackgroundColor),
                    loaded = true
                )
            }
        }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun selectAvatar(index: Int) {
        _uiState.update { it.copy(selectedAvatarIndex = index, photoUri = null) }
    }

    fun selectProfileImage(uri: Uri) {
        viewModelScope.launch {
            val savedUri = AvatarHelper.saveProfileImage(context, uri)
            _uiState.update {
                it.copy(photoUri = savedUri ?: uri, selectedAvatarIndex = -1)
            }
        }
    }

    fun selectBackgroundColor(index: Int) {
        _uiState.update { it.copy(selectedBackgroundColor = index) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            userPreferencesRepository.updateUserName(state.userName.trim())
            // A photo wins over a preset only while it is the current selection:
            // picking a preset clears the photo, and both paths write the URI the
            // rest of the app reads, so no stale "avatar://" can outlive the choice.
            val image = state.photoUri?.toString()
                ?: state.selectedAvatarIndex
                    .takeIf { it in AvatarHelper.avatarDrawables.indices }
                    ?.let { "avatar://$it" }
            if (image != null) {
                userPreferencesRepository.updateProfileImageUri(image)
            }
            AvatarHelper.avatarBackgroundColors.getOrNull(state.selectedBackgroundColor)?.let { color ->
                userPreferencesRepository.updateProfileBackgroundColor(color)
            }
            _uiState.update { it.copy(saving = false) }
            onSaved()
        }
    }
}
