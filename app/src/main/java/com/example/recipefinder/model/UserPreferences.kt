package com.example.recipefinder.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context

data class UserPreferences(
    val dietaryPreferences: Set<String> = setOf(),
    val allergies: Set<String> = setOf(),
    val cuisinePreferences: Set<String> = setOf(),
    val difficultyPreference: Set<String> = setOf()
) {
    companion object {
        fun getUserPreferences(context: Context): UserPreferences {
            val sharedPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPrefs.getString("preferences", null)
            
            return if (json != null) {
                val type = object : TypeToken<UserPreferences>() {}.type
                gson.fromJson(json, type)
            } else {
                UserPreferences()
            }
        }
    }

    fun hasAnyPreferences(): Boolean {
        return dietaryPreferences.isNotEmpty() ||
               allergies.isNotEmpty() ||
               cuisinePreferences.isNotEmpty() ||
               difficultyPreference.isNotEmpty()
    }
} 