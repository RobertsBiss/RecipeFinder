package com.example.recipefinder.model

import android.content.Context
import android.util.Log
import com.example.recipefinder.util.DatabaseHelper
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

    suspend fun searchRecipesByIngredients(ingredients: List<String>): List<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val ingredientsParam = ingredients.joinToString(",")
                val urlString = "$API_BASE_URL/findByIngredients?ingredients=$ingredientsParam&apiKey=f9b2507224434ec287f6913d6896dee5"
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

                    parseRecipesJson(response.toString())
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

    suspend fun getRecipeDetails(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "$API_BASE_URL/$recipeId/information?apiKey=f9b2507224434ec287f6913d6896dee5"
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
                val urlString = "$API_BASE_URL/random?number=10&apiKey=f9b2507224434ec287f6913d6896dee5"
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
        return databaseHelper.getAllFavorites()
    }

    fun addFavoriteRecipe(recipe: Recipe) {
        databaseHelper.addFavorite(recipe)
    }

    fun removeFavoriteRecipe(recipeId: String) {
        databaseHelper.removeFavorite(recipeId)
    }

    fun isRecipeFavorite(recipeId: String): Boolean {
        return databaseHelper.isFavorite(recipeId)
    }
}