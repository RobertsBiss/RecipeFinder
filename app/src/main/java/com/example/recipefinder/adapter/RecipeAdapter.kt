package com.example.recipefinder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipefinder.R
import com.example.recipefinder.model.Recipe

class RecipeAdapter(private val onItemClick: (Recipe) -> Unit) :
    ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        itemView: View,
        private val onItemClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.recipe_title)
        private val recipeImageView: ImageView = itemView.findViewById(R.id.recipe_image)
        private val cookTimeTextView: TextView = itemView.findViewById(R.id.cook_time)
        private val ingredientsCountTextView: TextView = itemView.findViewById(R.id.ingredients_count)

        fun bind(recipe: Recipe) {
            titleTextView.text = recipe.title
            cookTimeTextView.text = "${recipe.cookTime} min"
            ingredientsCountTextView.text = "${recipe.ingredients.size} ingredients"

            Glide.with(itemView.context)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(recipeImageView)

            itemView.setOnClickListener {
                onItemClick(recipe)
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}