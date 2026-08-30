package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutAudioFocusIntegrationTest {
    @Test
    fun continuousDuckingClassifiesEveryTrainingState() {
        listOf(
            TrainingPhase.CONCENTRIC,
            TrainingPhase.ECCENTRIC,
            TrainingPhase.ISOMETRIC,
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            TrainingPhase.WARMUP,
            TrainingPhase.COUNTDOWN
        ).forEach { phase ->
            assertTrue(phase.name, shouldUseContinuousWorkoutDucking(workout(phase)))
        }
        listOf(
            TrainingPhase.REST,
            TrainingPhase.REST_BETWEEN_EXERCISES
        ).forEach { phase ->
            assertFalse(phase.name, shouldUseContinuousWorkoutDucking(workout(phase)))
        }
        assertFalse(shouldUseContinuousWorkoutDucking(TrainingUiState.Home))
        assertFalse(shouldUseContinuousWorkoutDucking(TrainingUiState.Completed))
        assertFalse(
            shouldUseContinuousWorkoutDucking(
                workout(TrainingPhase.CONCENTRIC, paused = true)
            )
        )
    }

    @Test
    fun startDelayTakesPriorityOverItsUnderlyingRestPhase() {
        assertTrue(
            shouldUseContinuousWorkoutDucking(
                workout(TrainingPhase.REST, inStartDelay = true)
            )
        )
    }

    @Test
    fun executionTicksRequestOnceAndRestReleasesOnce() {
        val fixture = fixture(enabled = true)

        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))
        fixture.session.onStateChanged(workout(TrainingPhase.REST))

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun restToExecutionRequestsFocusAgain() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))

        fixture.session.onStateChanged(workout(TrainingPhase.ECCENTRIC))

        assertEquals(1, fixture.gateway.requestCount)
    }

    @Test
    fun pauseReleasesAndExecutionResumeRequestsAgain() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))

        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC, paused = true))
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))

        assertEquals(2, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun resumeDuringRestDoesNotRequestFocus() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST, paused = true))

        fixture.session.onStateChanged(workout(TrainingPhase.REST))

        assertEquals(0, fixture.gateway.requestCount)
    }

    @Test
    fun startDelayRequestsAndFinishReleases() {
        val fixture = fixture(enabled = true)

        fixture.session.onStateChanged(workout(TrainingPhase.REST, inStartDelay = true))
        fixture.session.onStateChanged(TrainingUiState.Completed)

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun cleanupReleasesFocusLikeServiceDestroy() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.ISOMETRIC))

        fixture.session.cleanup()

        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun disabledSessionNeverRequestsFocus() {
        val fixture = fixture(enabled = false)

        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))
        fixture.session.onUtteranceSubmitted("rest-alert")

        assertEquals(0, fixture.gateway.requestCount)
    }

    @Test
    fun enablingDuringExecutionRequestsAndDisablingReleasesFocusImmediately() {
        val fixture = fixture(enabled = false)
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))

        fixture.session.setEnabled(true)
        assertEquals(1, fixture.gateway.requestCount)

        fixture.session.setEnabled(false)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun enablingOrReenablingDuringRestDoesNotRequestContinuousFocus() {
        val fixture = fixture(enabled = false)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))

        fixture.session.setEnabled(true)
        fixture.session.setEnabled(false)
        fixture.session.setEnabled(true)

        assertEquals(0, fixture.gateway.requestCount)
    }

    @Test
    fun reenablingDuringExecutionReevaluatesAndRequestsFocusAgain() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.ISOMETRIC))
        fixture.session.setEnabled(false)

        fixture.session.setEnabled(true)

        assertEquals(2, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun disablingDuringTransientSpeechReleasesFocusAndClearsItsToken() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        fixture.session.onUtteranceSubmitted("rest-alert")

        fixture.session.setEnabled(false)
        fixture.session.onUtteranceTerminated("rest-alert")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun restUtteranceAcquiresAndDoneReleasesTransientFocus() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }

        utterances.submit("alert", replacesPending = false, onCompleted = null)
        assertEquals(1, fixture.gateway.requestCount)
        utterances.complete("alert")

        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun restUtteranceErrorAndImmediateFailureReleaseTransientFocus() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }

        utterances.submit("error", replacesPending = false, onCompleted = null)
        utterances.fail("error")
        utterances.submit("immediate-failure", replacesPending = false, onCompleted = null)
        utterances.fail("immediate-failure")

        assertEquals(2, fixture.gateway.requestCount)
        assertEquals(2, fixture.gateway.abandonCount)
    }

    @Test
    fun stopAndVoiceReleasePathClearAllTransientFocus() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }
        utterances.submit("first", replacesPending = false, onCompleted = null)
        utterances.submit("second", replacesPending = false, onCompleted = null)

        utterances.stop()
        utterances.stop()

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun queueAddKeepsIndependentTokensUntilBothUtterancesFinish() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST_BETWEEN_EXERCISES))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }
        utterances.submit("number", replacesPending = false, onCompleted = null)
        utterances.submit("rest", replacesPending = false, onCompleted = null)

        utterances.complete("number")
        assertEquals(0, fixture.gateway.abandonCount)
        utterances.complete("rest")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun queueFlushReplacesPendingTokensWithoutDroppingFocusBetweenUtterances() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }
        utterances.submit("old", replacesPending = false, onCompleted = null)

        utterances.submit("replacement", replacesPending = true, onCompleted = null)

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
        utterances.complete("old")
        assertEquals(0, fixture.gateway.abandonCount)
        utterances.complete("replacement")
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun warmupAndCountdownUtterancesDoNotRequestTransientFocus() {
        val fixture = fixture(enabled = true)

        fixture.session.onStateChanged(workout(TrainingPhase.WARMUP))
        fixture.session.onUtteranceSubmitted("warmup")
        fixture.session.onStateChanged(workout(TrainingPhase.COUNTDOWN))
        fixture.session.onUtteranceSubmitted("countdown")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun finalRestCountdownKeepsOneFocusRequestThroughEveryUtteranceAndHandsOffToContinuous() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }

        utterances.beginGroup("final-countdown")
        listOf("three", "two", "one").forEach { utteranceId ->
            utterances.submit(utteranceId, replacesPending = false, onCompleted = null)
            utterances.complete(utteranceId)
            assertEquals(0, fixture.gateway.abandonCount)
        }

        fixture.session.onStateChanged(workout(TrainingPhase.REST, inStartDelay = true))
        utterances.submit("go", replacesPending = false, onCompleted = null)
        utterances.endGroup("final-countdown")
        utterances.complete("go")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun stoppingVoiceDuringGroupedRestCountdownReleasesFocusOnce() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        val utterances = VoiceUtteranceBookkeeper().apply { listener = fixture.session }
        utterances.beginGroup("final-countdown")
        utterances.submit("three", replacesPending = false, onCompleted = null)

        utterances.stop()

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(1, fixture.gateway.abandonCount)
    }

    @Test
    fun executionUtteranceDoesNotDisturbContinuousOwnership() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))

        fixture.session.onUtteranceSubmitted("repetition")
        fixture.session.onUtteranceTerminated("repetition")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun finishingTransientWhileContinuousIsActiveDoesNotAbandonFocus() {
        val fixture = fixture(enabled = true)
        fixture.session.onStateChanged(workout(TrainingPhase.REST))
        fixture.session.onUtteranceSubmitted("alert")
        fixture.session.onStateChanged(workout(TrainingPhase.CONCENTRIC))

        fixture.session.onUtteranceTerminated("alert")

        assertEquals(1, fixture.gateway.requestCount)
        assertEquals(0, fixture.gateway.abandonCount)
    }

    @Test
    fun completedUtteranceRunsItsCallbackOnceAndDuplicateTerminalsAreIgnored() {
        val events = RecordingUtteranceListener()
        val utterances = VoiceUtteranceBookkeeper().apply { listener = events }
        var completionCount = 0
        utterances.submit("voice", replacesPending = false) { completionCount += 1 }

        utterances.complete("voice")
        utterances.complete("voice")
        utterances.fail("voice")

        assertEquals(1, completionCount)
        assertEquals(listOf("submitted:voice", "terminated:voice"), events.events)
    }

    private fun fixture(enabled: Boolean): FocusFixture {
        val gateway = IntegrationAudioFocusGateway()
        val controller = WorkoutAudioFocusController(gateway, enabled)
        return FocusFixture(WorkoutAudioFocusSession(controller), gateway)
    }

    private fun workout(
        phase: TrainingPhase,
        paused: Boolean = false,
        inStartDelay: Boolean = false
    ): TrainingUiState.Workout {
        val exercise = Exercise("focus", "Focus", 1, 1, 1, 1, 1)
        val segment = PlannedWorkoutSegment(
            type = if (inStartDelay) {
                PlannedWorkoutSegmentType.START_DELAY
            } else {
                PlannedWorkoutSegmentType.CONCENTRIC
            },
            durationSeconds = 1
        )
        return TrainingUiState.Workout(
            routine = Routine("focus", "Focus", false, listOf(exercise), 1),
            exerciseIndex = 0,
            seriesNumber = 1,
            repetitionNumber = 1,
            phase = phase,
            secondsRemaining = 1,
            phaseDurationSeconds = 1,
            phaseStartedAtMillis = 0L,
            phasePausedAtMillis = if (paused) 0L else null,
            isPaused = paused,
            currentExerciseNotes = "",
            isStartingExecution = inStartDelay,
            plannedTimeline = PlannedWorkoutTimeline(listOf(segment))
        )
    }

    private data class FocusFixture(
        val session: WorkoutAudioFocusSession,
        val gateway: IntegrationAudioFocusGateway
    )
}

private class IntegrationAudioFocusGateway : WorkoutAudioFocusGateway {
    var requestCount = 0
    var abandonCount = 0

    override fun requestFocus(onPermanentFocusLoss: () -> Unit): Boolean {
        requestCount += 1
        return true
    }

    override fun abandonFocus() {
        abandonCount += 1
    }
}

private class RecordingUtteranceListener : VoiceUtteranceLifecycleListener {
    val events = mutableListOf<String>()

    override fun onUtteranceSubmitted(utteranceId: String) {
        events += "submitted:$utteranceId"
    }

    override fun onUtteranceTerminated(utteranceId: String) {
        events += "terminated:$utteranceId"
    }
}
