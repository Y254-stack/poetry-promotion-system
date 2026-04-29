package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.poetry.R
import com.example.poetry.databinding.FragmentStudyDashboardBinding
import com.example.poetry.features.user.viewmodel.UserViewModel

class StudyDashboardFragment : Fragment(R.layout.fragment_study_dashboard) {

    private var _binding: FragmentStudyDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStudyDashboardBinding.bind(view)

        viewModel.dashboardExtraStats.observe(viewLifecycleOwner) { stats ->
            if (stats.size >= 4) {
                binding.masteredValue.text = stats[0].value
                binding.cumulativeDaysValue.text = stats[1].value
                binding.accuracyValue.text = stats[2].value
                binding.reviewPendingValue.text = stats[3].value
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
