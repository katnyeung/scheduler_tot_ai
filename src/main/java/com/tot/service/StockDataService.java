package com.tot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching real-time stock data to reduce LLM hallucinations
 * Only activates when ToT content contains stock-related criteria
 */
@Service
public class StockDataService {
    private static final Logger logger = LoggerFactory.getLogger(StockDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final Map<String, Boolean> symbolValidationCache = new HashMap<>();
    
    @Value("${tot.stock.validation.enabled:false}")
    private boolean stockValidationEnabled;
    
    @Value("${tot.stock.api.provider:finnhub}")
    private String stockApiProvider;
    
    @Value("${tot.stock.api.key:}")
    private String stockApiKey;

    public StockDataService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://finnhub.io")
                .build();
    }

    /**
     * Checks if the ToT content contains stock-related criteria
     */
    public boolean containsStockCriteria(String treeJson) {
        if (!stockValidationEnabled) {
            return false;
        }
        
        String lowerContent = treeJson.toLowerCase();
        return lowerContent.contains("stock") || 
               lowerContent.contains("price") || 
               lowerContent.contains("ticker") ||
               lowerContent.contains("symbol") ||
               lowerContent.contains("market") ||
               lowerContent.contains("investment") ||
               containsStockSymbol(treeJson);
    }

    /**
     * Extracts stock symbols from ToT content using regex
     */
    public String[] extractStockSymbols(String treeJson) {
        Pattern pattern = Pattern.compile("\\b[A-Z]{2,5}\\b");
        Matcher matcher = pattern.matcher(treeJson);
        
        return matcher.results()
                .map(MatchResult::group)
                .filter(this::isLikelyStockSymbol)
                .filter(this::isValidStockSymbol)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Enriches user prompt with real stock data before sending to LLM
     */
    public String enrichPromptWithStockData(String originalPrompt, String treeJson, int comparisonDays) {
        if (!containsStockCriteria(treeJson)) {
            logger.debug("No stock criteria detected, using original prompt");
            return originalPrompt;
        }

        String[] symbols = extractStockSymbols(treeJson);
        if (symbols.length == 0) {
            logger.debug("No stock symbols extracted, using original prompt");
            return originalPrompt;
        }

        StringBuilder enrichedPrompt = new StringBuilder(originalPrompt);
        enrichedPrompt.append("\n\n=== STOCK DATA FOR TREE ANALYSIS ===\n");
        enrichedPrompt.append("IMPORTANT: Each stock's metrics are labeled with symbol. Do NOT mix data between stocks.\n\n");

        for (String symbol : symbols) {
            enrichedPrompt.append(String.format("▼▼▼ %s STOCK DATA ▼▼▼\n", symbol));
            
            try {
                StockData currentData = fetchCurrentStockData(symbol);
                StockData historicalData = fetchHistoricalStockData(symbol, comparisonDays);
                
                if (currentData != null && historicalData != null) {
                    enrichedPrompt.append(String.format("Current Price (%s): $%.2f\n", symbol, currentData.price));
                    String formattedMetrics = formatHistoricalMetrics(symbol, historicalData);
                    enrichedPrompt.append(formattedMetrics);
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch data for symbol {}: {}", symbol, e.getMessage());
                enrichedPrompt.append(String.format("%s: Data unavailable - %s\n", symbol, e.getMessage()));
            }
            
            enrichedPrompt.append(String.format("▲▲▲ END %s DATA ▲▲▲\n\n", symbol));
        }

        enrichedPrompt.append("=== REMINDER: Use symbol-specific data only ===\n\n");
        return enrichedPrompt.toString();
    }

    /**
     * Fetches current stock data from Finnhub API
     */
    private StockData fetchCurrentStockData(String symbol) {
        try {
            String url = String.format("/api/v1/quote?symbol=%s&token=%s", symbol, stockApiKey);
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            logger.info("current stock data {}", response);
            return parseFinnhubQuoteResponse(response, symbol);
        } catch (RestClientException e) {
            logger.error("Error fetching current data for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches historical stock data for comparison using Finnhub stock metric API
     * Uses 52-week metrics as historical reference points since candle API requires paid plan
     */
    private StockData fetchHistoricalStockData(String symbol, int daysBack) {
        try {
            String url = String.format("/api/v1/stock/metric?symbol=%s&metric=all&token=%s", symbol, stockApiKey);
            
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            // Filter response to only log the metric data, excluding the large series data
            try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode metricOnly = root.path("metric");
                String filteredResponse = objectMapper.writeValueAsString(metricOnly);
                logger.info("historical stock metrics data (metric only): {}", filteredResponse);
            } catch (Exception e) {
                logger.warn("Could not filter response for logging: {}", e.getMessage());
                logger.info("historical stock metrics data (full response)");
            }
            
            return parseFinnhubMetricResponse(response, symbol);
        } catch (RestClientException e) {
            logger.error("Error fetching historical metrics for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Parses Finnhub quote API response
     */
    private StockData parseFinnhubQuoteResponse(String response, String symbol) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error")) {
                logger.error("Finnhub API error: {}", root.get("error").asText());
                return null;
            }

            double currentPrice = root.path("c").asDouble(); // current price
            double previousClose = root.path("pc").asDouble(); // previous close
            
            // Finnhub quote doesn't provide volume directly, fetch basic financials for market cap
            long marketCap = fetchMarketCap(symbol);
            
            // Use 0 as volume placeholder since it's not in the quote endpoint
            return new StockData(currentPrice, 0, marketCap);
        } catch (Exception e) {
            logger.error("Error parsing Finnhub quote response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses Finnhub stock metric API response for historical data
     * Uses 52-week low as a reference point for historical comparison
     */
    private StockData parseFinnhubMetricResponse(String response, String symbol) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error")) {
                logger.error("Finnhub API error for metrics: {}", root.get("error").asText());
                return null;
            }

            JsonNode metric = root.path("metric");
            if (metric.isMissingNode()) {
                logger.error("Missing metric data in Finnhub response");
                return null;
            }

            // Use 52-week low as historical reference price
            double historicalPrice = metric.path("52WeekLow").asDouble();
            
            // Alternative: use 52-week high if low is not available
            if (historicalPrice == 0) {
                historicalPrice = metric.path("52WeekHigh").asDouble();
            }
            
            // Get average volume if available
            long averageVolume = metric.path("10DayAverageTradingVolume").asLong();
            
            // Get market cap
            long marketCap = metric.path("marketCapitalization").asLong();

            if (historicalPrice == 0) {
                logger.warn("No historical price data available for {}", symbol);
                return null;
            }

            return new StockData(historicalPrice, averageVolume, marketCap);
        } catch (Exception e) {
            logger.error("Error parsing Finnhub metric response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetches market capitalization from Finnhub basic financials
     */
    private long fetchMarketCap(String symbol) {
        try {
            String url = String.format("/api/v1/stock/metric?symbol=%s&metric=all&token=%s", symbol, stockApiKey);
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode metric = root.path("metric");
            
            if (!metric.isMissingNode() && metric.has("marketCapitalization")) {
                return metric.path("marketCapitalization").asLong();
            }
            
            return 0; // Default if not available
        } catch (Exception e) {
            logger.debug("Could not fetch market cap for {}: {}", symbol, e.getMessage());
            return 0;
        }
    }

    /**
     * Simple heuristic to check if a string is likely a stock symbol
     */
    private boolean isLikelyStockSymbol(String token) {
        return token.length() >= 2 && token.length() <= 5 && 
               token.matches("[A-Z]+") && 
               !isCommonWord(token) &&
               !isSingleLetterInvalidSymbol(token);
    }

    private boolean isCommonWord(String token) {
        String[] commonWords = {"THE", "AND", "FOR", "ARE", "BUT", "NOT", "YOU", "ALL", "CAN", "HER", "WAS", "ONE", "OUR", "HAD", "BUT", "WHAT", "SO", "UP", "OUT", "IF", "ABOUT", "WHO", "GET", "WHICH", "GO", "ME", "TO", "OF", "IN", "IT", "IS", "AT", "ON", "AS", "BE", "OR", "AN", "WE", "DO", "BY", "MY", "HE", "US", "NO", "AM", "HI", "OK", "YES", "NO", "OH", "AH", "EH", "UM", "HM"};
        for (String word : commonWords) {
            if (word.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSingleLetterInvalidSymbol(String token) {
        // Common single letters that are not valid stock symbols
        String[] invalidSingleLetters = {"A", "I", "O", "U", "E", "Y", "W", "H", "L", "N", "R", "S", "T", "M", "P", "B", "D", "G", "J", "K", "Q", "V", "X", "Z"};
        if (token.length() == 1) {
            for (String invalid : invalidSingleLetters) {
                if (invalid.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Validates if a stock symbol exists by making a lightweight API call to Finnhub
     * Uses caching to avoid repeated API calls for the same symbol
     */
    private boolean isValidStockSymbol(String symbol) {
        // Check cache first
        if (symbolValidationCache.containsKey(symbol)) {
            return symbolValidationCache.get(symbol);
        }
        
        try {
            String url = String.format("/api/v1/quote?symbol=%s&token=%s", symbol, stockApiKey);
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check if the response contains valid data
            // If the symbol doesn't exist, Finnhub returns current price as 0
            double currentPrice = root.path("c").asDouble();
            
            // Also check if there's any error or if all values are 0 (invalid symbol)
            boolean isValid = currentPrice > 0 && 
                             !root.has("error") && 
                             !root.path("c").isMissingNode();
            
            // Cache the result
            symbolValidationCache.put(symbol, isValid);
            
            logger.debug("Symbol {} validation result: {}", symbol, isValid);
            return isValid;
        } catch (Exception e) {
            logger.debug("Symbol validation failed for {}: {}", symbol, e.getMessage());
            // Cache negative result too
            symbolValidationCache.put(symbol, false);
            return false;
        }
    }

    private boolean containsStockSymbol(String content) {
        String[] symbols = extractStockSymbols(content);
        return symbols.length > 0;
    }

    /**
     * Formats comprehensive historical stock metrics with symbol labeling
     */
    private String formatHistoricalMetrics(String symbol, StockData historicalData) {
        try {
            // Re-fetch the full metrics response to get all historical data
            String url = String.format("/api/v1/stock/metric?symbol=%s&metric=all&token=%s", symbol, stockApiKey);
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode metric = root.path("metric");
            
            if (metric.isMissingNode()) {
                return String.format("%s: No historical metrics available\n", symbol);
            }
            
            StringBuilder formatted = new StringBuilder();
            formatted.append(String.format("=== %s HISTORICAL ANALYSIS DATA ===\n", symbol));
            
            // Volume & Trading Data
            formatted.append(String.format("10-Day Avg Volume (%s): %.2f M\n", symbol, metric.path("10DayAverageTradingVolume").asDouble()));
            formatted.append(String.format("3-Month Avg Volume (%s): %.2f M\n", symbol, metric.path("3MonthAverageTradingVolume").asDouble()));
            
            // Price Performance  
            formatted.append(String.format("52W High (%s): $%.2f\n", symbol, metric.path("52WeekHigh").asDouble()));
            formatted.append(String.format("52W Low (%s): $%.2f\n", symbol, metric.path("52WeekLow").asDouble()));
            formatted.append(String.format("5-Day Return (%s): %.2f%%\n", symbol, metric.path("5DayPriceReturnDaily").asDouble()));
            formatted.append(String.format("13W Return (%s): %.2f%%\n", symbol, metric.path("13WeekPriceReturnDaily").asDouble()));
            formatted.append(String.format("YTD Return (%s): %.2f%%\n", symbol, metric.path("yearToDatePriceReturnDaily").asDouble()));
            
            // Valuation Metrics
            formatted.append(String.format("P/E Ratio (%s): %.1f\n", symbol, metric.path("peTTM").asDouble()));
            formatted.append(String.format("P/B Ratio (%s): %.2f\n", symbol, metric.path("pb").asDouble()));
            formatted.append(String.format("Beta (%s): %.2f\n", symbol, metric.path("beta").asDouble()));
            
            // Growth Metrics
            formatted.append(String.format("Revenue Growth TTM (%s): %.1f%%\n", symbol, metric.path("revenueGrowthTTMYoy").asDouble()));
            formatted.append(String.format("EPS Growth TTM (%s): %.1f%%\n", symbol, metric.path("epsGrowthTTMYoy").asDouble()));
            formatted.append(String.format("Revenue Growth 3Y (%s): %.1f%%\n", symbol, metric.path("revenueGrowth3Y").asDouble()));
            
            // Profitability  
            formatted.append(String.format("Gross Margin (%s): %.1f%%\n", symbol, metric.path("grossMarginTTM").asDouble()));
            formatted.append(String.format("Operating Margin (%s): %.1f%%\n", symbol, metric.path("operatingMarginTTM").asDouble()));
            formatted.append(String.format("Net Margin (%s): %.1f%%\n", symbol, metric.path("netProfitMarginTTM").asDouble()));
            
            // Financial Health
            formatted.append(String.format("Current Ratio (%s): %.2f\n", symbol, metric.path("currentRatioQuarterly").asDouble()));
            formatted.append(String.format("ROE (%s): %.1f%%\n", symbol, metric.path("roeTTM").asDouble()));
            formatted.append(String.format("Market Cap (%s): $%.0f M\n", symbol, metric.path("marketCapitalization").asDouble()));
            
            formatted.append(String.format("=== END %s ANALYSIS DATA ===\n", symbol));
            
            return formatted.toString();
            
        } catch (Exception e) {
            logger.error("Error formatting comprehensive metrics for {}: {}", symbol, e.getMessage());
            return String.format("%s: Historical metrics formatting error\n", symbol);
        }
    }

    /**
     * Data class for stock information
     */
    public static class StockData {
        public final double price;
        public final long volume;
        public final long marketCap;

        public StockData(double price, long volume, long marketCap) {
            this.price = price;
            this.volume = volume;
            this.marketCap = marketCap;
        }
    }
}