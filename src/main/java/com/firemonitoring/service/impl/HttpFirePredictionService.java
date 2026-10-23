package com.firemonitoring.service.impl;

import com.firemonitoring.exception.ServiceUnavailableException;
import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.response.PredictionResult;
import com.firemonitoring.service.FirePredictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HttpFirePredictionService implements FirePredictionService {
    private final RestClient restClient;

    public HttpFirePredictionService(RestClient.Builder builder,
                                     @Value("${prediction.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public PredictionResult predict(DataRequest data) {
        try {
            PredictionResult result = restClient.post().uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON).body(data).retrieve()
                    .body(PredictionResult.class);
            if (result == null || result.label() == null || !Double.isFinite(result.confidence())
                    || result.confidence() < 0 || result.confidence() > 1) {
                throw new ServiceUnavailableException("Prediction service returned an invalid response");
            }
            return result;
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("Fire prediction service is unavailable", ex);
        }
    }
}
