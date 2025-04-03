package com.example.recipefinder

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.recipefinder.model.Recipe
import com.example.recipefinder.model.RecipeRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private lateinit var recipe: Recipe
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var favoriteButton: FloatingActionButton
    private var isFavorite = false
    private var userIngredients = listOf<String>()

    companion object {
        private const val ARG_RECIPE = "recipe"
        private const val ARG_USER_INGREDIENTS = "userIngredients"

        fun newInstance(recipe: Recipe, userIngredients: List<String>): RecipeDetailFragment {
            val fragment = RecipeDetailFragment()
            val args = Bundle()
            args.putString("id", recipe.id)
            args.putString("title", recipe.title)
            args.putString("imageUrl", recipe.imageUrl)
            args.putStringArrayList("ingredients", ArrayList(recipe.ingredients))
            args.putString("instructions", recipe.instructions)
            args.putInt("cookTime", recipe.cookTime)
            args.putInt("servings", recipe.servings)
            args.putStringArrayList("userIngredients", ArrayList(userIngredients))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeRepository = RecipeRepository(requireContext())

        // Extract recipe from arguments
        arguments?.let { args ->
            val id = args.getString("id") ?: ""
            val title = args.getString("title") ?: ""
            val imageUrl = args.getString("imageUrl") ?: ""
            val ingredients = args.getStringArrayList("ingredients") ?: arrayListOf()
            val instructions = args.getString("instructions") ?: ""
            val cookTime = args.getInt("cookTime", 0)
            val servings = args.getInt("servings", 0)
            userIngredients = args.getStringArrayList("userIngredients") ?: arrayListOf()

            recipe = Recipe(id, title, imageUrl, ingredients, instructions, cookTime, servings)

            // Load complete recipe details
            loadCompleteRecipeDetails(id)
        }

        // Initialize views
        val titleTextView = view.findViewById<TextView>(R.id.recipe_title)
        val recipeImageView = view.findViewById<ImageView>(R.id.recipe_image)
        val cookTimeTextView = view.findViewById<TextView>(R.id.cook_time)
        val servingsTextView = view.findViewById<TextView>(R.id.servings)
        val ingredientsTextView = view.findViewById<TextView>(R.id.ingredients_list)
        val instructionsTextView = view.findViewById<TextView>(R.id.instructions)
        favoriteButton = view.findViewById(R.id.favorite_button)
        val backButton = view.findViewById<Button>(R.id.back_button)
        val addToShoppingListButton = view.findViewById<Button>(R.id.add_to_shopping_list_button)

        // Set basic data
        titleTextView.text = recipe.title
        cookTimeTextView.text = "Cook time: ${recipe.cookTime} minutes"
        servingsTextView.text = "Servings: ${recipe.servings}"

        updateIngredientsDisplay(ingredientsTextView)

        updateInstructionsDisplay(instructionsTextView, recipe.instructions)

        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(recipeImageView)

        // Check if recipe is in favorites
        isFavorite = recipeRepository.isRecipeFavorite(recipe.id)
        updateFavoriteButtonIcon()

        // Set favorite button click listener
        favoriteButton.setOnClickListener {
            if (isFavorite) {
                recipeRepository.removeFavoriteRecipe(recipe.id)
                Toast.makeText(context, "Recipe removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                recipeRepository.addFavoriteRecipe(recipe)
                Toast.makeText(context, "Recipe added to favorites", Toast.LENGTH_SHORT).show()
            }
            isFavorite = !isFavorite
            updateFavoriteButtonIcon()
        }

        // Set back button click listener
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup add to shopping list button
        addToShoppingListButton.setOnClickListener {
            val missingIngredients = recipe.ingredients.filter { ingredient ->
                !userIngredients.any { userIngredient ->
                    ingredient.lowercase().contains(userIngredient.lowercase())
                }
            }

            if (missingIngredients.isNotEmpty()) {
                ShoppingListFragment.addToShoppingList(
                    requireContext(),
                    recipe.id,
                    recipe.title,
                    missingIngredients
                )
                Toast.makeText(context, "Added missing ingredients to shopping list", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "You have all the ingredients!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCompleteRecipeDetails(recipeId: String) {

        lifecycleScope.launch {
            val detailedRecipe = recipeRepository.getRecipeDetails(recipeId)

            detailedRecipe?.let { fullRecipe ->
                recipe = fullRecipe

                view?.let { view ->
                    val ingredientsTextView = view.findViewById<TextView>(R.id.ingredients_list)
                    val instructionsTextView = view.findViewById<TextView>(R.id.instructions)

                    updateIngredientsDisplay(ingredientsTextView)

                    updateInstructionsDisplay(instructionsTextView, fullRecipe.instructions)
                }
            }

        }
    }

    private fun updateIngredientsDisplay(ingredientsTextView: TextView) {

        val spannableString = SpannableStringBuilder()

        val normalizedUserIngredients = userIngredients.map { it.lowercase() }

        recipe.ingredients.forEachIndexed { index, ingredient ->
            val isUserHasIngredient = normalizedUserIngredients.any {
                ingredient.lowercase().contains(it)
            }

            val ingredientText = "• $ingredient\n"
            val start = spannableString.length
            spannableString.append(ingredientText)

            // If user doesn't have this ingredient, highlight it
            if (!isUserHasIngredient) {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    start,
                    start + ingredientText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    start + ingredientText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        spannableString.append("\n")
        val legendStart = spannableString.length
        spannableString.append("* Red ingredients are not in your pantry")
        spannableString.setSpan(
            ForegroundColorSpan(Color.RED),
            legendStart,
            legendStart + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        ingredientsTextView.text = spannableString
    }

    private fun updateInstructionsDisplay(instructionsTextView: TextView, instructionsText: String) {
        var cleanedInstructions = instructionsText.trim()

        if (cleanedInstructions.contains("<")) {
            try {
                val htmlSpanned = Html.fromHtml(cleanedInstructions, Html.FROM_HTML_MODE_COMPACT)
                cleanedInstructions = htmlSpanned.toString().trim()
            } catch (e: Exception) {
            }
        }

        val steps = splitIntoSteps(cleanedInstructions)

        val formattedInstructions = formatSteps(steps)

        instructionsTextView.text = formattedInstructions
    }

    private fun splitIntoSteps(instructions: String): List<String> {
        val numberedPattern = "\\d+\\.\\s".toRegex()

        if (numberedPattern.containsMatchIn(instructions)) {
            val numberedSteps = mutableListOf<String>()
            val matches = numberedPattern.findAll(instructions)

            var startIndex = 0
            for (match in matches) {
                val endIndex = if (matches.count() > 1) match.range.first else instructions.length
                if (startIndex > 0 && startIndex < endIndex) {
                    val step = instructions.substring(startIndex, endIndex).trim()
                    if (step.isNotEmpty()) {
                        numberedSteps.add(step)
                    }
                }
                startIndex = match.range.first
            }

            if (startIndex < instructions.length) {
                val lastStep = instructions.substring(startIndex).trim()
                if (lastStep.isNotEmpty()) {
                    numberedSteps.add(lastStep)
                }
            }

            return if (numberedSteps.isNotEmpty()) numberedSteps else listOf(instructions)
        }

        val bulletPattern = "([•\\*\\-])\\s".toRegex()
        if (bulletPattern.containsMatchIn(instructions)) {
            return instructions.split(bulletPattern).filter { it.trim().isNotEmpty() }
        }

        if (instructions.contains("\n\n")) {
            return instructions.split("\n\n").filter { it.trim().isNotEmpty() }
        }

        if (instructions.contains("\n")) {
            return instructions.split("\n").filter { it.trim().isNotEmpty() }
        }

        val sentencePattern = "(?<=[.!?])\\s+".toRegex()
        val sentences = instructions.split(sentencePattern).filter { it.trim().isNotEmpty() }

        return if (sentences.size > 1) sentences else listOf(instructions)
    }

    private fun formatSteps(steps: List<String>): String {
        val formattedText = StringBuilder()

        steps.forEachIndexed { index, step ->
            var cleanStep = step.trim()

            val leadingNumberPattern = "^\\d+\\.\\s+".toRegex()
            val leadingBulletPattern = "^[•\\*\\-]\\s+".toRegex()

            cleanStep = cleanStep.replace(leadingNumberPattern, "")
            cleanStep = cleanStep.replace(leadingBulletPattern, "")

            if (!cleanStep.endsWith(".") && !cleanStep.endsWith("!") && !cleanStep.endsWith("?")) {
                cleanStep += "."
            }

            formattedText.append("${index + 1}. $cleanStep\n\n")
        }

        return formattedText.toString().trim()
    }

    private fun updateFavoriteButtonIcon() {
        favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }
}