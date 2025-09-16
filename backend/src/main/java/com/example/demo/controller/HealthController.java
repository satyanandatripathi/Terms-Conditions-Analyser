package com.example.demo.controller;

import com.example.demo.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private DocumentRepository documentRepository;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("application", "UP");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            // Check database connection
            try (Connection connection = dataSource.getConnection()) {
                health.put("database", "UP");
                health.put("databaseUrl", connection.getMetaData().getURL());
            } catch (Exception e) {
                health.put("database", "DOWN");
                health.put("databaseError", e.getMessage());
            }
            
            // Check repository
            try {
                long count = documentRepository.count();
                health.put("repository", "UP");
                health.put("documentCount", count);
            } catch (Exception e) {
                health.put("repository", "DOWN");
                health.put("repositoryError", e.getMessage());
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("application", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }
    
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Digital Consent Tracker Backend is running!");
    }
}