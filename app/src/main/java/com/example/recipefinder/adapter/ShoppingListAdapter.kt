package com.example.recipefinder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefinder.R
import com.example.recipefinder.model.ShoppingListItem

class ShoppingListAdapter(
    private var items: List<ShoppingListItem>,
    private val onItemCheckedChanged: (ShoppingListItem) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.ingredient_checkbox)
        val ingredientText: TextView = view.findViewById(R.id.ingredient_text)
        val recipeTitle: TextView = view.findViewById(R.id.recipe_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.checkbox.isChecked = item.isChecked
        holder.ingredientText.text = item.ingredient
        holder.recipeTitle.text = "From: ${item.recipeTitle}"

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onItemCheckedChanged(item.copy(isChecked = isChecked))
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ShoppingListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}