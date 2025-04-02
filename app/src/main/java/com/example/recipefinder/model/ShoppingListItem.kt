package com.example.recipefinder.model

data class ShoppingListItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val ingredient: String,
    val isChecked: Boolean = false,
    val recipeId: String,
    val recipeTitle: String
)