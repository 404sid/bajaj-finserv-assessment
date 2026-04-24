package com.quiz.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitRequest(
        @JsonProperty("regNo") String regNo, 
        @JsonProperty("leaderboard") List<LeaderboardEntry> leaderboard) {
}
