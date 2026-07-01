package com.lending.bre.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// Implementation of storage that saves files to the local hard drive.
@Service
public class LocalFileStorageService implements FileStorageService {

    // The directory where we will save uploads.
    private final String UPLOAD_DIR = "uploads/kyc/";

    // Maximum file size (5MB).
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public String storeFile(String applicantId, MultipartFile file) throws IOException {
        
        // 1. SECURITY: Server-side file size validation.
        // Never trust the client-side (Angular) validation entirely. A malicious user can bypass it.
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        // 2. SECURITY: Server-side MIME type sniffing.
        // Don't just check the file extension (e.g., .pdf). A hacker can rename an .exe to .pdf.
        // We use Java's Files.probeContentType to sniff the actual file contents (magic bytes).
        // (For enterprise grade, consider Apache Tika library).
        Path tempFile = Files.createTempFile("upload_", ".tmp");
        file.transferTo(tempFile.toFile());
        String mimeType = Files.probeContentType(tempFile);
        
        if (mimeType == null || (!mimeType.equals("application/pdf") && !mimeType.startsWith("image/"))) {
            Files.delete(tempFile); // Clean up
            throw new IllegalArgumentException("Invalid file type detected. Only PDF/Images allowed.");
        }

        // 3. SECURITY: Rename the file to a random UUID.
        // Never use the original file name, as it could contain malicious path traversal payloads (e.g., ../../../etc/passwd).
        String extension = getExtension(file.getOriginalFilename());
        String safeFileName = UUID.randomUUID().toString() + extension;

        // Ensure the upload directory exists.
        Path targetLocation = Paths.get(UPLOAD_DIR + applicantId).toAbsolutePath().normalize();
        Files.createDirectories(targetLocation);

        // Move the file from the temp location to its final destination.
        Path targetFile = targetLocation.resolve(safeFileName);
        Files.move(tempFile, targetFile);

        // Return a mock URL/path that could be stored in the database.
        return targetFile.toString();
    }

    // Helper method to safely extract the extension.
    private String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}