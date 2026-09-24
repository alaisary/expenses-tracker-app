package com.pennywiseai.tracker.di

import com.pennywiseai.tracker.billing.EntitlementSource
import com.pennywiseai.tracker.billing.PlayBillingGateway
import com.pennywiseai.tracker.billing.PurchaseGateway
import com.pennywiseai.tracker.billing.PurchaseLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for billing. [PurchaseGateway] is implemented by Google Play
 * Billing 9; the narrow interfaces ([EntitlementSource] for readers,
 * [PurchaseLauncher] for the paywall) bind to the same singleton so consumers
 * can depend on only what they need (ISP).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindPurchaseGateway(impl: PlayBillingGateway): PurchaseGateway

    @Binds
    @Singleton
    abstract fun bindEntitlementSource(impl: PlayBillingGateway): EntitlementSource

    @Binds
    @Singleton
    abstract fun bindPurchaseLauncher(impl: PlayBillingGateway): PurchaseLauncher
}
