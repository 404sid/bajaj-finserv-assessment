package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizResponse(String regNo, String setId, int pollIndex, List<Event> events) {
}
