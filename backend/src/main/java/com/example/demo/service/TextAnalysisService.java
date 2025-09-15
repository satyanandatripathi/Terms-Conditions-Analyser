package com.example.demo.service;

import com.example.demo.entity.Clause;
import com.example.demo.entity.Document;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class TextAnalysisService {
    
    // Risk keywords and their associated risk scores
    private final Map<String, Double> riskKeywords = new HashMap<String, Double>() {{
        // Data Collection & Usage - High Risk
        put("collect.*personal data", 0.9);
        put("share.*third parties", 0.9);
        put("sell.*information", 0.95);
        put("location.*tracking", 0.85);
        put("biometric", 0.9);
        put("indefinitely", 0.85);
        put("unlimited", 0.8);
        put("permanent", 0.8);
        put("irrevocable", 0.9);
        
        // High-risk data sharing
        put("share.*without.*consent", 0.95);
        put("transfer.*overseas", 0.8);
        put("affiliate.*company", 0.7);
        put("business.*partner", 0.7);
        
        // Tracking - Medium-High Risk
        put("cookies", 0.6);
        put("analytics", 0.5);
        put("advertising", 0.7);
        put("marketing", 0.6);
        put("tracking", 0.7);
        put("behavioral", 0.75);
        put("cross.*device", 0.8);
        put("fingerprint", 0.85);
        
        // Rights & Control - High Risk
        put("cannot.*opt.out", 0.9);
        put("no.*control", 0.8);
        put("automatic.*renewal", 0.7);
        put("no.*refund", 0.75);
        put("terminate.*account", 0.6);
        put("suspend.*service", 0.6);
        put("delete.*account", 0.5);
        
        // Legal & Liability - Medium-High Risk
        put("not.*liable", 0.7);
        put("waive.*rights", 0.85);
        put("arbitration", 0.6);
        put("class.*action", 0.65);
        put("governing.*law", 0.3);
        put("disclaim.*warranty", 0.7);
        put("limitation.*liability", 0.6);
        
        // Changes & Modifications - Medium Risk
        put("modify.*terms", 0.5);
        put("change.*policy", 0.4);
        put("without.*notice", 0.8);
        put("sole.*discretion", 0.7);
        put("at.*any.*time", 0.6);
        
        // Financial & Subscription Terms
        put("auto.*renew", 0.7);
        put("recurring.*charge", 0.6);
        put("cancellation.*fee", 0.8);
        put("early.*termination", 0.7);
    }};
    
    // Category classification keywords
    private final Map<String, String> categoryKeywords = new HashMap<String, String>() {{
        put("data|information|personal|collect|store|process", "Data Collection");
        put("share|third.party|partner|affiliate|sell", "Data Sharing");
        put("track|cookie|analytics|advertising|marketing", "Tracking & Analytics");
        put("cancel|terminate|refund|subscription", "Cancellation & Refunds");
        put("liable|responsibility|warranty|damages", "Liability & Warranties");
        put("modify|change|update|amend", "Terms Modification");
        put("location|gps|geolocation", "Location Services");
        put("arbitration|dispute|court|legal", "Legal & Disputes");
        put("payment|billing|charge|fee", "Payment Terms");
        put("account|profile|user", "Account Management");
    }};
    
    public List<Clause> analyzeDocument(Document document) {
        List<Clause> clauses = new ArrayList<>();
        String content = document.getContent().toLowerCase();
        
        // Split content into sentences for analysis
        String[] sentences = content.split("[.!?]+");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 15) continue; // Skip very short sentences
            
            double riskScore = calculateRiskScore(sentence);
            if (riskScore > 0.15) { // Lower threshold to catch more concerning clauses
                String category = categorizeClause(sentence);
                String suggestion = generateSuggestion(category, riskScore);
                
                // Capitalize the first letter of the sentence for better display
                String displaySentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);
                
                Clause clause = new Clause(displaySentence, category, riskScore, suggestion, document);
                clauses.add(clause);
            }
        }
        
        // Sort by risk score descending
        clauses.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
        
        // Limit to top 25 clauses to avoid overwhelming the user but include more potential issues
        return clauses.size() > 25 ? clauses.subList(0, 25) : clauses;
    }
    
    private double calculateRiskScore(String text) {
        double maxRisk = 0.0;
        
        for (Map.Entry<String, Double> entry : riskKeywords.entrySet()) {
            if (Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                maxRisk = Math.max(maxRisk, entry.getValue());
            }
        }
        
        // Add bonus risk for multiple concerning patterns
        long concerningPatterns = riskKeywords.keySet().stream()
            .mapToLong(keyword -> Pattern.compile(keyword, Pattern.CASE_INSENSITIVE)
                .matcher(text).find() ? 1 : 0)
            .sum();
        
        if (concerningPatterns > 1) {
            maxRisk = Math.min(1.0, maxRisk + (concerningPatterns - 1) * 0.1);
        }
        
        // Boost risk for certain high-concern words
        if (text.contains("sell") && (text.contains("data") || text.contains("information"))) {
            maxRisk = Math.max(maxRisk, 0.9);
        }
        
        if (text.contains("cannot") && text.contains("opt")) {
            maxRisk = Math.max(maxRisk, 0.85);
        }
        
        return maxRisk;
    }
    
    private String categorizeClause(String text) {
        for (Map.Entry<String, String> entry : categoryKeywords.entrySet()) {
            if (Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                return entry.getValue();
            }
        }
        return "General Terms";
    }
    
    private String generateSuggestion(String category, double riskScore) {
        Map<String, String> suggestions = new HashMap<String, String>() {{
            put("Data Collection", "Review what personal data is collected and if it's necessary for the service. Check if you can limit data collection.");
            put("Data Sharing", "Check if you can opt-out of data sharing with third parties. Understand who your data is shared with.");
            put("Tracking & Analytics", "Look for cookie preferences or tracking opt-out options in privacy settings.");
            put("Cancellation & Refunds", "Understand the cancellation process, notice periods, and refund policy before subscribing.");
            put("Liability & Warranties", "Be aware of limited liability clauses that may affect your legal rights in case of issues.");
            put("Terms Modification", "Check how you'll be notified of changes to terms and your options if you disagree with changes.");
            put("Location Services", "Consider if location tracking is necessary for the service and review location privacy settings.");
            put("Legal & Disputes", "Understand dispute resolution processes, arbitration clauses, and your legal rights.");
            put("Payment Terms", "Review billing cycles, automatic renewals, and cancellation fees before agreeing to paid services.");
            put("Account Management", "Understand account termination policies and what happens to your data when you close your account.");
        }};
        
        String baseSuggestion = suggestions.getOrDefault(category, "Review this clause carefully and consider its implications.");
        
        if (riskScore >= 0.7) {
            return "HIGH RISK: " + baseSuggestion + " Consider if you're comfortable accepting these terms or if alternatives exist.";
        } else if (riskScore >= 0.5) {
            return "MEDIUM RISK: " + baseSuggestion + " Weigh the benefits against potential privacy concerns.";
        } else if (riskScore >= 0.25) {
            return "LOW RISK: " + baseSuggestion;
        } else {
            return baseSuggestion;
        }
    }
}
