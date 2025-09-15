package com.example.demo.repository;

import com.example.demo.entity.Clause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseRepository extends JpaRepository<Clause, Long> {
    
    @Query("SELECT c FROM Clause c WHERE c.document.id = :documentId ORDER BY c.riskScore DESC")
    List<Clause> findByDocumentIdOrderByRiskScoreDesc(@Param("documentId") Long documentId);
    
    @Query("SELECT c FROM Clause c WHERE c.riskScore >= :minRisk ORDER BY c.riskScore DESC")
    List<Clause> findHighRiskClauses(@Param("minRisk") Double minRisk);
}