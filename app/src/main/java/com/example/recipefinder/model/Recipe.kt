package com.example.recipefinder.model

data class Recipe(
    val id: String,
    val title: String,
    val imageUrl: String,
    val ingredients: List<String>,
    val instructions: String,
    val cookTime: Int,
    val servings: Int,
    val missingIngredients: List<String> = emptyList()
)