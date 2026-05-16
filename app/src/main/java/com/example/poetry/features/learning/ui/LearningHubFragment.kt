package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentLearningHubBinding
import com.example.poetry.features.learning.viewmodel.LearningViewModel

class LearningHubFragment : Fragment(R.layout.fragment_learning_hub) {

    private var _binding: FragmentLearningHubBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LearningViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLearningHubBinding.bind(view)

        viewModel.modes.observe(viewLifecycleOwner) { modes ->
            val views = listOf(
                Triple(binding.modeBadge1, binding.modeTitle1, binding.modeSubtitle1),
                Triple(binding.modeBadge2, binding.modeTitle2, binding.modeSubtitle2),
                Triple(binding.modeBadge3, binding.modeTitle3, binding.modeSubtitle3),
                Triple(binding.modeBadge4, binding.modeTitle4, binding.modeSubtitle4)
            )
            val cards = listOf(
                binding.modeCard1,
                binding.modeCard2,
                binding.modeCard3,
                binding.modeCard4
            )

            cards.forEachIndexed { index, card ->
                val mode = modes.getOrNull(index)
                val triple = views[index]
                if (mode == null) {
                    card.visibility = View.INVISIBLE
                } else {
                    card.visibility = View.VISIBLE
                    triple.first.text = mode.title.take(1)
                    triple.second.text = mode.title
                    triple.third.text = mode.subtitle
                    card.setOnClickListener {
                        findNavController().navigate(mode.destinationId)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!SessionManager(requireContext()).isLoggedIn() &&
            findNavController().currentDestination?.id == R.id.learningHubFragment
        ) {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
