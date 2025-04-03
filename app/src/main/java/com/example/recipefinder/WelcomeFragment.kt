package com.example.recipefinder

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.recipefinder.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.getStartedButton.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("has_seen_welcome", true).apply()

            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, RecipeSearchFragment())
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentTransaction.commit()

            requireActivity().supportFragmentManager.executePendingTransactions()
            (activity as? MainActivity)?.showBottomNavigation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun shouldShowWelcomeScreen(context: Context): Boolean {
            val sharedPref = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            return !sharedPref.getBoolean("has_seen_welcome", false)
        }
    }
}