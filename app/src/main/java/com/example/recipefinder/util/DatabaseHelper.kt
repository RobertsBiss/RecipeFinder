package com.example.recipefinder.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.recipefinder.model.Recipe

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "recipes.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_FAVORITES = "favorites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_INGREDIENTS = "ingredients"
        private const val COLUMN_INSTRUCTIONS = "instructions"
        private const val COLUMN_COOK_TIME = "cook_time"
        private const val COLUMN_SERVINGS = "servings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TITLE TEXT,
                $COLUMN_IMAGE_URL TEXT,
                $COLUMN_INGREDIENTS TEXT,
                $COLUMN_INSTRUCTIONS TEXT,
                $COLUMN_COOK_TIME INTEGER,
                $COLUMN_SERVINGS INTEGER
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        onCreate(db)
    }

    fun addFavorite(recipe: Recipe) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, recipe.id)
            put(COLUMN_TITLE, recipe.title)
            put(COLUMN_IMAGE_URL, recipe.imageUrl)
            put(COLUMN_INGREDIENTS, recipe.ingredients.joinToString("|"))
            put(COLUMN_INSTRUCTIONS, recipe.instructions)
            put(COLUMN_COOK_TIME, recipe.cookTime)
            put(COLUMN_SERVINGS, recipe.servings)
        }

        db.insert(TABLE_FAVORITES, null, values)
        db.close()
    }

    fun removeFavorite(recipeId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_FAVORITES, "$COLUMN_ID = ?", arrayOf(recipeId))
        db.close()
    }

    fun isFavorite(recipeId: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_FAVORITES WHERE $COLUMN_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(recipeId))
        val isFavorite = cursor.count > 0
        cursor.close()
        db.close()
        return isFavorite
    }

    fun getAllFavorites(): List<Recipe> {
        val favorites = mutableListOf<Recipe>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_FAVORITES"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL))
                val ingredientsString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS))
                val ingredients = ingredientsString.split("|")
                val instructions = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS))
                val cookTime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COOK_TIME))
                val servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS))

                favorites.add(Recipe(id, title, imageUrl, ingredients, instructions, cookTime, servings))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return favorites
    }
}