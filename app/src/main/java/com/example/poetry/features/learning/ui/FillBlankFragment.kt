package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentFillBlankBinding
import com.google.android.material.chip.Chip

class FillBlankFragment : Fragment(R.layout.fragment_fill_blank) {

    private var _binding: FragmentFillBlankBinding? = null
    private val binding get() = _binding!!

    // Data Mocking
    private val titleText = "如梦令·昨夜雨疏风骤"
    private val fullAnswer = "知否知否应是绿肥红瘦"
    private val candidateWords = listOf("知", "否", "否", "肥", "知", "红", "瘦", "是", "绿", "应")
    
    private val answerGrids = mutableListOf<TextView>()
    private var selectedGridIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillBlankBinding.bind(view)

        setupGrids()
        setupCandidates()
        
        binding.btnHint.setOnClickListener {
            provideHint()
        }
    }

    private fun setupGrids() {
        answerGrids.clear()
        val row1 = binding.row1
        val row2 = binding.row2

        for (i in 0 until row1.childCount) {
            val tv = row1.getChildAt(i) as TextView
            answerGrids.add(tv)
            setupGridClick(tv, i)
        }
        for (i in 0 until row2.childCount) {
            val tv = row2.getChildAt(i) as TextView
            answerGrids.add(tv)
            setupGridClick(tv, row1.childCount + i)
        }
        
        updateSelection(0)
    }

    private fun setupGridClick(tv: TextView, index: Int) {
        tv.setOnClickListener {
            val currentText = tv.text.toString()
            if (currentText.isNotEmpty()) {
                // Return text to candidates
                findAndEnableCandidate(currentText)
                tv.text = ""
            }
            updateSelection(index)
        }
    }

    private fun setupCandidates() {
        binding.candidateContainer.removeAllViews()
        candidateWords.forEach { word ->
            val textView = LayoutInflater.from(requireContext()).inflate(R.layout.item_candidate_word, binding.candidateContainer, false) as TextView
            textView.text = word
            textView.setOnClickListener {
                fillGrid(word, textView)
            }
            binding.candidateContainer.addView(textView)
        }
    }

    private fun fillGrid(word: String, candidateView: View) {
        if (selectedGridIndex < answerGrids.size) {
            val grid = answerGrids[selectedGridIndex]
            
            // If grid already has text, return it first
            if (grid.text.isNotEmpty()) {
                findAndEnableCandidate(grid.text.toString())
            }

            grid.text = word
            candidateView.isEnabled = false
            candidateView.alpha = 0.5f

            // Move selection to next empty grid
            val nextEmpty = answerGrids.indexOfFirst { it.text.isEmpty() }
            if (nextEmpty != -1) {
                updateSelection(nextEmpty)
            } else {
                checkAnswer()
            }
        }
    }

    private fun updateSelection(index: Int) {
        answerGrids.forEach { it.isSelected = false }
        if (index in answerGrids.indices) {
            selectedGridIndex = index
            answerGrids[index].isSelected = true
        }
    }

    private fun findAndEnableCandidate(word: String) {
        for (i in 0 until binding.candidateContainer.childCount) {
            val view = binding.candidateContainer.getChildAt(i) as TextView
            if (view.text == word && !view.isEnabled) {
                view.isEnabled = true
                view.alpha = 1.0f
                break
            }
        }
    }

    private fun provideHint() {
        val emptyIndex = answerGrids.indexOfFirst { it.text.isEmpty() }
        if (emptyIndex != -1) {
            val correctChar = fullAnswer[emptyIndex].toString()
            
            // Find this char in candidates
            for (i in 0 until binding.candidateContainer.childCount) {
                val view = binding.candidateContainer.getChildAt(i) as TextView
                if (view.text == correctChar && view.isEnabled) {
                    fillGrid(correctChar, view)
                    break
                }
            }
        }
    }

    private fun checkAnswer() {
        val userAnswer = answerGrids.joinToString("") { it.text.toString() }
        if (userAnswer.length == fullAnswer.length) {
            if (userAnswer == fullAnswer) {
                Toast.makeText(context, "答对了！太棒了", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "答案有误，再检查一下吧", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
