
## ✨ Features

- **OCR Text Detection**: Uses Tesseract for accurate text recognition
- **Multiple Masking Modes**:
  - 🖤 **BLACK_BOX**: Replaces text with black rectangles
  - 🔵 **BLUR**: Applies Gaussian blur to sensitive areas
  - 🧊 **PIXELATE**: Pixelates the detected text regions
  - 🔤 **TEXT_REPLACE**: Replaces text with "XXXXXX" placeholder
- **Smart Pattern Detection**: Identifies various sensitive data types:
  - Email addresses
  - Phone numbers
  - Monetary values
  - Dates
  - Addresses
  - Account numbers
  - Barcodes
- **Real-time Processing**: Instant processing with live feedback
- **File Upload/Download**: Easy image upload and result download

## 🛠️ Prerequisites

- **Java**: JDK 11 or higher
- **Maven**: 3.6.0 or higher
- **Tesseract OCR**: Installed and configured on your system
- **Web Browser**: Modern browser with JavaScript support

## 🚀 Quick Start

### Backend Setup

0. **Navigate to backend directory**:
   ```bash
   cd backend
OCR Masker - Setup & Usage
==========================

1. Prerequisites
----------------
- Java 11 or higher
- Apache Maven
- Tesseract OCR data files (download from https://github.com/tesseract-ocr/tessdata)

2. Project Setup
----------------
Create project directory:
mkdir ocr-masker-backend
cd ocr-masker-backend

Create directory structure:
mkdir -p src/main/java/com/guru/pii/{controller,service,dto,config}
mkdir -p src/main/resources
mkdir tessdata

Download Tesseract data files:
cd tessdata
wget https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
cd ..

3. Build and Run
----------------
Build the project:
mvn clean package

Run the application:
java -jar target/ocr-masker-backend-1.0.0.jar

Or run with Maven:
mvn spring-boot:run

4. Verify Backend
-----------------
Access in browser:
http://localhost:8080/api/health

Expected response:
Backend is running

5. Frontend Setup
-----------------
Open frontend/ocr-frontend.html in your web browser
OR use a local server (Live Server extension recommended)

Ensure backend is running at http://localhost:8080
Frontend will automatically detect backend status

6. Usage
--------
1. Upload an image file containing sensitive information
2. Select a masking mode
3. Click "Process Document"
4. Download the masked image result

7. API Reference
----------------
POST /api/process
- Process an image with selected masking mode
- Parameters:
  file (MultipartFile) : Image file to process
  mode (String)        : Masking mode (BLACK_BOX, BLUR, PIXELATE, TEXT_REPLACE)
- Response:
  { "status": "success", ... }

GET /api/health
- Health check endpoint
- Response:
  Backend is running

8. Configuration
----------------
Backend Configuration (application.properties):
server.port=8080
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
tessdata.path=tessdata

Tesseract Setup:
Windows: https://github.com/UB-Mannheim/tesseract/wiki
macOS: brew install tesseract
Linux:  sudo apt-get install tesseract-ocr

Download language data files into tessdata/ folder

9. Troubleshooting
------------------
Port 8080 already in use:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

Tesseract not found:
- Ensure Tesseract is installed and added to PATH
- Verify tessdata/ folder contains language files

CORS errors:
- Backend includes CORS configuration for all origins

Large file uploads:
- Adjust max-file-size in application.properties

10. Development
---------------
Build from source:
cd backend
mvn clean package
java -jar target/ocr-masker-backend-1.0.0.jar

Code Structure:
- Controller: Handles HTTP requests and responses
- Service: Contains business logic and OCR processing
- DTO: Data transfer objects for API responses
- Config: Application configuration and CORS setup

11. Contributing
----------------
1. Fork the repository
2. Create a feature branch
3. Make changes
4. Test thoroughly
5. Submit a pull request

12. License
-----------
This project is licensed under the MIT License.

Acknowledgments:
- Spring Boot: Backend framework
- Tesseract OCR: Optical character recognition
- JavaScript: Frontend interactivity

Note:
This application is designed for document security and privacy protection. 
Use responsibly and in compliance with applicable laws and regulations.
