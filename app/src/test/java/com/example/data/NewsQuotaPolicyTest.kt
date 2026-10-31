package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Quota-gate tests for the news auto-refresh policy.
 *
 * The user's news API quota is a finite, paid-for resource. These tests pin down the exact contract
 * so a refactor cannot silently turn an automatic refresh into a quota spend:
 *
 *  - quota at or below the floor blocks automatic refresh
 *  - a user-forced refresh bypasses the quota floor but never the offline/backoff guards
 *  - battery-optimization mode blocks automatic refresh
 *  - offline always surfaces the cache rather than losing it
 */
@RunWith(RobolectricTestRunner::class)
class NewsQuotaPolicyTest {

    private val now = 1_000_000L

    /** The canonical "user has quota and nothing is wrong" baseline. */
    private fun healthy() = BlockingArgs()

    private data class BlockingArgs(
        val force: Boolean = false,
        val autoRefreshEnabled: Boolean = true,
        val batteryOptimizationMode: Boolean = false,
        val isOffline: Boolean = false,
        val rateLimitRemaining: Int? = 100,
        val backoffUntilEpochSeconds: Long? = null
    )

    private fun reason(args: BlockingArgs): AutoRefreshBlock? =
        NewsQuotaPolicy.blockingReason(
            nowEpochSeconds = now,
            force = args.force,
            autoRefreshEnabled = args.autoRefreshEnabled,
            batteryOptimizationMode = args.batteryOptimizationMode,
            isOffline = args.isOffline,
            rateLimitRemaining = args.rateLimitRemaining,
            backoffUntilEpochSeconds = args.backoffUntilEpochSeconds
        )

    // ------------------------------------------------------------------
    // 1. Quota floor blocks automatic refresh
    // ------------------------------------------------------------------

    @Test
    fun quotaAtFloorBlocksAutoRefresh() {
        assertEquals(
            AutoRefreshBlock.QUOTA_FLOOR_REACHED,
            reason(healthy().copy(rateLimitRemaining = NewsQuotaPolicy.QUOTA_FLOOR))
        )
    }

    @Test
    fun quotaBelowFloorBlocksAutoRefresh() {
        assertEquals(
            AutoRefreshBlock.QUOTA_FLOOR_REACHED,
            reason(healthy().copy(rateLimitRemaining = 3))
        )
    }

    @Test
    fun quotaAboveFloorDoesNotBlock() {
        assertNull(reason(healthy().copy(rateLimitRemaining = NewsQuotaPolicy.QUOTA_FLOOR + 1)))
    }

    @Test
    fun unknownQuotaDoesNotBlock() {
        // A null (never-seen) quota must not be read as "plenty left" — but it also must not be
        // treated as exhausted; it is simply unknown and the first result sets it.
        assertNull(reason(healthy().copy(rateLimitRemaining = null)))
    }

    // ------------------------------------------------------------------
    // 2. Forced refresh may bypass the quota floor
    // ------------------------------------------------------------------

    @Test
    fun forcedRefreshBypassesQuotaFloor() {
        assertNull(
            reason(
                healthy().copy(
                    force = true,
                    rateLimitRemaining = 1
                )
            )
        )
    }

    @Test
    fun forcedRefreshCannotBypassOffline() {
        assertEquals(
            AutoRefreshBlock.OFFLINE,
            reason(healthy().copy(force = true, isOffline = true))
        )
    }

    @Test
    fun forcedRefreshCannotBypassBackoff() {
        assertEquals(
            AutoRefreshBlock.BACKOFF,
            reason(healthy().copy(force = true, backoffUntilEpochSeconds = now + 60))
        )
    }

    // ------------------------------------------------------------------
    // 3. Battery optimization blocks automatic refresh
    // ------------------------------------------------------------------

    @Test
    fun batteryOptimizationBlocksAutoRefresh() {
        assertEquals(
            AutoRefreshBlock.BATTERY_OPTIMIZATION,
            reason(healthy().copy(batteryOptimizationMode = true))
        )
    }

    @Test
    fun batteryOptimizationDoesNotBlockForcedRefresh() {
        assertNull(reason(healthy().copy(force = true, batteryOptimizationMode = true)))
    }

    // ------------------------------------------------------------------
    // 4. Offline always surfaces the cache
    // ------------------------------------------------------------------

    @Test
    fun offlineBlocksRefresh() {
        assertEquals(
            AutoRefreshBlock.OFFLINE,
            reason(healthy().copy(isOffline = true))
        )
    }

    @Test
    fun offlineBlocksEvenWhenAutoRefreshEnabled() {
        assertEquals(
            AutoRefreshBlock.OFFLINE,
            reason(
                healthy().copy(
                    isOffline = true,
                    autoRefreshEnabled = true,
                    rateLimitRemaining = 500
                )
            )
        )
    }

    // ------------------------------------------------------------------
    // 5. Auto-refresh is opt-in
    // ------------------------------------------------------------------

    @Test
    fun autoRefreshDisabledBlocksBackgroundRefresh() {
        assertEquals(
            AutoRefreshBlock.NOT_ENABLED,
            reason(healthy().copy(autoRefreshEnabled = false))
        )
    }

    @Test
    fun autoRefreshDisabledDoesNotBlockForcedRefresh() {
        assertNull(reason(healthy().copy(force = true, autoRefreshEnabled = false)))
    }

    // ------------------------------------------------------------------
    // 6. Ordering: the hard network guards outrank the user's opt-in toggle
    // ------------------------------------------------------------------

    @Test
    fun offlineOutranksBackoffAndQuotaFloor() {
        // Offline is checked first: there is no point reasoning about quota when nothing can send.
        assertEquals(
            AutoRefreshBlock.OFFLINE,
            reason(
                healthy().copy(
                    isOffline = true,
                    backoffUntilEpochSeconds = now + 60,
                    rateLimitRemaining = 0
                )
            )
        )
    }

    @Test
    fun backoffOutranksQuotaFloor() {
        assertEquals(
            AutoRefreshBlock.BACKOFF,
            reason(healthy().copy(backoffUntilEpochSeconds = now + 60, rateLimitRemaining = 0))
        )
    }

    @Test
    fun expiredBackoffDoesNotBlock() {
        // A backoff that has already lapsed must not keep the widget permanently stale.
        assertNull(reason(healthy().copy(backoffUntilEpochSeconds = now - 1, rateLimitRemaining = 50)))
    }

    // ------------------------------------------------------------------
    // 7. Retry-after hint
    // ------------------------------------------------------------------

    @Test
    fun quotaRetryAfterComputedFromResetEpoch() {
        val hint = NewsQuotaPolicy.quotaRetryAfterSeconds(now + 900, now)
        assertEquals(900L, hint)
    }

    @Test
    fun quotaRetryAfterIsNullWhenResetIsUnknownOrPast() {
        assertNull(NewsQuotaPolicy.quotaRetryAfterSeconds(null, now))
        assertNull(NewsQuotaPolicy.quotaRetryAfterSeconds(0L, now))
        assertNull(NewsQuotaPolicy.quotaRetryAfterSeconds(now - 30, now))
    }

    // ------------------------------------------------------------------
    // 8. The persisted default really is off
    // ------------------------------------------------------------------

    @Test
    fun defaultPreferencesDoNotAutoRefresh() {
        // Regression guard: a default-constructed state must never opt itself in.
        val state = NewsPreferencesState()
        assertTrue(
            "default NewsPreferencesState must not enable background auto-refresh",
            !state.autoRefreshEnabled
        )
    }
}
