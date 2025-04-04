package com.example.recipefinder

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    loadFragment(RecipeSearchFragment())
                    true
                }
                R.id.nav_favorites -> {
                    loadFragment(FavoritesFragment())
                    true
                }
                R.id.nav_shopping_list -> {
                    loadFragment(ShoppingListFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // Show welcome screen if it's the first launch
        if (WelcomeFragment.shouldShowWelcomeScreen(this)) {
            loadFragment(WelcomeFragment())
            bottomNavigation.visibility = View.GONE
        } else {
            // Load the default fragment
            if (savedInstanceState == null) {
                loadFragment(RecipeSearchFragment())
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun showBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.VISIBLE
    }
}