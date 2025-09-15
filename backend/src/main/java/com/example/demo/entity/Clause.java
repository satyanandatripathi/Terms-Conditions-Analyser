package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

@Entity
@Table(name = "clauses")
public class Clause {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "clause_text", columnDefinition = "TEXT")
    private String clauseText;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "risk_score")
    private Double riskScore;
    
    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonBackReference
    private Document document;
    
    // Default constructor
    public Clause() {}
    
    // Custom constructor
    public Clause(String clauseText, String category, Double riskScore, String suggestion, Document document) {
        this.clauseText = clauseText;
        this.category = category;
        this.riskScore = riskScore;
        this.suggestion = suggestion;
        this.document = document;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getClauseText() { return clauseText; }
    public void setClauseText(String clauseText) { this.clauseText = clauseText; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
}