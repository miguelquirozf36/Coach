package com.miguel.coach

internal const val INITIAL_COUNTDOWN_SECONDS = 10
internal const val START_DELAY_SECONDS = 1

enum class PlannedWorkoutSegmentType {
    WARMUP,
    INITIAL_COUNTDOWN,
    START_DELAY,
    CONCENTRIC,
    ECCENTRIC,
    ISOMETRIC_SHORTENED,
    ISOMETRIC_STRETCHED,
    REST,
    REST_BETWEEN_EXERCISES
}

data class PlannedWorkoutSegment(
    val type: PlannedWorkoutSegmentType,
    val durationSeconds: Int,
    val exerciseIndex: Int? = null,
    val seriesNumber: Int? = null,
    val repetitionNumber: Int? = null,
    val side: ExerciseSide? = null
)

data class PlannedWorkoutTimeline(val segments: List<PlannedWorkoutSegment>) {
    val totalDurationSeconds: Long
        get() = segments.sumOf { it.durationSeconds.toLong() }

    fun remainingDurationSeconds(fromSegmentIndex: Int): Long =
        segments.drop(fromSegmentIndex.coerceIn(0, segments.size)).sumOf { it.durationSeconds.toLong() }
}

fun Routine.plannedTimeline(): PlannedWorkoutTimeline =
    buildPlannedTimeline(startExerciseIndex = 0, includeRoutineWarmup = true)

fun Routine.plannedTimelineFromExercise(startExerciseIndex: Int): PlannedWorkoutTimeline =
    buildPlannedTimeline(startExerciseIndex, includeRoutineWarmup = false)

fun Routine.plannedDurationSeconds(): Long = plannedTimeline().totalDurationSeconds

fun Routine.plannedDurationSecondsFromExercise(startExerciseIndex: Int): Long =
    plannedTimelineFromExercise(startExerciseIndex).totalDurationSeconds

private fun Routine.buildPlannedTimeline(
    startExerciseIndex: Int,
    includeRoutineWarmup: Boolean
): PlannedWorkoutTimeline {
    if (startExerciseIndex !in exercises.indices) return PlannedWorkoutTimeline(emptyList())

    val segments = mutableListOf<PlannedWorkoutSegment>()
    segments += PlannedWorkoutSegment(
        type = if (includeRoutineWarmup && warmupSeconds > 0) {
            PlannedWorkoutSegmentType.WARMUP
        } else {
            PlannedWorkoutSegmentType.INITIAL_COUNTDOWN
        },
        durationSeconds = if (includeRoutineWarmup && warmupSeconds > 0) {
            warmupSeconds
        } else {
            INITIAL_COUNTDOWN_SECONDS
        }
    )
    segments += startDelaySegment(startExerciseIndex, seriesNumber = 1, side = exercises[startExerciseIndex].firstSide())

    exercises.indices.drop(startExerciseIndex).forEach { exerciseIndex ->
        val exercise = exercises[exerciseIndex]
        val executions = exercise.executions()
        executions.forEachIndexed { executionIndex, execution ->
            repeat(exercise.repetitions) { repetitionIndex ->
                val repetitionNumber = repetitionIndex + 1
                segments += exerciseSegment(
                    PlannedWorkoutSegmentType.CONCENTRIC,
                    exercise.concentricSeconds,
                    exerciseIndex,
                    execution,
                    repetitionNumber
                )
                if (repetitionNumber < exercise.repetitions) {
                    if (exercise.isometricPauseMode == IsometricPauseMode.SHORTENED) {
                        segments += exerciseSegment(
                            PlannedWorkoutSegmentType.ISOMETRIC_SHORTENED,
                            exercise.isometricDurationSeconds,
                            exerciseIndex,
                            execution,
                            repetitionNumber
                        )
                    }
                    segments += exerciseSegment(
                        PlannedWorkoutSegmentType.ECCENTRIC,
                        exercise.eccentricSeconds,
                        exerciseIndex,
                        execution,
                        repetitionNumber
                    )
                    if (exercise.isometricPauseMode == IsometricPauseMode.STRETCHED) {
                        segments += exerciseSegment(
                            PlannedWorkoutSegmentType.ISOMETRIC_STRETCHED,
                            exercise.isometricDurationSeconds,
                            exerciseIndex,
                            execution,
                            repetitionNumber
                        )
                    }
                }
            }

            if (executionIndex < executions.lastIndex) {
                val nextExecution = executions[executionIndex + 1]
                segments += exerciseSegment(
                    PlannedWorkoutSegmentType.REST,
                    exercise.restSeconds,
                    exerciseIndex,
                    execution,
                    exercise.repetitions
                )
                segments += startDelaySegment(
                    exerciseIndex,
                    nextExecution.seriesNumber,
                    nextExecution.side
                )
            }
        }

        if (exerciseIndex < exercises.lastIndex) {
            segments += PlannedWorkoutSegment(
                type = PlannedWorkoutSegmentType.REST_BETWEEN_EXERCISES,
                durationSeconds = restBetweenExercisesSeconds,
                exerciseIndex = exerciseIndex
            )
            val nextExerciseIndex = exerciseIndex + 1
            segments += startDelaySegment(
                nextExerciseIndex,
                seriesNumber = 1,
                side = exercises[nextExerciseIndex].firstSide()
            )
        }
    }
    return PlannedWorkoutTimeline(segments)
}

private data class PlannedExecution(val seriesNumber: Int, val side: ExerciseSide?)

private fun Exercise.executions(): List<PlannedExecution> = buildList {
    for (seriesNumber in 1..sets) {
        if (executionMode == ExerciseExecutionMode.ONE_SIDE_AT_A_TIME) {
            add(PlannedExecution(seriesNumber, ExerciseSide.RIGHT))
            add(PlannedExecution(seriesNumber, ExerciseSide.LEFT))
        } else {
            add(PlannedExecution(seriesNumber, null))
        }
    }
}

private fun Exercise.firstSide(): ExerciseSide? =
    ExerciseSide.RIGHT.takeIf { executionMode == ExerciseExecutionMode.ONE_SIDE_AT_A_TIME }

private fun exerciseSegment(
    type: PlannedWorkoutSegmentType,
    durationSeconds: Int,
    exerciseIndex: Int,
    execution: PlannedExecution,
    repetitionNumber: Int
) = PlannedWorkoutSegment(
    type = type,
    durationSeconds = durationSeconds,
    exerciseIndex = exerciseIndex,
    seriesNumber = execution.seriesNumber,
    repetitionNumber = repetitionNumber,
    side = execution.side
)

private fun startDelaySegment(exerciseIndex: Int, seriesNumber: Int, side: ExerciseSide?) =
    PlannedWorkoutSegment(
        type = PlannedWorkoutSegmentType.START_DELAY,
        durationSeconds = START_DELAY_SECONDS,
        exerciseIndex = exerciseIndex,
        seriesNumber = seriesNumber,
        side = side
    )
