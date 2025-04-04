package com.example.recipefinder.model

import android.content.Context
import android.util.Log
import com.example.recipefinder.util.DatabaseHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RecipeRepository(private val context: Context) {
    private val databaseHelper = DatabaseHelper(context)
    private val API_BASE_URL = "https://api.spoonacular.com/recipes"
    private val sharedPreferences = context.getSharedPreferences("recipes", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun searchRecipesByIngredients(ingredients: List<String>): List<Recipe> = withContext(Dispatchers.IO) {
        val userPreferences = UserPreferences.getUserPreferences(context)
        val allRecipes = getAllRecipes()
        
        // Filter out recipes with allergens
        val filteredRecipes = allRecipes.filter { recipe ->
            recipe.allergens.intersect(userPreferences.allergies).isEmpty()
        }

        // Score and sort recipes based on preferences
        val scoredRecipes = filteredRecipes.map { recipe ->
            val score = calculateRecipeScore(recipe, ingredients, userPreferences)
            recipe to score
        }.sortedByDescending { it.second }

        scoredRecipes.map { it.first }
    }

    private fun calculateRecipeScore(
        recipe: Recipe,
        userIngredients: List<String>,
        preferences: UserPreferences
    ): Int {
        var score = 0

        // Base score: number of matching ingredients
        val matchingIngredients = recipe.ingredients.count { ingredient ->
            userIngredients.any { userIngredient ->
                ingredient.contains(userIngredient, ignoreCase = true)
            }
        }
        score += matchingIngredients * 10

        // Dietary preferences bonus
        if (recipe.dietaryTags.intersect(preferences.dietaryPreferences).isNotEmpty()) {
            score += 30
        }

        // Cuisine preference bonus
        if (preferences.cuisinePreferences.contains(recipe.cuisineType)) {
            score += 20
        }

        // Difficulty preference bonus
        if (preferences.difficultyPreference.contains(recipe.difficulty)) {
            score += 15
        }

        return score
    }

    suspend fun getRecipeDetails(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "$API_BASE_URL/$recipeId/information?apiKey=89b1e02aa1064bb69925ab953ccb8b47"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    parseRecipeDetailsJson(response.toString())
                } else {
                    Log.e("RecipeRepository", "Error: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Exception: ${e.message}")
                null
            }
        }
    }

    private fun parseRecipesJson(jsonString: String): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val recipeJson = jsonArray.getJSONObject(i)
                val recipe = Recipe(
                    id = recipeJson.getInt("id").toString(),
                    title = recipeJson.getString("title"),
                    imageUrl = recipeJson.getString("image"),
                    cookTime = 0, // Will be updated with complete recipe details
                    servings = 0, // Will be updated with complete recipe details
                    ingredients = emptyList(), // Will be updated with complete recipe details
                    instructions = "" // Will be updated with complete recipe details
                )
                recipes.add(recipe)
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error parsing recipes JSON: ${e.message}")
        }
        return recipes
    }

    private fun parseRecipeDetailsJson(jsonString: String): Recipe? {
        try {
            val jsonObject = JSONObject(jsonString)
            return Recipe(
                id = jsonObject.getInt("id").toString(),
                title = jsonObject.getString("title"),
                imageUrl = jsonObject.getString("image"),
                cookTime = jsonObject.getInt("readyInMinutes"),
                servings = jsonObject.getInt("servings"),
                ingredients = parseIngredients(jsonObject.getJSONArray("extendedIngredients")),
                instructions = parseInstructions(jsonObject.getJSONArray("analyzedInstructions"))
            )
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error parsing recipe details JSON: ${e.message}")
            return null
        }
    }

    private fun parseIngredients(ingredientsArray: JSONArray): List<String> {
        val ingredients = mutableListOf<String>()
        for (i in 0 until ingredientsArray.length()) {
            val ingredient = ingredientsArray.getJSONObject(i)
            ingredients.add(ingredient.getString("original"))
        }
        return ingredients
    }

    private fun parseInstructions(instructionsArray: JSONArray): String {
        if (instructionsArray.length() == 0) return ""
        val steps = mutableListOf<String>()
        val instructions = instructionsArray.getJSONObject(0)
        val stepsArray = instructions.getJSONArray("steps")
        for (i in 0 until stepsArray.length()) {
            val step = stepsArray.getJSONObject(i)
            steps.add("${i + 1}. ${step.getString("step")}")
        }
        return steps.joinToString("\n\n")
    }

    suspend fun getPopularRecipes(): List<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "$API_BASE_URL/random?number=10&apiKey=89b1e02aa1064bb69925ab953ccb8b47"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    parsePopularRecipesJson(response.toString())
                } else {
                    Log.e("RecipeRepository", "Error: $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Exception: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parsePopularRecipesJson(jsonString: String): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        try {
            val jsonObject = JSONObject(jsonString)
            val recipesArray = jsonObject.getJSONArray("recipes")
            for (i in 0 until recipesArray.length()) {
                val recipeJson = recipesArray.getJSONObject(i)
                val recipe = Recipe(
                    id = recipeJson.getInt("id").toString(),
                    title = recipeJson.getString("title"),
                    imageUrl = recipeJson.getString("image"),
                    cookTime = recipeJson.getInt("readyInMinutes"),
                    servings = recipeJson.getInt("servings"),
                    ingredients = parseIngredients(recipeJson.getJSONArray("extendedIngredients")),
                    instructions = parseInstructions(recipeJson.getJSONArray("analyzedInstructions"))
                )
                recipes.add(recipe)
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error parsing popular recipes JSON: ${e.message}")
        }
        return recipes
    }

    fun getFavoriteRecipes(): List<Recipe> {
        val json = sharedPreferences.getString("favorite_recipes", "[]")
        val type = object : TypeToken<List<Recipe>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addFavoriteRecipe(recipe: Recipe) {
        val favorites = getFavoriteRecipes().toMutableList()
        if (!favorites.any { it.id == recipe.id }) {
            favorites.add(recipe)
            saveFavoriteRecipes(favorites)
        }
    }

    fun removeFavoriteRecipe(recipeId: String) {
        val favorites = getFavoriteRecipes().toMutableList()
        favorites.removeAll { it.id == recipeId }
        saveFavoriteRecipes(favorites)
    }

    private fun saveFavoriteRecipes(recipes: List<Recipe>) {
        val json = gson.toJson(recipes)
        sharedPreferences.edit().putString("favorite_recipes", json).apply()
    }

    fun isRecipeFavorite(recipeId: String): Boolean {
        return getFavoriteRecipes().any { it.id == recipeId }
    }

    // This would typically fetch from an API, but for now we'll return sample data
    private fun getAllRecipes(): List<Recipe> {
        // TODO: Replace with actual API call
        return listOf(
            Recipe(
                title = "Spaghetti Carbonara",
                ingredients = listOf("spaghetti", "eggs", "pecorino cheese", "guanciale", "black pepper"),
                instructions = "1. Cook pasta...",
                imageUrl = "https://example.com/carbonara.jpg",
                cookTime = 30,
                servings = 4,
                cuisineType = "Italian",
                difficulty = "Medium",
                dietaryTags = setOf(),
                allergens = setOf("Wheat", "Eggs")
            ),
            Recipe(
                title = "Vegan Sushi Roll",
                ingredients = listOf("sushi rice", "nori", "cucumber", "avocado", "carrots"),
                instructions = "1. Cook sushi rice...",
                imageUrl = "https://example.com/sushi.jpg",
                cookTime = 45,
                servings = 2,
                cuisineType = "Japanese",
                difficulty = "Hard",
                dietaryTags = setOf("Vegan", "Vegetarian"),
                allergens = setOf()
            ),
            // Add more sample recipes as needed
        )
    }
}