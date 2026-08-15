package com.miguel.coach

import android.content.SharedPreferences

data class SeriesLoadRecord(
    val exerciseId: String,
    val seriesNumber: Int,
    val load: String
)

data class WorkoutLoadSession(
    val completedAtMillis: Long,
    val seriesLoads: List<SeriesLoadRecord>
)

interface WorkoutLoadHistory {
    fun previousLoad(exerciseId: String): String?
    fun saveCompletedSession(seriesLoads: List<SeriesLoadRecord>): Boolean
}

interface WorkoutLoadStorage {
    fun read(): String?
    fun write(value: String): Boolean
}

class SharedPreferencesWorkoutLoadStorage(
    private val preferences: SharedPreferences
) : WorkoutLoadStorage {
    override fun read(): String? = preferences.getString(WORKOUT_LOAD_SESSIONS, null)

    override fun write(value: String): Boolean =
        preferences.edit().putString(WORKOUT_LOAD_SESSIONS, value).commit()

    private companion object {
        const val WORKOUT_LOAD_SESSIONS = "workout_load_sessions_json"
    }
}

class WorkoutLoadRepository(
    private val storage: WorkoutLoadStorage,
    private val wallClockMillis: () -> Long = System::currentTimeMillis
) : WorkoutLoadHistory {
    override fun previousLoad(exerciseId: String): String? = loadSessions()
        .asReversed()
        .firstNotNullOfOrNull { session ->
            session.seriesLoads.lastOrNull { it.exerciseId == exerciseId && it.load.isNotBlank() }?.load
        }

    override fun saveCompletedSession(seriesLoads: List<SeriesLoadRecord>): Boolean {
        if (seriesLoads.isEmpty()) return true
        val sessions = loadSessions() + WorkoutLoadSession(wallClockMillis(), seriesLoads.toList())
        return storage.write(WorkoutLoadJsonCodec.encode(sessions))
    }

    fun loadSessions(): List<WorkoutLoadSession> =
        storage.read()?.let(WorkoutLoadJsonCodec::decode).orEmpty()
}

private object EmptyWorkoutLoadHistory : WorkoutLoadHistory {
    override fun previousLoad(exerciseId: String): String? = null
    override fun saveCompletedSession(seriesLoads: List<SeriesLoadRecord>): Boolean = true
}

internal val NoWorkoutLoadHistory: WorkoutLoadHistory = EmptyWorkoutLoadHistory

internal object WorkoutLoadJsonCodec {
    fun encode(sessions: List<WorkoutLoadSession>): String = buildString {
        append("{\"sessions\":[")
        sessions.forEachIndexed { sessionIndex, session ->
            if (sessionIndex > 0) append(',')
            append("{\"completedAtMillis\":${session.completedAtMillis},\"seriesLoads\":[")
            session.seriesLoads.forEachIndexed { loadIndex, record ->
                if (loadIndex > 0) append(',')
                append("{\"exerciseId\":")
                appendJsonString(record.exerciseId)
                append(",\"seriesNumber\":${record.seriesNumber},\"load\":")
                appendJsonString(record.load)
                append('}')
            }
            append("]}")
        }
        append("]}")
    }

    fun decode(json: String): List<WorkoutLoadSession>? = try {
        val root = JsonParser(json).parse() as? JsonValue.ObjectValue ?: return null
        val sessions = root.values["sessions"] as? JsonValue.ArrayValue ?: return null
        sessions.values.map(::decodeSession)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decodeSession(value: JsonValue): WorkoutLoadSession {
        val fields = (value as? JsonValue.ObjectValue)?.values ?: throw IllegalArgumentException()
        val completedAt = (fields["completedAtMillis"] as? JsonValue.NumberValue)
            ?.value?.toLongOrNull() ?: throw IllegalArgumentException()
        val loads = (fields["seriesLoads"] as? JsonValue.ArrayValue)
            ?.values?.map(::decodeRecord) ?: throw IllegalArgumentException()
        return WorkoutLoadSession(completedAt, loads)
    }

    private fun decodeRecord(value: JsonValue): SeriesLoadRecord {
        val fields = (value as? JsonValue.ObjectValue)?.values ?: throw IllegalArgumentException()
        return SeriesLoadRecord(
            exerciseId = (fields["exerciseId"] as? JsonValue.StringValue)?.value
                ?: throw IllegalArgumentException(),
            seriesNumber = (fields["seriesNumber"] as? JsonValue.NumberValue)
                ?.value?.toIntOrNull() ?: throw IllegalArgumentException(),
            load = (fields["load"] as? JsonValue.StringValue)?.value
                ?: throw IllegalArgumentException()
        )
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }
}
