package com.medai.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PredictionRequest {

    private String diseaseType; // heart, stroke, diabetes, lung-tabular, kidney-tabular, liver
    private Map<String, Object> features;
}
