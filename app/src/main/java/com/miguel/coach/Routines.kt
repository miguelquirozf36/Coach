package com.miguel.coach

import androidx.annotation.StringRes

data class Routine(
    val id: String,
    @param:StringRes val nameRes: Int,
    val exercises: List<Exercise>
)

data class Exercise(
    @param:StringRes val nameRes: Int,
    val sets: Int,
    val repetitions: Int,
    val concentricDurationMillis: Long,
    val eccentricDurationMillis: Long,
    val restDurationMillis: Long
)

object Routines {
    val all = listOf(
        Routine(
            id = "full_body",
            nameRes = R.string.routine_full_body,
            exercises = listOf(
                Exercise(
                    nameRes = R.string.exercise_squat,
                    sets = 3,
                    repetitions = 8,
                    concentricDurationMillis = 1_000,
                    eccentricDurationMillis = 3_000,
                    restDurationMillis = 60_000
                ),
                Exercise(
                    nameRes = R.string.exercise_bench_press,
                    sets = 3,
                    repetitions = 8,
                    concentricDurationMillis = 1_000,
                    eccentricDurationMillis = 3_000,
                    restDurationMillis = 60_000
                ),
                Exercise(
                    nameRes = R.string.exercise_row,
                    sets = 3,
                    repetitions = 8,
                    concentricDurationMillis = 1_000,
                    eccentricDurationMillis = 3_000,
                    restDurationMillis = 60_000
                )
            )
        ),
        Routine(
            id = "upper_body",
            nameRes = R.string.routine_upper_body,
            exercises = listOf(
                Exercise(
                    nameRes = R.string.exercise_bench_press,
                    sets = 4,
                    repetitions = 10,
                    concentricDurationMillis = 1_000,
                    eccentricDurationMillis = 3_000,
                    restDurationMillis = 60_000
                ),
                Exercise(
                    nameRes = R.string.exercise_row,
                    sets = 4,
                    repetitions = 10,
                    concentricDurationMillis = 1_000,
                    eccentricDurationMillis = 3_000,
                    restDurationMillis = 60_000
                )
            )
        )
    )
}
