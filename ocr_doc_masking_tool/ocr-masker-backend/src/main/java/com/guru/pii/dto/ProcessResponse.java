package com.guru.pii.dto;

public class ProcessResponse {
    private String status;
    private String message;
    private String imageData;
    
    public ProcessResponse() {}
    
    public ProcessResponse(String status, String message, String imageData) {
        this.status = status;
        this.message = message;
        this.imageData = imageData;
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }
}
