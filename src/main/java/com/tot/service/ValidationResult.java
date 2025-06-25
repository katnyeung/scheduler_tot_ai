package com.tot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to hold validation result and detailed criteria with data source tracking
 */
public class ValidationResult {
    private final String result;
    private final String criteria;
    private final List<String> dataSources;
    private final Double confidenceScore;
    private final LocalDateTime dataTimestamp;
    private final boolean stockDataEnriched;

    public ValidationResult(String result, String criteria) {
        this.result = result;
        this.criteria = criteria;
        this.dataSources = new ArrayList<>();
        this.confidenceScore = null;
        this.dataTimestamp = LocalDateTime.now();
        this.stockDataEnriched = false;
    }

    public ValidationResult(String result, String criteria, List<String> dataSources, 
                          Double confidenceScore, boolean stockDataEnriched) {
        this.result = result;
        this.criteria = criteria;
        this.dataSources = dataSources != null ? new ArrayList<>(dataSources) : new ArrayList<>();
        this.confidenceScore = confidenceScore;
        this.dataTimestamp = LocalDateTime.now();
        this.stockDataEnriched = stockDataEnriched;
    }

    public String getResult() {
        return result;
    }

    public String getCriteria() {
        return criteria;
    }

    public List<String> getDataSources() {
        return new ArrayList<>(dataSources);
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public LocalDateTime getDataTimestamp() {
        return dataTimestamp;
    }

    public boolean isStockDataEnriched() {
        return stockDataEnriched;
    }

    public boolean isPositive() {
        return "true".equalsIgnoreCase(result);
    }

    public void addDataSource(String source) {
        if (source != null && !dataSources.contains(source)) {
            dataSources.add(source);
        }
    }
}