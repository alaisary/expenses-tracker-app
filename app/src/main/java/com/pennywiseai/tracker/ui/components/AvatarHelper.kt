package com.pennywiseai.tracker.ui.components

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.pennywiseai.tracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AvatarHelper {

    val avatarDrawables = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
        R.drawable.avatar_7,
        R.drawable.avatar_8,
        R.drawable.avatar_9,
        R.drawable.avatar_10
    )

    /**
     * The sixteen background colours offered behind the avatar, in picker order.
     *
     * Lives here rather than in a ViewModel because two screens offer the same
     * choice — onboarding and the profile editor — and a palette duplicated per
     * screen drifts: a colour picked at sign-up has to be recognisable, and
     * still selected, when the same choice is shown again months later.
     */
    val avatarBackgroundColors = listOf(
        0xFFDC8A78.toInt(), // Rosewater
        0xFFDD7878.toInt(), // Flamingo
        0xFFEA76CB.toInt(), // Pink
        0xFF8839EF.toInt(), // Mauve
        0xFFD20F39.toInt(), // Red
        0xFFFE640B.toInt(), // Peach
        0xFFDF8E1D.toInt(), // Yellow
        0xFF40A02B.toInt(), // Green
        0xFF179299.toInt(), // Teal
        0xFF04A5E5.toInt(), // Sky
        0xFF209FB5.toInt(), // Sapphire
        0xFF1E66F5.toInt(), // Blue
        0xFF7287FD.toInt(), // Lavender
        0xFF6C6F85.toInt(), // Subtext0
        0xFF8C8FA1.toInt(), // Overlay1
        0xFFACB0BE.toInt()  // Overlay2
    )

    private const val AVATAR_SCHEME = "avatar://"

    /**
     * The preset index encoded in an "avatar://INDEX" URI, or null when the URI
     * is a picked photo (or anything else) rather than a preset avatar.
     */
    fun avatarIndexOf(uri: String?): Int? {
        val value = uri ?: return null
        if (!value.startsWith(AVATAR_SCHEME)) return null
        val index = value.removePrefix(AVATAR_SCHEME).toIntOrNull() ?: return null
        return index.takeIf { it in avatarDrawables.indices }
    }

    /**
     * Resolves an "avatar://INDEX" URI to a drawable resource ID.
     * Returns null if the URI is not an avatar URI or index is out of range.
     */
    fun resolveAvatarDrawable(uri: String): Int? =
        avatarIndexOf(uri)?.let { avatarDrawables[it] }

    /**
     * Copies a picked photo into app storage and returns its URI, or null when
     * the picker handed back something unreadable.
     *
     * One slot per profile, so a second pick replaces the first — the file the
     * old URI pointed at stops being referenced the moment the new one is saved.
     */
    suspend fun saveProfileImage(context: Context, sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val file = File(context.filesDir, PROFILE_IMAGE_FILE)
            file.outputStream().use { output -> inputStream.use { input -> input.copyTo(output) } }
            file.toUri()
        } catch (_: Exception) {
            null
        }
    }

    private const val PROFILE_IMAGE_FILE = "profile_image.jpg"
}
