package com.example.demo.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentProcessingService {
    
    private final Tika tika = new Tika();
    
    public String extractTextFromFile(MultipartFile file) throws IOException, TikaException {
        // Use Apache Tika to extract text from various file formats (PDF, DOCX, TXT, etc.)
        return tika.parseToString(file.getInputStream());
    }
    
    public boolean isSupportedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return false;
        
        return contentType.equals("text/plain") ||
               contentType.equals("application/pdf") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               contentType.equals("application/msword");
    }
    
    public String getFileTypeDescription(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return "Unknown";
        
        switch (contentType) {
            case "text/plain": return "Text File";
            case "application/pdf": return "PDF Document";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return "Word Document (DOCX)";
            case "application/msword": return "Word Document (DOC)";
            default: return "Other Document";
        }
    }
}