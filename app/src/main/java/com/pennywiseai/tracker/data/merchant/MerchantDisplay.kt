package com.pennywiseai.tracker.data.merchant

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Resolves the name shown for a merchant at render time, so a user-defined alias
 * (#583) applies everywhere — transaction rows, the analytics merchant list and
 * the receipt header — without prop-drilling the alias map through every screen.
 *
 * Defaults to the raw name; `MainScreen` provides the aliased version. (The UPI
 * contact-name resolution that used to sit alongside this was removed with the
 * rest of the India-specific surface.)
 */
val LocalMerchantDisplay = staticCompositionLocalOf<(String?) -> String?> { { it } }
