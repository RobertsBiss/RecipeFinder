package com.example.recipefinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefinder.adapter.RecipeAdapter
import com.example.recipefinder.model.RecipeRepository

class FavoritesFragment : Fragment() {

    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var recipeRepository: RecipeRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeRepository = RecipeRepository(requireContext())

        recipeRecyclerView = view.findViewById(R.id.favorites_recycler_view)
        emptyStateTextView = view.findViewById(R.id.empty_state_text)

        // Setup RecyclerView
        recipeRecyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter { recipe ->
            val detailFragment = RecipeDetailFragment.newInstance(recipe, emptyList())
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        recipeRecyclerView.adapter = recipeAdapter
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val favorites = recipeRepository.getFavoriteRecipes()

        if (favorites.isEmpty()) {
            recipeRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
        } else {
            recipeRecyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
            recipeAdapter.submitList(favorites)
        }
    }
}