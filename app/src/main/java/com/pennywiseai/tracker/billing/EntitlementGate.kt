package com.pennywiseai.tracker.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature access gate. This build is fully unlocked: every Pro-gated feature
 * is always available and there is no paywall or Play Billing at runtime.
 */
@Singleton
class EntitlementGate @Inject constructor() {

    /** Always `true` — this build has no paid tier. */
    val isProEntitled: StateFlow<Boolean> = MutableStateFlow(true)
}
