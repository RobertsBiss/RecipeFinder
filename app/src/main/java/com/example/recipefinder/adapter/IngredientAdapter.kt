package com.example.recipefinder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefinder.R

class IngredientAdapter(
    private val ingredients: List<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount(): Int = ingredients.size

    class IngredientViewHolder(
        itemView: View,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ingredientTextView: TextView = itemView.findViewById(R.id.ingredient_name)
        private val deleteButton: View = itemView.findViewById(R.id.delete_button)

        fun bind(ingredient: String) {
            ingredientTextView.text = ingredient

            deleteButton.setOnClickListener {
                onDeleteClick(adapterPosition)
            }
        }
    }
}