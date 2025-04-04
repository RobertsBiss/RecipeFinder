package com.example.recipefinder.model

data class Recipe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val imageUrl: String,
    val ingredients: List<String>,
    val instructions: String,
    val cookTime: Int,
    val servings: Int,
    val dietaryTags: Set<String> = setOf(), // e.g., "Vegan", "Vegetarian", etc.
    val allergens: Set<String> = setOf(), // e.g., "Peanuts", "Wheat", etc.
    val cuisineType: String = "", // e.g., "Italian", "Japanese", etc.
    val difficulty: String = "Medium", // "Easy", "Medium", or "Hard"
    val missingIngredients: List<String> = emptyList()
)