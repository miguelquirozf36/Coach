package com.miguel.coach

import android.content.SharedPreferences

interface RoutineStorage {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
}

class SharedPreferencesRoutineStorage(
    private val preferences: SharedPreferences
) : RoutineStorage {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String): Boolean =
        preferences.edit().putString(key, value).commit()
}

class RoutineRepository(
    private val storage: RoutineStorage,
    private val seedRoutines: List<Routine> = Routines.all
) {
    fun load(): List<Routine> {
        val primaryJson = storage.read(ROUTINES_JSON)
        decodeAndValidate(primaryJson)?.let { return it }

        val backupJson = storage.read(ROUTINES_BACKUP_JSON)
        decodeAndValidate(backupJson)?.let { recoveredRoutines ->
            storage.write(ROUTINES_JSON, backupJson!!)
            return recoveredRoutines
        }

        if (primaryJson == null && backupJson == null) {
            save(seedRoutines)
        }
        return seedRoutines
    }

    fun save(routines: List<Routine>): Boolean {
        if (!RoutineValidator.isValid(routines)) return false
        val newJson = RoutineJsonCodec.encode(routines)

        val currentPrimary = storage.read(ROUTINES_JSON)
        if (decodeAndValidate(currentPrimary) != null &&
            !storage.write(ROUTINES_BACKUP_JSON, currentPrimary!!)
        ) {
            return false
        }

        if (!storage.write(ROUTINES_JSON, newJson)) {
            restoreBackup()
            return false
        }

        val savedRoutines = decodeAndValidate(storage.read(ROUTINES_JSON))
        if (savedRoutines == routines) return true

        restoreBackup()
        return false
    }

    private fun restoreBackup() {
        val backupJson = storage.read(ROUTINES_BACKUP_JSON)
        if (decodeAndValidate(backupJson) != null) {
            storage.write(ROUTINES_JSON, backupJson!!)
        }
    }

    private fun decodeAndValidate(json: String?): List<Routine>? =
        json?.let(RoutineJsonCodec::decode)?.takeIf(RoutineValidator::isValid)

    private companion object {
        const val ROUTINES_JSON = "routines_json"
        const val ROUTINES_BACKUP_JSON = "routines_backup_json"
    }
}

private object RoutineValidator {
    fun isValid(routines: List<Routine>): Boolean {
        if (routines.isEmpty()) return false
        val identifiers = mutableSetOf<String>()
        return routines.all { routine ->
            routine.id.isNotBlank() &&
                routine.name.isNotBlank() &&
                routine.exercises.isNotEmpty() &&
                routine.restBetweenExercisesSeconds >= 0 &&
                identifiers.add(routine.id) &&
                routine.exercises.all { exercise ->
                    exercise.id.isNotBlank() &&
                        exercise.name.isNotBlank() &&
                        exercise.sets >= 1 &&
                        exercise.repetitions >= 1 &&
                        exercise.concentricSeconds >= 0 &&
                        exercise.eccentricSeconds >= 0 &&
                        exercise.restSeconds >= 0 &&
                        identifiers.add(exercise.id)
                }
        }
    }
}

private object RoutineJsonCodec {
    fun encode(routines: List<Routine>): String = buildString {
        append("{\"routines\":[")
        routines.forEachIndexed { index, routine ->
            if (index > 0) append(',')
            append("{\"id\":")
            appendQuoted(routine.id)
            append(",\"name\":")
            appendQuoted(routine.name)
            append(",\"isCustom\":${routine.isCustom}")
            append(",\"restBetweenExercisesSeconds\":${routine.restBetweenExercisesSeconds}")
            append(",\"exercises\":[")
            routine.exercises.forEachIndexed { exerciseIndex, exercise ->
                if (exerciseIndex > 0) append(',')
                append("{\"id\":")
                appendQuoted(exercise.id)
                append(",\"name\":")
                appendQuoted(exercise.name)
                append(",\"sets\":${exercise.sets}")
                append(",\"repetitions\":${exercise.repetitions}")
                append(",\"concentricSeconds\":${exercise.concentricSeconds}")
                append(",\"eccentricSeconds\":${exercise.eccentricSeconds}")
                append(",\"restSeconds\":${exercise.restSeconds}}")
            }
            append("]}")
        }
        append("]}")
    }

    fun decode(json: String): List<Routine>? = try {
        val root = JsonParser(json).parse() as? JsonValue.ObjectValue ?: return null
        val routines = root.values["routines"] as? JsonValue.ArrayValue ?: return null
        routines.values.map(::routineFromJson)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun routineFromJson(value: JsonValue): Routine {
        val fields = (value as? JsonValue.ObjectValue)?.values ?: throw IllegalArgumentException()
        val exercises = (fields.required("exercises") as? JsonValue.ArrayValue)?.values
            ?.map(::exerciseFromJson) ?: throw IllegalArgumentException()
        return Routine(
            id = fields.requiredString("id"),
            name = fields.requiredString("name"),
            isCustom = fields.requiredBoolean("isCustom"),
            exercises = exercises,
            restBetweenExercisesSeconds = fields.requiredInt("restBetweenExercisesSeconds")
        )
    }

    private fun exerciseFromJson(value: JsonValue): Exercise {
        val fields = (value as? JsonValue.ObjectValue)?.values ?: throw IllegalArgumentException()
        return Exercise(
            id = fields.requiredString("id"),
            name = fields.requiredString("name"),
            sets = fields.requiredInt("sets"),
            repetitions = fields.requiredInt("repetitions"),
            concentricSeconds = fields.requiredInt("concentricSeconds"),
            eccentricSeconds = fields.requiredInt("eccentricSeconds"),
            restSeconds = fields.requiredInt("restSeconds")
        )
    }

    private fun MutableMap<String, JsonValue>.required(name: String): JsonValue =
        this[name] ?: throw IllegalArgumentException()

    private fun MutableMap<String, JsonValue>.requiredString(name: String): String =
        (required(name) as? JsonValue.StringValue)?.value ?: throw IllegalArgumentException()

    private fun MutableMap<String, JsonValue>.requiredBoolean(name: String): Boolean =
        (required(name) as? JsonValue.BooleanValue)?.value ?: throw IllegalArgumentException()

    private fun MutableMap<String, JsonValue>.requiredInt(name: String): Int =
        (required(name) as? JsonValue.NumberValue)?.value?.toIntOrNull() ?: throw IllegalArgumentException()

    private fun StringBuilder.appendQuoted(value: String) {
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
                else -> {
                    if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
                }
            }
        }
        append('"')
    }
}

private sealed interface JsonValue {
    data class ObjectValue(val values: MutableMap<String, JsonValue>) : JsonValue
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
}

private class JsonParser(private val source: String) {
    private var position = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        if (position != source.length) throw IllegalArgumentException()
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.StringValue(parseString())
            't' -> parseLiteral("true", JsonValue.BooleanValue(true))
            'f' -> parseLiteral("false", JsonValue.BooleanValue(false))
            '-', in '0'..'9' -> JsonValue.NumberValue(parseNumber())
            else -> throw IllegalArgumentException()
        }
    }

    private fun parseObject(): JsonValue.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = mutableMapOf<String, JsonValue>()
        if (consume('}')) return JsonValue.ObjectValue(values)
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            if (values.put(key, value) != null) throw IllegalArgumentException()
            skipWhitespace()
            if (consume('}')) return JsonValue.ObjectValue(values)
            expect(',')
        }
    }

    private fun parseArray(): JsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (consume(']')) return JsonValue.ArrayValue(values)
        while (true) {
            values += parseValue()
            skipWhitespace()
            if (consume(']')) return JsonValue.ArrayValue(values)
            expect(',')
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (position < source.length) {
            when (val character = source[position++]) {
                '"' -> return result.toString()
                '\\' -> result.append(parseEscape())
                else -> {
                    if (character.code < 0x20) throw IllegalArgumentException()
                    result.append(character)
                }
            }
        }
        throw IllegalArgumentException()
    }

    private fun parseEscape(): Char = when (val escape = next()) {
        '"', '\\', '/' -> escape
        'b' -> '\b'
        'f' -> '\u000C'
        'n' -> '\n'
        'r' -> '\r'
        't' -> '\t'
        'u' -> {
            val hexadecimal = source.substring(position, (position + 4).coerceAtMost(source.length))
            if (hexadecimal.length != 4 || hexadecimal.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) {
                throw IllegalArgumentException()
            }
            position += 4
            hexadecimal.toInt(16).toChar()
        }
        else -> throw IllegalArgumentException()
    }

    private fun parseNumber(): String {
        val start = position
        consume('-')
        if (consume('0')) {
            if (peekOrNull()?.isDigit() == true) throw IllegalArgumentException()
        } else {
            requireDigit()
            while (peekOrNull()?.isDigit() == true) position++
        }
        if (consume('.')) {
            requireDigit()
            while (peekOrNull()?.isDigit() == true) position++
        }
        if (consume('e') || consume('E')) {
            consume('+') || consume('-')
            requireDigit()
            while (peekOrNull()?.isDigit() == true) position++
        }
        return source.substring(start, position)
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        if (!source.regionMatches(position, literal, 0, literal.length)) throw IllegalArgumentException()
        position += literal.length
        return value
    }

    private fun requireDigit() {
        if (peekOrNull()?.isDigit() != true) throw IllegalArgumentException()
        position++
    }

    private fun expect(character: Char) {
        if (!consume(character)) throw IllegalArgumentException()
    }

    private fun consume(character: Char): Boolean {
        if (peekOrNull() != character) return false
        position++
        return true
    }

    private fun peek(): Char = peekOrNull() ?: throw IllegalArgumentException()

    private fun peekOrNull(): Char? = source.getOrNull(position)

    private fun next(): Char = source.getOrNull(position++) ?: throw IllegalArgumentException()

    private fun skipWhitespace() {
        while (peekOrNull()?.isWhitespace() == true) position++
    }
}
