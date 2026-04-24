package com.quiz.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.model.QuizResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QuizApiClient {
    private static final Logger log = LoggerFactory.getLogger(QuizApiClient.class);
    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public QuizApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public QuizApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public QuizResponse getMessages(String regNo, int pollIndex) {
        String url = String.format("%s/quiz/messages?regNo=%s&poll=%d", BASE_URL, regNo, pollIndex);
        log.info("Fetching events from: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        int maxAttempts = 4; // 1 initial + 3 retries
        int attempt = 1;

        while (attempt <= maxAttempts) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), QuizResponse.class);
                } else if (response.statusCode() >= 500) {
                    log.error("Server Error (5xx) on poll {}. Attempt {}/{}. Status: {}", pollIndex, attempt, maxAttempts, response.statusCode());
                    if (attempt == maxAttempts) {
                        throw new RuntimeException("API Error during GET (Retries exhausted): " + response.statusCode());
                    }
                    attempt++;
                    Thread.sleep(2000); // 2 second backoff
                } else {
                    log.error("Failed to fetch events. Status Code: {}, Body: {}", response.statusCode(), response.body());
                    throw new RuntimeException("API Error during GET: " + response.statusCode());
                }
            } catch (IOException e) {
                log.error("Network Exception on poll {}. Attempt {}/{}. Error: {}", pollIndex, attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Failed to get messages (Retries exhausted)", e);
                }
                attempt++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Sleep interrupted", ie);
                }
            } catch (InterruptedException e) {
                log.error("Exception occurred while fetching events for poll index {}: {}", pollIndex, e.getMessage());
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to get messages", e);
            }
        }
        throw new RuntimeException("Failed to get messages after retries");
    }

    public SubmitResponse submitLeaderboard(SubmitRequest submitRequest, boolean isDryRun) {
        String url = BASE_URL + "/quiz/submit";
        log.info("Submitting final leaderboard to: {}", url);

        try {
            String jsonPayload = objectMapper.writeValueAsString(submitRequest);
            log.info("Sending Payload: {}", jsonPayload);

            if (isDryRun) {
                log.info("DRY RUN ENABLED - Skipping actual HTTP POST request to validator.");
                return new SubmitResponse(true, true, 0, 0, "Dry Run Success");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Submit Response Status: {}", response.statusCode());
            log.debug("Submit Response Body: {}", response.body());

            if (response.statusCode() >= 400) {
                 log.error("Submission failed. Status: {}, Body: {}", response.statusCode(), response.body());
            }

            return objectMapper.readValue(response.body(), SubmitResponse.class);
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while submitting leaderboard: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to submit leaderboard", e);
        }
    }
    
    // Kept for backwards compatibility with existing test files
    public SubmitResponse submitLeaderboard(SubmitRequest submitRequest) {
        return submitLeaderboard(submitRequest, false);
    }
}
