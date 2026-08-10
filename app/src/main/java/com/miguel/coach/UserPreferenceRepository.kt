package com.miguel.coach

import android.content.SharedPreferences
import androidx.core.content.edit

const val MAX_USER_NAME_LENGTH = 40

interface UserPreferenceStorage {
    fun readUserName(): String?
    fun writeUserName(name: String): Boolean
    fun readTourCompleted(): Boolean? = null
    fun writeTourCompleted(completed: Boolean): Boolean = false
}

class SharedPreferencesUserStorage(
    private val preferences: SharedPreferences
) : UserPreferenceStorage {
    override fun readUserName(): String? = preferences.getString(USER_NAME, null)

    override fun writeUserName(name: String): Boolean = preferences.edit().putString(USER_NAME, name).commit()

    override fun readTourCompleted(): Boolean? =
        if (preferences.contains(TOUR_COMPLETED)) preferences.getBoolean(TOUR_COMPLETED, false) else null

    override fun writeTourCompleted(completed: Boolean): Boolean =
        preferences.edit().putBoolean(TOUR_COMPLETED, completed).commit()

    private companion object {
        const val USER_NAME = "user_name"
        const val TOUR_COMPLETED = "onboarding_tour_completed_v17"
    }
}

data class UserNameValidation(
    val value: String? = null,
    val message: String? = null,
    val saved: Boolean = false
)

fun validateUserName(input: String): UserNameValidation {
    val trimmed = input.trim()
    if (input.isNotEmpty() && trimmed.isEmpty()) {
        return UserNameValidation(message = "El nombre no puede contener solo espacios.")
    }
    val normalized = trimmed
        .split(Regex("\\s+"))
        .filter(String::isNotEmpty)
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { character -> character.titlecase() }
        }
    if (normalized.length > MAX_USER_NAME_LENGTH) {
        return UserNameValidation(message = "El nombre no puede superar los 40 caracteres.")
    }
    return UserNameValidation(value = normalized)
}

class UserPreferenceRepository(private val storage: UserPreferenceStorage) {
    fun loadUserName(): String = storage.readUserName().orEmpty().trim().take(MAX_USER_NAME_LENGTH)

    fun saveUserName(input: String): UserNameValidation {
        val validation = validateUserName(input)
        val value = validation.value ?: return validation
        return if (storage.writeUserName(value)) {
            validation.copy(saved = true)
        } else {
            UserNameValidation(message = "No se pudo guardar el nombre.")
        }
    }

    /** Initializes the v1.7 marker once. Existing installations are migrated as completed. */
    fun initializeOnboarding(existingInstallation: Boolean): Boolean {
        val stored = storage.readTourCompleted()
        if (stored != null) return !stored
        val completed = existingInstallation
        storage.writeTourCompleted(completed)
        return !completed
    }

    fun isTourCompleted(): Boolean = storage.readTourCompleted() == true

    fun completeTour(): Boolean = storage.writeTourCompleted(true)
}

fun homeGreeting(userName: String): List<String> = buildList {
    userName.trim().takeIf(String::isNotEmpty)?.let { add("Hola, $it") }
    add("¿Qué quieres entrenar hoy?")
}
