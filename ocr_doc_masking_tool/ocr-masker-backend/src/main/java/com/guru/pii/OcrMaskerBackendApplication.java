package com.guru.pii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OcrMaskerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(OcrMaskerBackendApplication.class, args);
        System.out.println("OCR Masker Backend started on http://localhost:8080");
    }
}
