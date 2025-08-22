package com.guru.pii.controller;

import com.guru.pii.dto.ProcessResponse;
import com.guru.pii.service.OcrMaskerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OcrMaskerController {
    
    @Autowired
    private OcrMaskerService ocrMaskerService;
    
    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> processImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mode") String mode) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ProcessResponse("error", "No file uploaded", null));
            }
            
            if (!isValidImageType(file.getContentType())) {
                return ResponseEntity.badRequest()
                    .body(new ProcessResponse("error", "Invalid image type", null));
            }
            
            ProcessResponse response = ocrMaskerService.processImage(file, mode);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ProcessResponse("error", "Processing failed: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Backend is running");
    }
    
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/png") ||
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/bmp")
        );
    }
}
