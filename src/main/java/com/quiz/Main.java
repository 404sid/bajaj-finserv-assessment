package com.quiz;

import com.quiz.client.QuizApiClient;
import com.quiz.service.QuizService;

public class Main {
    public static void main(String[] args) {
        String regNo = "RA2311003010935"; // Test Registration Number to avoid lockouts
        boolean isDryRun = false;       // Dry-Run Flag
        
        QuizApiClient apiClient = new QuizApiClient();
        // Passing 5000ms delay and the isDryRun flag
        QuizService service = new QuizService(apiClient, regNo, 5000, isDryRun);
        
        service.processQuizTask();
    }
}
