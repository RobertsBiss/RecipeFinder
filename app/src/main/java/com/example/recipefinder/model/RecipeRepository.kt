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
        try {
            val ingredientsString = ingredients.joinToString(",")
            val urlString = "$API_BASE_URL/findByIngredients?apiKey=89b1e02aa1064bb69925ab953ccb8b47&ingredients=$ingredientsString&number=20"
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

                val basicRecipes = parseRecipesJson(response.toString())
                val userPreferences = UserPreferences.getUserPreferences(context)

                val detailedRecipes = basicRecipes.mapNotNull { recipe ->
                    getRecipeDetails(recipe.id)
                }

                // Only apply filtering if user has set preferences
                val filteredRecipes = if (userPreferences.hasAnyPreferences()) {
                    detailedRecipes.filter { recipe ->
                        val matchesDietary = userPreferences.dietaryPreferences.isEmpty() ||
                                matchesDietaryPreferences(recipe, userPreferences.dietaryPreferences)

                        val noAllergens = userPreferences.allergies.isEmpty() ||
                                !containsAllergens(recipe, userPreferences.allergies)

                        val matchesDifficulty = userPreferences.difficultyPreference.isEmpty() ||
                                matchesDifficulty(recipe, userPreferences.difficultyPreference)

                        val matchesCuisine = userPreferences.cuisinePreferences.isEmpty() ||
                                matchesCuisine(recipe, userPreferences.cuisinePreferences)

                        matchesDietary && noAllergens && matchesDifficulty && matchesCuisine
                    }
                } else {
                    detailedRecipes
                }

                val scoredRecipes = filteredRecipes.map { recipe ->
                    recipe to calculateRecipeScore(recipe, ingredients, userPreferences)
                }.sortedByDescending { it.second }

                scoredRecipes.map { it.first }
            } else {
                Log.e("RecipeRepository", "Error: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Exception: ${e.message}")
            emptyList()
        }
    }

    private fun matchesDietaryPreferences(recipe: Recipe, dietaryPreferences: Set<String>): Boolean {
        val recipeDietaryTags = inferDietaryTags(recipe)
        return recipeDietaryTags.any { it in dietaryPreferences }
    }

    private fun containsAllergens(recipe: Recipe, allergies: Set<String>): Boolean {
        // Check ingredients for common allergens
        return recipe.ingredients.any { ingredient ->
            allergies.any { allergen ->
                ingredient.lowercase().contains(allergen.lowercase())
            }
        }
    }

    private fun matchesDifficulty(recipe: Recipe, difficultyPreference: Set<String>): Boolean {
        val difficulty = inferDifficulty(recipe)
        return difficulty in difficultyPreference
    }

    private fun matchesCuisine(recipe: Recipe, cuisinePreferences: Set<String>): Boolean {
        val cuisineType = recipe.cuisineType.ifEmpty { inferCuisineType(recipe) }
        return cuisineType in cuisinePreferences
    }

    private fun inferDietaryTags(recipe: Recipe): Set<String> {
        if (recipe.dietaryTags.isNotEmpty()) {
            return recipe.dietaryTags
        }

        val inferredTags = mutableSetOf<String>()
        val ingredientsLower = recipe.ingredients.joinToString(" ").lowercase()

        // Check for vegetarian (no meat)
        val meatKeywords = listOf("meat", "beef", "chicken", "pork", "turkey", "lamb", "bacon")
        val isVegetarianPossible = !meatKeywords.any { ingredientsLower.contains(it) }

        // Check for vegan (no animal products)
        val animalProductKeywords = listOf("milk", "cheese", "cream", "egg", "butter", "yogurt", "honey")
        val isVeganPossible = isVegetarianPossible &&
                !animalProductKeywords.any { ingredientsLower.contains(it) }

        // Check for gluten free
        val glutenKeywords = listOf("wheat", "flour", "bread", "pasta", "barley", "rye")
        val isGlutenFreePossible = !glutenKeywords.any { ingredientsLower.contains(it) }

        // Check for dairy free
        val dairyKeywords = listOf("milk", "cheese", "cream", "butter", "yogurt")
        val isDairyFreePossible = !dairyKeywords.any { ingredientsLower.contains(it) }

        // Check for pescatarian
        val isPescatarianPossible = isVegetarianPossible ||
                ingredientsLower.contains("fish") ||
                ingredientsLower.contains("seafood") ||
                ingredientsLower.contains("shellfish")

        // Check for keto
        val carbyKeywords = listOf("sugar", "flour", "potato", "rice", "pasta", "bread")
        val isKetoPossible = !carbyKeywords.any { ingredientsLower.contains(it) }

        if (isVeganPossible) inferredTags.add("Vegan")
        if (isVegetarianPossible) inferredTags.add("Vegetarian")
        if (isGlutenFreePossible) inferredTags.add("Gluten Free")
        if (isDairyFreePossible) inferredTags.add("Dairy Free")
        if (isPescatarianPossible) inferredTags.add("Pescatarian")
        if (isKetoPossible) inferredTags.add("Keto")

        return inferredTags
    }

    private fun inferDifficulty(recipe: Recipe): String {
        if (recipe.difficulty.isNotEmpty() && recipe.difficulty != "Medium") {
            return recipe.difficulty
        }

        return when {
            recipe.cookTime < 20 && recipe.ingredients.size < 6 -> "Easy"
            recipe.cookTime > 45 || recipe.ingredients.size > 10 -> "Hard"
            else -> "Medium"
        }
    }

    private fun inferCuisineType(recipe: Recipe): String {
        val title = recipe.title.lowercase()
        val ingredients = recipe.ingredients.joinToString(" ").lowercase()

        // Check for cuisine keywords
        val cuisineKeywords = mapOf(
            "Italian" to listOf("pasta", "pizza", "risotto", "parmesan", "mozzarella", "italian"),
            "Mexican" to listOf("taco", "burrito", "mexican", "tortilla", "salsa", "enchilada"),
            "Chinese" to listOf("chinese", "stir fry", "soy sauce", "wok", "tofu", "ginger"),
            "Japanese" to listOf("sushi", "japanese", "miso", "ramen", "tempura", "wasabi"),
            "French" to listOf("french", "baguette", "croissant", "coq au vin", "ratatouille"),
            "Korean" to listOf("korean", "kimchi", "gochujang", "bulgogi", "bibimbap")
        )

        val textToCheck = "$title $ingredients"

        for ((cuisine, keywords) in cuisineKeywords) {
            if (keywords.any { textToCheck.contains(it) }) {
                return cuisine
            }
        }

        return ""
    }

    private fun calculateRecipeScore(
        recipe: Recipe,
        userIngredients: List<String>,
        preferences: UserPreferences
    ): Int {
        var score = 0

        val matchingIngredients = recipe.ingredients.count { ingredient ->
            userIngredients.any { userIngredient ->
                ingredient.lowercase().contains(userIngredient.lowercase())
            }
        }
        score += matchingIngredients * 10

        val coveragePercent = (matchingIngredients.toDouble() / recipe.ingredients.size * 100).toInt()
        score += coveragePercent / 2

        val recipeDietaryTags = inferDietaryTags(recipe)
        if (preferences.dietaryPreferences.isNotEmpty()) {
            val matchingDietaryPrefs = recipeDietaryTags.intersect(preferences.dietaryPreferences).size
            score += matchingDietaryPrefs * 30
        }

        val cuisineType = recipe.cuisineType.ifEmpty { inferCuisineType(recipe) }
        if (preferences.cuisinePreferences.isNotEmpty() &&
            cuisineType.isNotEmpty() &&
            preferences.cuisinePreferences.contains(cuisineType)) {
            score += 20
        }

        val difficulty = inferDifficulty(recipe)
        if (preferences.difficultyPreference.isNotEmpty() &&
            preferences.difficultyPreference.contains(difficulty)) {
            score += 15
        }

        if (preferences.allergies.isNotEmpty() &&
            containsAllergens(recipe, preferences.allergies)) {
            score -= 100  // Heavy penalty for allergens
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
                    cookTime = 0,
                    servings = 0,
                    ingredients = emptyList(),
                    instructions = ""
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

            // Extract basic recipe info
            val id = jsonObject.getInt("id").toString()
            val title = jsonObject.getString("title")
            val imageUrl = jsonObject.optString("image", "")
            val cookTime = jsonObject.optInt("readyInMinutes", 30)
            val servings = jsonObject.optInt("servings", 4)

            // Extract ingredients
            val ingredients = parseIngredients(jsonObject.getJSONArray("extendedIngredients"))

            // Extract instructions
            val instructions = if (jsonObject.has("analyzedInstructions")) {
                parseInstructions(jsonObject.getJSONArray("analyzedInstructions"))
            } else {
                jsonObject.optString("instructions", "")
            }

            // Extract dietary tags
            val dietaryTags = mutableSetOf<String>()
            if (jsonObject.has("vegetarian") && jsonObject.getBoolean("vegetarian")) {
                dietaryTags.add("Vegetarian")
            }
            if (jsonObject.has("vegan") && jsonObject.getBoolean("vegan")) {
                dietaryTags.add("Vegan")
            }
            if (jsonObject.has("glutenFree") && jsonObject.getBoolean("glutenFree")) {
                dietaryTags.add("Gluten Free")
            }
            if (jsonObject.has("dairyFree") && jsonObject.getBoolean("dairyFree")) {
                dietaryTags.add("Dairy Free")
            }
            if (jsonObject.has("ketogenic") && jsonObject.getBoolean("ketogenic")) {
                dietaryTags.add("Keto")
            }

            // Extract allergens from ingredient information
            val allergens = extractAllergensFromIngredients(jsonObject.getJSONArray("extendedIngredients"))

            // Extract cuisine type
            var cuisineType = ""
            if (jsonObject.has("cuisines") && jsonObject.getJSONArray("cuisines").length() > 0) {
                cuisineType = jsonObject.getJSONArray("cuisines").getString(0)
            }

            // Determine difficulty
            val difficulty = when {
                cookTime < 20 && ingredients.size < 6 -> "Easy"
                cookTime > 45 || ingredients.size > 10 -> "Hard"
                else -> "Medium"
            }

            return Recipe(
                id = id,
                title = title,
                imageUrl = imageUrl,
                cookTime = cookTime,
                servings = servings,
                ingredients = ingredients,
                instructions = instructions,
                dietaryTags = dietaryTags,
                allergens = allergens,
                cuisineType = cuisineType,
                difficulty = difficulty
            )
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error parsing recipe details JSON: ${e.message}")
            return null
        }
    }

    private fun extractAllergensFromIngredients(ingredientsArray: JSONArray): Set<String> {
        val allergens = mutableSetOf<String>()
        val allergenKeywords = mapOf(
            "Milk" to listOf("milk", "cheese", "cream", "butter", "dairy"),
            "Eggs" to listOf("egg"),
            "Peanuts" to listOf("peanut"),
            "Shellfish" to listOf("shrimp", "crab", "lobster", "shellfish"),
            "Wheat" to listOf("wheat", "flour", "bread", "pasta"),
            "Soy" to listOf("soy", "tofu", "soya")
        )

        for (i in 0 until ingredientsArray.length()) {
            val ingredient = ingredientsArray.getJSONObject(i)
            val ingredientName = ingredient.getString("name").lowercase()

            for ((allergen, keywords) in allergenKeywords) {
                if (keywords.any { ingredientName.contains(it) }) {
                    allergens.add(allergen)
                    break
                }
            }
        }

        return allergens
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

    private suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        try {
            val urlString = "$API_BASE_URL/findByIngredients?apiKey=89b1e02aa1064bb69925ab953ccb8b47&number=10"
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