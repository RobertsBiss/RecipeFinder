package com.example.recipefinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recipefinder.model.UserPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsFragment : Fragment() {
    private lateinit var dietaryPreferencesGroup: ChipGroup
    private lateinit var allergiesGroup: ChipGroup
    private lateinit var cuisineGroup: ChipGroup
    private lateinit var difficultyGroup: ChipGroup
    private lateinit var saveButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dietaryPreferencesGroup = view.findViewById(R.id.dietaryPreferencesGroup)
        allergiesGroup = view.findViewById(R.id.allergiesGroup)
        cuisineGroup = view.findViewById(R.id.cuisineGroup)
        difficultyGroup = view.findViewById(R.id.difficultyGroup)
        saveButton = view.findViewById(R.id.btnSaveSettings)

        makeChipsCheckable(dietaryPreferencesGroup)
        makeChipsCheckable(allergiesGroup)
        makeChipsCheckable(cuisineGroup)
        makeChipsCheckable(difficultyGroup)

        loadSavedPreferences()

        // Setup save button
        saveButton.setOnClickListener {
            savePreferences()
            Toast.makeText(context, "Preferences saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeChipsCheckable(chipGroup: ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.isCheckable = true
        }
    }

    private fun loadSavedPreferences() {
        val sharedPrefs = requireContext().getSharedPreferences("user_preferences", android.content.Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("preferences", null)
        
        if (json != null) {
            val type = object : TypeToken<UserPreferences>() {}.type
            val preferences: UserPreferences = gson.fromJson(json, type)
            
            // Apply saved preferences to chips
            applyPreferencesToChipGroup(dietaryPreferencesGroup, preferences.dietaryPreferences)
            applyPreferencesToChipGroup(allergiesGroup, preferences.allergies)
            applyPreferencesToChipGroup(cuisineGroup, preferences.cuisinePreferences)
            applyPreferencesToChipGroup(difficultyGroup, preferences.difficultyPreference)
        }
    }

    private fun applyPreferencesToChipGroup(chipGroup: ChipGroup, selectedItems: Set<String>) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip != null) {
                chip.isChecked = selectedItems.contains(chip.text.toString())
            }
        }
    }

    private fun savePreferences() {
        val preferences = UserPreferences(
            dietaryPreferences = getSelectedChips(dietaryPreferencesGroup),
            allergies = getSelectedChips(allergiesGroup),
            cuisinePreferences = getSelectedChips(cuisineGroup),
            difficultyPreference = getSelectedChips(difficultyGroup)
        )

        val sharedPrefs = requireContext().getSharedPreferences("user_preferences", android.content.Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(preferences)
        
        sharedPrefs.edit().putString("preferences", json).apply()
    }

    private fun getSelectedChips(chipGroup: ChipGroup): Set<String> {
        val selected = mutableSetOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selected.add(chip.text.toString())
            }
        }
        return selected
    }
}