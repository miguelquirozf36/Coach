package com.miguel.coach

fun workoutOverallProgress(state: TrainingUiState, nowMillis: Long): Float = when (state) {
    TrainingUiState.Completed -> 1f
    TrainingUiState.Home -> 0f
    is TrainingUiState.Workout -> workoutOverallProgress(state, nowMillis)
}

fun workoutOverallProgress(state: TrainingUiState.Workout, nowMillis: Long): Float {
    val timeline = state.plannedTimeline
    val totalMillis = timeline.totalDurationSeconds * MILLIS_PER_SECOND
    if (totalMillis <= 0L) return 0f

    val segmentIndex = state.plannedSegmentIndex.coerceIn(0, timeline.segments.size)
    val completedMillis = timeline.segments
        .take(segmentIndex)
        .sumOf { it.durationSeconds.toLong() * MILLIS_PER_SECOND }
    val currentSegmentMillis = timeline.segments.getOrNull(segmentIndex)
        ?.durationSeconds
        ?.toLong()
        ?.times(MILLIS_PER_SECOND)
        ?: 0L
    val effectiveNowMillis = state.plannedSegmentPausedAtMillis ?: nowMillis
    val elapsedInSegmentMillis = (effectiveNowMillis - state.plannedSegmentStartedAtMillis)
        .coerceIn(0L, currentSegmentMillis)

    return ((completedMillis + elapsedInSegmentMillis).toDouble() / totalMillis.toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

private const val MILLIS_PER_SECOND = 1_000L
