package com.tot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
                .map(result -> result.group())
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
        enrichedPrompt.append("\n\n=== REAL MARKET DATA ===\n");

        for (String symbol : symbols) {
            try {
                StockData currentData = fetchCurrentStockData(symbol);
                StockData historicalData = fetchHistoricalStockData(symbol, comparisonDays);
                
                if (currentData != null && historicalData != null) {
                    double priceChange = currentData.price - historicalData.price;
                    double percentChange = (priceChange / historicalData.price) * 100;
                    
                    enrichedPrompt.append(String.format("""
                        %s:
                        - Current Price: $%.2f
                        - 52-Week Low: $%.2f
                        - Change from 52W Low: $%.2f (%.2f%%)
                        - Average Volume: %,d
                        - Market Cap: $%.2fB
                        
                        """, symbol, currentData.price, historicalData.price,
                        priceChange, percentChange, historicalData.volume,
                        currentData.marketCap / 1_000_000_000.0));
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch data for symbol {}: {}", symbol, e.getMessage());
                enrichedPrompt.append(String.format("%s: Data unavailable\n", symbol));
            }
        }

        enrichedPrompt.append("=== END REAL DATA ===\nUse the above REAL data (not web search) for your analysis.\n\n");
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

            logger.info("historical stock metrics data {}", response);
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