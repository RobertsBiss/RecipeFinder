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

    // Replace with your chosen API base URL
    private val API_BASE_URL = "https://api.spoonacular.com/recipes"

    suspend fun searchRecipesByIngredients(ingredients: List<String>): List<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val ingredientsParam = ingredients.joinToString(",")
                val urlString = "$API_BASE_URL/findByIngredients?ingredients=$ingredientsParam&apiKey=89b1e02aa1064bb69925ab953ccb8b47"
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

    private fun parseRecipeDetailsJson(jsonString: String): Recipe? {
        try {
            val recipeJson = JSONObject(jsonString)

            val id = recipeJson.getString("id")
            val title = recipeJson.getString("title")
            val imageUrl = recipeJson.getString("image")
            val cookTime = recipeJson.optInt("readyInMinutes", 30)
            val servings = recipeJson.optInt("servings", 4)

            // Parse full ingredients list
            val ingredientsArray = recipeJson.getJSONArray("extendedIngredients")
            val ingredients = mutableListOf<String>()
            for (i in 0 until ingredientsArray.length()) {
                val ingredient = ingredientsArray.getJSONObject(i).getString("original")
                ingredients.add(ingredient)
            }

            // Get instructions
            var instructions = ""
            if (recipeJson.has("instructions") && !recipeJson.isNull("instructions")) {
                instructions = recipeJson.getString("instructions")
            } else if (recipeJson.has("analyzedInstructions")) {
                val analyzedInstructions = recipeJson.getJSONArray("analyzedInstructions")
                if (analyzedInstructions.length() > 0) {
                    val stepsJson = analyzedInstructions.getJSONObject(0).getJSONArray("steps")
                    val stepsBuilder = StringBuilder()
                    for (i in 0 until stepsJson.length()) {
                        val step = stepsJson.getJSONObject(i)
                        val number = step.getInt("number")
                        val stepText = step.getString("step")
                        stepsBuilder.append("$number. $stepText\n\n")
                    }
                    instructions = stepsBuilder.toString().trim()
                }
            }

            if (instructions.isEmpty()) {
                instructions = "No detailed instructions available for this recipe."
            }

            return Recipe(id, title, imageUrl, ingredients, instructions, cookTime, servings)
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Parse error: ${e.message}")
            return null
        }
    }
    private fun parseRecipesJson(jsonString: String): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val recipeJson = jsonArray.getJSONObject(i)

                val id = recipeJson.getString("id")
                val title = recipeJson.getString("title")
                val imageUrl = recipeJson.getString("image")

                // Parse ingredients - this will depend on your API structure
                val ingredientsArray = recipeJson.getJSONArray("usedIngredients")
                val ingredients = mutableListOf<String>()
                for (j in 0 until ingredientsArray.length()) {
                    val ingredient = ingredientsArray.getJSONObject(j).getString("name")
                    ingredients.add(ingredient)
                }

                // Note: For simplicity, we're assuming these details are in the initial response
                // For many APIs, you might need to make a second request to get full details
                val instructions = recipeJson.optString("instructions", "See website for detailed instructions")
                val cookTime = recipeJson.optInt("readyInMinutes", 30)
                val servings = recipeJson.optInt("servings", 4)

                recipes.add(Recipe(id, title, imageUrl, ingredients, instructions, cookTime, servings))
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Parse error: ${e.message}")
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