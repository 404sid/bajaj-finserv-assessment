package com.quiz;

import com.quiz.client.QuizApiClient;
import com.quiz.service.QuizService;

public class Main {
    public static void main(String[] args) {
        String regNo = "2024CS101"; // Registration Number for the quiz task
        boolean isDryRun = false;   // Live Mode (Set to true to skip actual HTTP POST)
        
        QuizApiClient apiClient = new QuizApiClient();
        // Passing 5000ms delay and the isDryRun flag
        QuizService service = new QuizService(apiClient, regNo, 5000, isDryRun);
        
        service.processQuizTask();
    }
}
