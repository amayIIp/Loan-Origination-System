package com.lending.bre.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/*
 * FILE STORAGE INTERFACE:
 * By programming to an interface, we make our code modular. Right now we use a local disk storage 
 * implementation, but if we later need to switch to Amazon S3 or Google Cloud Storage, 
 * we just create a new class implementing this interface, and no controller code needs to change.
 */
public interface FileStorageService {
    
    // Validates and saves a file securely, returning the path/URL where it can be found.
    String storeFile(String applicantId, MultipartFile file) throws IOException;
}