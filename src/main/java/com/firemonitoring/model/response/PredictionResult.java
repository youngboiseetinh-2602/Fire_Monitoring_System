package com.firemonitoring.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.firemonitoring.enums.EventType;

public record PredictionResult(EventType label, @JsonProperty("class_id") int classId, double confidence) {
}
