package com.example.recipefinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefinder.adapter.ShoppingListAdapter
import com.example.recipefinder.model.ShoppingListItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ShoppingListFragment : Fragment() {
    private lateinit var adapter: ShoppingListAdapter
    private var shoppingList = mutableListOf<ShoppingListItem>()
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shopping_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved shopping list
        loadShoppingList()

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.shopping_list_recycler_view)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        adapter = ShoppingListAdapter(shoppingList) { updatedItem ->
            updateShoppingListItem(updatedItem)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Setup clear all button
        view.findViewById<Button>(R.id.clear_all_button).setOnClickListener {
            clearAllItems()
        }

        // Update empty state visibility
        updateEmptyStateVisibility()
    }

    private fun loadShoppingList() {
        val sharedPrefs = requireContext().getSharedPreferences("shopping_list", android.content.Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("items", "[]")
        val type = object : TypeToken<List<ShoppingListItem>>() {}.type
        shoppingList = Gson().fromJson(json, type)
    }

    private fun saveShoppingList() {
        val sharedPrefs = requireContext().getSharedPreferences("shopping_list", android.content.Context.MODE_PRIVATE)
        val json = Gson().toJson(shoppingList)
        sharedPrefs.edit().putString("items", json).apply()
        updateEmptyStateVisibility()
    }

    private fun updateShoppingListItem(updatedItem: ShoppingListItem) {
        val index = shoppingList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            shoppingList[index] = updatedItem
            saveShoppingList()
        }
    }

    private fun clearAllItems() {
        shoppingList.clear()
        adapter.updateItems(shoppingList)
        saveShoppingList()
    }

    private fun updateEmptyStateVisibility() {
        emptyStateText.visibility = if (shoppingList.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        fun addToShoppingList(context: android.content.Context, recipeId: String, recipeTitle: String, ingredients: List<String>) {
            val sharedPrefs = context.getSharedPreferences("shopping_list", android.content.Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("items", "[]")
            val type = object : TypeToken<List<ShoppingListItem>>() {}.type
            val currentList = Gson().fromJson<List<ShoppingListItem>>(json, type).toMutableList()

            // Add new ingredients
            ingredients.forEach { ingredient ->
                if (!currentList.any { it.ingredient == ingredient }) {
                    currentList.add(ShoppingListItem(
                        ingredient = ingredient,
                        recipeId = recipeId,
                        recipeTitle = recipeTitle
                    ))
                }
            }

            // Save updated list
            val updatedJson = Gson().toJson(currentList)
            sharedPrefs.edit().putString("items", updatedJson).apply()
        }
    }
}