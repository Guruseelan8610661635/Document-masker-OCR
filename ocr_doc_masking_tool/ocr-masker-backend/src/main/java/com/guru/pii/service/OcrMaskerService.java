package com.guru.pii.service;

import com.guru.pii.dto.ProcessResponse;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OcrMaskerService {
    
    public enum MaskingMode {
        BLACK_BOX, BLUR, PIXELATE, TEXT_REPLACE
    }
    
    // Regex patterns (from original code)
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$?\\d{1,3}(,\\d{3})*(\\.\\d{2})?");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{2}/\\d{2}/(\\d{2}|\\d{4})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\\d+\\s+\\w+(\\s+\\w+)*,?\\s+[A-Z]{2}\\s+\\d{5}(-\\d{4})?");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("\\b\\d{10,}\\b");
    
    // Context keywords
    private static final List<String> ACCOUNT_KEYWORDS = Arrays.asList("Account", "Acct", "Acc");
    private static final List<String> BILL_KEYWORDS = Arrays.asList("Bill", "Invoice", "Lading", "P.O.", "PO", "Freight");
    private static final List<String> SEGMENT_KEYWORDS = Arrays.asList("Origin", "Destination", "Origin/Destination");
    
    public ProcessResponse processImage(MultipartFile file, String modeStr) throws Exception {
        try {
            // Convert MultipartFile to BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file");
            }
            
            // Parse masking mode
            MaskingMode mode = MaskingMode.valueOf(modeStr.toUpperCase());
            
            // Initialize Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
            
            // Perform OCR
            List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            
            // Apply masking
            BufferedImage maskedImage = deepCopy(image);
            Graphics2D g2d = maskedImage.createGraphics();
            
            for (int i = 0; i < words.size(); i++) {
                Word word = words.get(i);
                String text = word.getText().trim();
                if (text.isEmpty()) continue;
                
                // Multi-token long number detection
                StringBuilder sb = new StringBuilder(text);
                int j = i + 1;
                while (j < words.size() && words.get(j).getText().trim().matches("\\d+")) {
                    sb.append(words.get(j).getText().trim());
                    j++;
                }
                String combined = sb.toString();
                if (LONG_NUMBER_PATTERN.matcher(combined).matches()) {
                    for (int k = i; k < j; k++) {
                        maskWord(g2d, image, words.get(k), mode);
                    }
                    i = j - 1;
                    continue;
                }
                
                // Single-token patterns
                if (MONEY_PATTERN.matcher(text).matches()
                 || DATE_PATTERN.matcher(text).matches()
                 || PHONE_PATTERN.matcher(text).matches()
                 || EMAIL_PATTERN.matcher(text).matches()
                 || ADDRESS_PATTERN.matcher(text).matches()) {
                    maskWord(g2d, image, word, mode);
                    continue;
                }
                
                // Value after "#"
                if (text.equals("#") && i + 1 < words.size()) {
                    maskWord(g2d, image, words.get(i + 1), mode);
                    continue;
                }
                
                // Mask next token after segment keywords
                String clean = text.replaceAll("[^A-Za-z/]", "");
                if (SEGMENT_KEYWORDS.contains(clean) && i + 1 < words.size()) {
                    maskWord(g2d, image, words.get(i + 1), mode);
                    continue;
                }
                
                // Account or bill context
                if (ACCOUNT_PATTERN.matcher(text).matches()) {
                    if (isNearKeyword(words, i, ACCOUNT_KEYWORDS, 5)
                     || isNearKeyword(words, i, BILL_KEYWORDS, 5)) {
                        maskWord(g2d, image, word, mode);
                        continue;
                    }
                }
                
                // Heuristic company names (ALL CAPS, length > 3)
                if (text.equals(text.toUpperCase()) && text.length() > 3) {
                    int k = i;
                    while (k < words.size()
                        && words.get(k).getText().equals(words.get(k).getText().toUpperCase())) {
                        maskWord(g2d, image, words.get(k), mode);
                        k++;
                    }
                    i = k - 1;
                }
                
                // Document numbers near bottom
                if (i > words.size() - 10
                 && ACCOUNT_PATTERN.matcher(text).matches()) {
                    maskWord(g2d, image, word, mode);
                }
            }
            
            maskVisualBarcode(image, g2d, mode);
            g2d.dispose();
            
            // Convert to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(maskedImage, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            
            return new ProcessResponse("success", "Image processed successfully", base64Image);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
        }
    }
    
    // All the masking utility methods from the original code...
    private void maskWord(Graphics2D g2d, BufferedImage image, Word word, MaskingMode mode) {
        Rectangle rect = word.getBoundingBox();
        maskRegion(g2d, image, rect, mode);
    }
    
    private void maskRegion(Graphics2D g2d, BufferedImage image, Rectangle rect, MaskingMode mode) {
        switch (mode) {
            case BLACK_BOX:
                g2d.setColor(Color.BLACK);
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                break;
                
            case BLUR:
                BufferedImage sub = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
                float[] kernel = new float[9];
                Arrays.fill(kernel, 1f / 9f);
                ConvolveOp blurOp = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
                BufferedImage blurred = blurOp.filter(sub, null);
                g2d.drawImage(blurred, rect.x, rect.y, null);
                break;
                
            case PIXELATE:
                BufferedImage tile = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
                int w = Math.max(1, rect.width / 10);
                int h = Math.max(1, rect.height / 10);
                Image small = tile.getScaledInstance(w, h, Image.SCALE_FAST);
                BufferedImage pix = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D pg = pix.createGraphics();
                pg.drawImage(small, 0, 0, rect.width, rect.height, null);
                pg.dispose();
                g2d.drawImage(pix, rect.x, rect.y, null);
                break;
                
            case TEXT_REPLACE:
                g2d.setColor(Color.WHITE);
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, rect.height / 2));
                g2d.drawString("XXXXXX", rect.x + 5, rect.y + rect.height / 2);
                break;
        }
    }
    
    private void maskVisualBarcode(BufferedImage image, Graphics2D g2d, MaskingMode mode) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        for (int y = 0; y < height - 60; y += 10) {
            for (int x = 0; x < width - 200; x += 10) {
                int black = 0, total = 0;
                for (int dy = 0; dy < 60; dy++) {
                    for (int dx = 0; dx < 200; dx++) {
                        Color c = new Color(image.getRGB(x + dx, y + dy));
                        int brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                        if (brightness < 80) black++;
                        total++;
                    }
                }
                if (black / (double) total > 0.5) {
                    maskRegion(g2d, image, new Rectangle(x, y, 200, 60), mode);
                    y += 60;
                    break;
                }
            }
        }
    }
    
    private boolean isNearKeyword(List<Word> words, int index, List<String> keywords, int distance) {
        for (int i = Math.max(0, index - distance); i <= Math.min(words.size() - 1, index + distance); i++) {
            String t = words.get(i).getText().replaceAll("\\W", "");
            for (String kw : keywords) {
                if (kw.equalsIgnoreCase(t)) return true;
            }
        }
        return false;
    }
    
    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean premult = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, premult, null);
    }
}
