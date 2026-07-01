package com.lending.bre.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
public class LocalFileStorageService implements FileStorageService {

    
    private final String UPLOAD_DIR = "uploads/kyc/";

    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public String storeFile(String applicantId, MultipartFile file) throws IOException {
        
        
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        
        
        
        
        Path tempFile = Files.createTempFile("upload_", ".tmp");
        file.transferTo(tempFile.toFile());
        String mimeType = Files.probeContentType(tempFile);
        
        if (mimeType == null || (!mimeType.equals("application/pdf") && !mimeType.startsWith("image/"))) {
            Files.delete(tempFile); 
            throw new IllegalArgumentException("Invalid file type detected. Only PDF/Images allowed.");
        }

        
        
        String extension = getExtension(file.getOriginalFilename());
        String safeFileName = UUID.randomUUID().toString() + extension;

        
        Path targetLocation = Paths.get(UPLOAD_DIR + applicantId).toAbsolutePath().normalize();
        Files.createDirectories(targetLocation);

        
        Path targetFile = targetLocation.resolve(safeFileName);
        Files.move(tempFile, targetFile);

        
        return targetFile.toString();
    }

    
    private String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}