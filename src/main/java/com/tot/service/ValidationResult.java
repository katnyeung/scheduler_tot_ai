package com.tot.service;

/**
 * Class to hold validation result and detailed criteria
 */
public class ValidationResult {
    private final String result;
    private final String criteria;

    public ValidationResult(String result, String criteria) {
        this.result = result;
        this.criteria = criteria;
    }

    public String getResult() {
        return result;
    }

    public String getCriteria() {
        return criteria;
    }

    public boolean isPositive() {
        return "true".equalsIgnoreCase(result);
    }
}