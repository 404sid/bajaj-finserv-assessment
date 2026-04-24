package com.quiz.service;

import com.quiz.client.QuizApiClient;
import com.quiz.model.Event;
import com.quiz.model.LeaderboardEntry;
import com.quiz.model.QuizResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizApiClient apiClient;

    private QuizService quizService;

    private final String regNo = "TEST12345";

    @BeforeEach
    void setUp() {
        // Using 0 ms delay for instant unit tests
        quizService = new QuizService(apiClient, regNo, 0);
    }

    @Test
    void testProcessQuizTask_DeduplicationAndAggregation() {
        // Mock Poll 0
        QuizResponse r0 = new QuizResponse(regNo, "S1", 0, List.of(
                new Event("R1", "Alice", 10),
                new Event("R1", "Bob", 20)
        ));
        when(apiClient.getMessages(regNo, 0)).thenReturn(r0);

        // Mock Poll 1 (Contains a duplicate for R1-Alice, and new R2-Alice)
        QuizResponse r1 = new QuizResponse(regNo, "S1", 1, List.of(
                new Event("R1", "Alice", 10), // DUPLICATE, should be ignored
                new Event("R2", "Alice", 50),
                new Event("R2", "Charlie", 30)
        ));
        when(apiClient.getMessages(regNo, 1)).thenReturn(r1);

        // Mock Remaining Polls 2-9 with empty responses
        for (int i = 2; i < 10; i++) {
            when(apiClient.getMessages(regNo, i)).thenReturn(new QuizResponse(regNo, "S1", i, List.of()));
        }

        // Mock Submit Response
        SubmitResponse mockSubmitResponse = new SubmitResponse(true, true, 110, 110, "Correct");
        when(apiClient.submitLeaderboard(any())).thenReturn(mockSubmitResponse);

        // Execute
        quizService.processQuizTask();

        // Verify API was polled exactly 10 times
        verify(apiClient, times(10)).getMessages(eq(regNo), anyInt());

        // Capture submit request to verify logic
        ArgumentCaptor<SubmitRequest> captor = ArgumentCaptor.forClass(SubmitRequest.class);
        verify(apiClient, times(1)).submitLeaderboard(captor.capture());

        SubmitRequest submitRequest = captor.getValue();
        assertEquals(regNo, submitRequest.regNo());

        List<LeaderboardEntry> leaderboard = submitRequest.leaderboard();
        
        // Expected Scores:
        // Alice: 10 (R1) + 50 (R2) = 60
        // Charlie: 30 (R2) = 30
        // Bob: 20 (R1) = 20
        
        assertEquals(3, leaderboard.size(), "Leaderboard should have 3 unique participants");

        // Assuming descending order by totalScore
        assertEquals("Alice", leaderboard.get(0).participant());
        assertEquals(60, leaderboard.get(0).totalScore());

        assertEquals("Charlie", leaderboard.get(1).participant());
        assertEquals(30, leaderboard.get(1).totalScore());

        assertEquals("Bob", leaderboard.get(2).participant());
        assertEquals(20, leaderboard.get(2).totalScore());
    }
}
