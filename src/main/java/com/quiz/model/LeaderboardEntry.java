package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaderboardEntry(
        @JsonProperty("participant") String participant, 
        @JsonProperty("totalScore") int totalScore) {
}
