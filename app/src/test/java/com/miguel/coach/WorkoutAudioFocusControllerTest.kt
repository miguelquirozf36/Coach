package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutAudioFocusControllerTest {
    @Test
    fun disabledDoesNotRequestFocus() {
        val fixture = fixture(enabled = false)

        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.acquireTransient("voice")

        assertEquals(0, fixture.gateway.requestCount)
    }

    @Test
    fun enabledContinuousOwnershipRequestsOnce() {
        val fixture = fixture(enabled = true)

        fixture.controller.setContinuousDuckingDesired(true)

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun repeatedContinuousStateDoesNotDuplicateRequest() {
        val fixture = fixture(enabled = true)

        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.setContinuousDuckingDesired(true)

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun clearingContinuousOwnershipAbandonsOnce() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)

        fixture.controller.setContinuousDuckingDesired(false)
        fixture.controller.setContinuousDuckingDesired(false)

        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun transientTokenRequestsFocus() {
        val fixture = fixture(enabled = true)

        fixture.controller.acquireTransient("voice")

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun duplicateTransientTokenDoesNotDuplicateOwnership() {
        val fixture = fixture(enabled = true)

        fixture.controller.acquireTransient("voice")
        fixture.controller.acquireTransient("voice")

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun multipleTransientTokensKeepFocusUntilTheLastOneIsReleased() {
        val fixture = fixture(enabled = true)
        fixture.controller.acquireTransient("first")
        fixture.controller.acquireTransient("second")

        fixture.controller.releaseTransient("first")
        assertEquals(0, fixture.gateway.abandonCount)

        fixture.controller.releaseTransient("second")
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun releasingUnknownTransientTokenIsSafe() {
        val fixture = fixture(enabled = true)

        fixture.controller.releaseTransient("missing")

        assertEquals(0, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun transientReleaseDoesNotRemoveContinuousOwnership() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.acquireTransient("voice")

        fixture.controller.releaseTransient("voice")

        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun clearingContinuousOwnershipKeepsTransientOwnership() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.acquireTransient("voice")

        fixture.controller.setContinuousDuckingDesired(false)

        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun toggleOffAbandonsFocusAndClearsTransientTokens() {
        val fixture = fixture(enabled = true)
        fixture.controller.acquireTransient("voice")

        fixture.controller.setEnabled(false)
        fixture.controller.setEnabled(true)

        assertEquals(1, fixture.gateway.abandonCount)
        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun toggleOffPreventsNewTransientRequests() {
        val fixture = fixture(enabled = true)
        fixture.controller.setEnabled(false)

        fixture.controller.acquireTransient("voice")

        assertEquals(0, fixture.gateway.requestCount)
    }

    @Test
    fun toggleOnReevaluatesExistingContinuousOwnership() {
        val fixture = fixture(enabled = false)
        fixture.controller.setContinuousDuckingDesired(true)

        fixture.controller.setEnabled(true)

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun rejectedRequestDoesNotMarkFocusAsOwnedAndCanRetryAfterAValidTransition() {
        val gateway = FakeWorkoutAudioFocusGateway(grantRequests = false)
        val controller = WorkoutAudioFocusController(gateway, enabled = true)
        controller.setContinuousDuckingDesired(true)

        controller.setContinuousDuckingDesired(false)
        gateway.grantRequests = true
        controller.setContinuousDuckingDesired(true)

        assertEquals(2, gateway.requestCount)
        assertEquals(0, gateway.abandonCount)
    }

    @Test
    fun rejectedRequestDoesNotRetryWhileFocusRemainsDesired() {
        val gateway = FakeWorkoutAudioFocusGateway(grantRequests = false)
        val controller = WorkoutAudioFocusController(gateway, enabled = true)

        controller.acquireTransient("first")
        controller.acquireTransient("second")
        controller.setContinuousDuckingDesired(true)

        assertEquals(1, gateway.requestCount)
    }

    @Test
    fun cleanupReleasesFocusAndClearsAllOwnership() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.acquireTransient("voice")

        fixture.controller.cleanup()
        fixture.controller.setEnabled(true)

        assertEquals(1, fixture.gateway.abandonCount)
        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun repeatedCleanupIsSafe() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)

        fixture.controller.cleanup()
        fixture.controller.cleanup()

        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun continuousAndMultipleTransientTokensShareOneFocusRequest() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)
        fixture.controller.acquireTransient("first")
        fixture.controller.acquireTransient("second")

        fixture.controller.setContinuousDuckingDesired(false)
        fixture.controller.releaseTransient("first")
        fixture.controller.releaseTransient("second")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun permanentFocusLossAllowsARequestAfterTheNextValidTransition() {
        val fixture = fixture(enabled = true)
        fixture.controller.setContinuousDuckingDesired(true)
        fixture.gateway.loseFocusPermanently()

        fixture.controller.setContinuousDuckingDesired(false)
        fixture.controller.setContinuousDuckingDesired(true)

        assertEquals(2, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    private fun fixture(enabled: Boolean): Fixture {
        val gateway = FakeWorkoutAudioFocusGateway()
        return Fixture(WorkoutAudioFocusController(gateway, enabled), gateway)
    }

    private data class Fixture(
        val controller: WorkoutAudioFocusController,
        val gateway: FakeWorkoutAudioFocusGateway
    )
}

private class FakeWorkoutAudioFocusGateway(
    var grantRequests: Boolean = true
) : WorkoutAudioFocusGateway {
    var requestCount = 0
    var abandonCount = 0
    private var onPermanentFocusLoss: (() -> Unit)? = null

    override fun requestFocus(onPermanentFocusLoss: () -> Unit): Boolean {
        requestCount += 1
        this.onPermanentFocusLoss = onPermanentFocusLoss
        return grantRequests
    }

    override fun abandonFocus() {
        abandonCount += 1
    }

    fun loseFocusPermanently() {
        onPermanentFocusLoss?.invoke()
    }
}
