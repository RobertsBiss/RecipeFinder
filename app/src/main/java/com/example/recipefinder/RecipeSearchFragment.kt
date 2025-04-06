package com.example.recipefinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefinder.adapter.RecipeAdapter
import com.example.recipefinder.model.RecipeRepository
import com.example.recipefinder.model.UserPreferences
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class RecipeSearchFragment : Fragment() {

    private lateinit var ingredientEditText: EditText
    private lateinit var addIngredientButton: Button
    private lateinit var searchButton: Button
    private lateinit var ingredientChipGroup: ChipGroup
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var trendingTitle: TextView

    private val ingredients = mutableListOf<String>()
    private lateinit var recipeRepository: RecipeRepository

    companion object {
        private const val KEY_INGREDIENTS = "ingredients"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getStringArrayList(KEY_INGREDIENTS)?.let { savedIngredients ->
            ingredients.clear()
            ingredients.addAll(savedIngredients)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeRepository = RecipeRepository(requireContext())

        ingredientEditText = view.findViewById(R.id.ingredient_input)
        addIngredientButton = view.findViewById(R.id.add_ingredient_button)
        searchButton = view.findViewById(R.id.search_button)
        ingredientChipGroup = view.findViewById(R.id.ingredient_chip_group)
        recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view)
        trendingTitle = view.findViewById(R.id.trending_title)

        recipeRecyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter { recipe ->
            // Navigate to recipe detail with user ingredients
            val detailFragment = RecipeDetailFragment.newInstance(recipe, ingredients)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        recipeRecyclerView.adapter = recipeAdapter

        // Load popular recipes only if there are no ingredients
        if (ingredients.isEmpty()) {
            loadPopularRecipes()
        } else {
            // If there are saved ingredients, perform the search
            ingredients.forEach { ingredient ->
                addIngredientChip(ingredient)
            }
            searchRecipes()
        }

        addIngredientButton.setOnClickListener {
            val ingredient = ingredientEditText.text.toString().trim()
            if (ingredient.isNotEmpty() && !ingredients.contains(ingredient)) {
                addIngredientChip(ingredient)
                ingredients.add(ingredient)
                ingredientEditText.text.clear()
                // Hide trending title when adding ingredients
                trendingTitle.visibility = View.GONE
            } else if (ingredients.contains(ingredient)) {
                Toast.makeText(context, "Ingredient already added", Toast.LENGTH_SHORT).show()
            }
        }

        searchButton.setOnClickListener {
            if (ingredients.isEmpty()) {
                Toast.makeText(context, "Add at least one ingredient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            searchRecipes()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_INGREDIENTS, ArrayList(ingredients))
    }

    private fun addIngredientChip(ingredient: String) {
        val chip = Chip(context).apply {
            text = ingredient
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                ingredientChipGroup.removeView(this)
                ingredients.remove(ingredient)
                if (ingredients.isEmpty()) {
                    loadPopularRecipes()
                }
            }
        }
        ingredientChipGroup.addView(chip)
    }

    private fun loadPopularRecipes() {
        lifecycleScope.launch {
            val popularRecipes = recipeRepository.getPopularRecipes()
            if (popularRecipes.isNotEmpty()) {
                recipeAdapter.submitList(popularRecipes)
                trendingTitle.visibility = View.VISIBLE
            }
        }
    }

    private fun searchRecipes() {
        if (ingredients.isEmpty()) {
            Toast.makeText(context, "Add at least one ingredient", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        val loadingView = view?.findViewById<View>(R.id.loading_indicator)
        loadingView?.visibility = View.VISIBLE
        recipeRecyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recipes = recipeRepository.searchRecipesByIngredients(ingredients)

                // Hide loading indicator
                loadingView?.visibility = View.GONE
                recipeRecyclerView.visibility = View.VISIBLE

                if (recipes.isEmpty()) {
                    Toast.makeText(
                        context,
                        "No recipes found with these ingredients",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    recipeAdapter.submitList(recipes)
                    trendingTitle.visibility = View.GONE

                    // Check if preferences were applied
                    val userPreferences = UserPreferences.getUserPreferences(requireContext())
                    if (userPreferences.hasAnyPreferences()) {
                        Toast.makeText(
                            context,
                            "Showing recipes matching your preferences",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                loadingView?.visibility = View.GONE
                recipeRecyclerView.visibility = View.VISIBLE

                Toast.makeText(context, "Error searching recipes: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}