package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitResponse(boolean isCorrect, boolean isIdempotent, int submittedTotal, int expectedTotal, String message) {
}
