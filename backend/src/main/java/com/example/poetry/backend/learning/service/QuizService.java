package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.repository.QuizRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public QuizModels.FillBlankQuiz generateFillBlankQuiz() {
        Map<String, Object> data = quizRepository.getRandomSentenceForQuiz();
        Long workId = (Long) data.get("work_id");
        String sentence = (String) data.get("sentence_text");
        String title = (String) data.get("title");
        String author = (String) data.get("author");

        // Clean punctuation for candidate generation
        String cleanSentence = sentence.replaceAll("[，。？！；：、]", "");
        
        List<String> candidates = new ArrayList<>();
        for (char c : cleanSentence.toCharArray()) {
            candidates.add(String.valueOf(c));
        }

        // Add 3 distractor words
        List<String> distractors = quizRepository.getRandomDistractorChars(3);
        candidates.addAll(distractors);

        // Shuffle
        Collections.shuffle(candidates);

        return new QuizModels.FillBlankQuiz(
            workId,
            title + "·" + author,
            author,
            sentence,
            candidates
        );
    }

    public void submitResult(QuizModels.QuizSubmitRequest request) {
        quizRepository.saveQuizRecord(request);
    }
}
