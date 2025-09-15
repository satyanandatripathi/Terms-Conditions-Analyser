package com.example.demo.controller;

import com.example.demo.entity.Clause;
import com.example.demo.entity.Document;
import com.example.demo.repository.ClauseRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.service.DocumentProcessingService;
import com.example.demo.service.TextAnalysisService;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
// @CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ClauseRepository clauseRepository;
    
    @Autowired
    private TextAnalysisService textAnalysisService;
    
    @Autowired
    private DocumentProcessingService documentProcessingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a file to upload"));
            }
            
            if (!documentProcessingService.isSupportedFileType(file)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unsupported file type. Please upload PDF, DOCX, DOC, or TXT files."));
            }
            
            // Extract text using Tika
            String content;
            try {
                content = documentProcessingService.extractTextFromFile(file);
            } catch (TikaException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to extract text from file: " + e.getMessage()));
            }
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No readable text found in the uploaded file"));
            }
            
            // Create and save document
            Document document = new Document(file.getOriginalFilename(), content);
            document = documentRepository.save(document);
            
            // Analyze document and save clauses
            List<Clause> clauses = textAnalysisService.analyzeDocument(document);
            if (!clauses.isEmpty()) {
                clauseRepository.saveAll(clauses);
            }
            
            long highRiskCount = clauses.stream()
                .mapToLong(clause -> clause.getRiskScore() >= 0.7 ? 1 : 0)
                .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", document.getId());
            response.put("filename", file.getOriginalFilename());
            response.put("fileType", documentProcessingService.getFileTypeDescription(file));
            response.put("clausesFound", clauses.size());
            response.put("highRiskClauses", highRiskCount);
            response.put("contentLength", content.length());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Processing failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/paste")
    public ResponseEntity<Map<String, Object>> pasteText(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Content cannot be empty"));
            }
            
            if (content.length() < 50) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Text is too short. Please paste a complete terms and conditions document."));
            }
            
            // Create and save document
            Document document = new Document("Pasted Text", content);
            document = documentRepository.save(document);
            
            // Analyze document and save clauses
            List<Clause> clauses = textAnalysisService.analyzeDocument(document);
            if (!clauses.isEmpty()) {
                clauseRepository.saveAll(clauses);
            }
            
            long highRiskCount = clauses.stream()
                .mapToLong(clause -> clause.getRiskScore() >= 0.7 ? 1 : 0)
                .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", document.getId());
            response.put("clausesFound", clauses.size());
            response.put("highRiskClauses", highRiskCount);
            response.put("contentLength", content.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Processing failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{documentId}/clauses")
    public ResponseEntity<?> getClauses(@PathVariable Long documentId) {
        try {
            Optional<Document> document = documentRepository.findById(documentId);
            if (!document.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            List<Clause> clauses = clauseRepository.findByDocumentIdOrderByRiskScoreDesc(documentId);
            return ResponseEntity.ok(clauses);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch clauses: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocument(@PathVariable Long documentId) {
        Optional<Document> document = documentRepository.findById(documentId);
        return document.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/high-risk")
    public ResponseEntity<List<Clause>> getHighRiskClauses(@RequestParam(defaultValue = "0.7") Double minRisk) {
        List<Clause> highRiskClauses = clauseRepository.findHighRiskClauses(minRisk);
        return ResponseEntity.ok(highRiskClauses);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            long totalDocuments = documentRepository.count();
            long totalClauses = clauseRepository.count();
            List<Clause> highRiskClauses = clauseRepository.findHighRiskClauses(0.7);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", totalDocuments);
            stats.put("totalClauses", totalClauses);
            stats.put("highRiskClauses", highRiskClauses.size());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch statistics"));
        }
    }
}
