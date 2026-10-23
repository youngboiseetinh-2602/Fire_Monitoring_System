package com.firemonitoring.service;

import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.response.PredictionResult;

public interface FirePredictionService {
    PredictionResult predict(DataRequest data);
}
