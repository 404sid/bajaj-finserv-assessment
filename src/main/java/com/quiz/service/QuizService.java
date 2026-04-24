package com.quiz.service;

import com.quiz.client.QuizApiClient;
import com.quiz.model.Event;
import com.quiz.model.LeaderboardEntry;
import com.quiz.model.QuizResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QuizService {
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private static final int TOTAL_POLLS = 10;
    
    private final QuizApiClient apiClient;
    private final String regNo;
    private final long delayMs;
    private final boolean isDryRun;

    public QuizService(QuizApiClient apiClient, String regNo) {
        this(apiClient, regNo, 5000, false); // Default mandatory 5 seconds delay
    }

    public QuizService(QuizApiClient apiClient, String regNo, long delayMs) {
        this(apiClient, regNo, delayMs, false);
    }

    public QuizService(QuizApiClient apiClient, String regNo, long delayMs, boolean isDryRun) {
        this.apiClient = apiClient;
        this.regNo = regNo;
        this.delayMs = delayMs;
        this.isDryRun = isDryRun;
    }

    public void processQuizTask() {
        log.info("Starting Quiz Task for Registration Number: {} (Dry Run: {})", regNo, isDryRun);
        
        Set<String> processedEvents = new HashSet<>();
        Map<String, Integer> participantScores = new HashMap<>();

        int totalEventsFetched = 0;
        int duplicatesIgnored = 0;

        for (int i = 0; i < TOTAL_POLLS; i++) {
            log.info("--- Executing Poll {}/{} ---", i + 1, TOTAL_POLLS);
            try {
                QuizResponse response = apiClient.getMessages(regNo, i);
                
                if (response != null && response.events() != null) {
                    totalEventsFetched += response.events().size();
                    for (Event event : response.events()) {
                        String compositeKey = event.roundId() + "-" + event.participant();
                        
                        if (processedEvents.add(compositeKey)) {
                            participantScores.merge(event.participant(), event.score(), Integer::sum);
                            log.debug("Processed new event: {}", event);
                        } else {
                            duplicatesIgnored++;
                            log.debug("Ignored duplicate event: {}", event);
                        }
                    }
                } else {
                    log.warn("Received empty response or null events for poll index {}", i);
                }
            } catch (Exception e) {
                log.error("Error during poll index {}: {}", i, e.getMessage());
            }

            if (i < TOTAL_POLLS - 1) {
                log.info("Waiting {} ms before next poll...", delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    log.error("Sleep interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    return; 
                }
            }
        }

        log.info("All polls completed. Aggregating results...");

        List<LeaderboardEntry> leaderboard = participantScores.entrySet().stream()
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(LeaderboardEntry::totalScore).reversed()) // Sort descending
                .collect(Collectors.toList());

        int finalLeaderboardSize = leaderboard.size();
        int grandTotal = leaderboard.stream().mapToInt(LeaderboardEntry::totalScore).sum();
        
        log.info("========== RUN STATISTICS ==========");
        log.info("Total Events Fetched:   {}", totalEventsFetched);
        log.info("Duplicates Ignored:     {}", duplicatesIgnored);
        log.info("Final Leaderboard Size: {}", finalLeaderboardSize);
        log.info("Grand Total Score:      {}", grandTotal);
        log.info("====================================");

        log.info("");
        log.info("+------+-----------------+-------------+");
        log.info("| Rank | Participant     | Total Score |");
        log.info("+------+-----------------+-------------+");
        int rank = 1;
        for (LeaderboardEntry entry : leaderboard) {
            String name = entry.participant();
            if (name != null && name.length() > 15) {
                name = name.substring(0, 12) + "...";
            } else if (name == null) {
                name = "Unknown";
            }
            log.info(String.format("| %-4d | %-15s | %-11d |", rank++, name, entry.totalScore()));
        }
        log.info("+------+-----------------+-------------+");
        log.info("");

        SubmitRequest submitRequest = new SubmitRequest(regNo, leaderboard);
        
        try {
            SubmitResponse submitResponse = apiClient.submitLeaderboard(submitRequest, isDryRun);
            log.info("Final Submission Result:");
            log.info("Is Correct: {}", submitResponse.isCorrect());
            log.info("Message: {}", submitResponse.message());
            log.info("Submitted Total: {}, Expected Total: {}", submitResponse.submittedTotal(), submitResponse.expectedTotal());
            log.info("Is Idempotent: {}", submitResponse.isIdempotent());
        } catch (Exception e) {
            log.error("Failed to submit leaderboard: {}", e.getMessage());
        }
    }
}
