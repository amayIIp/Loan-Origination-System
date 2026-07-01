package com.lending.bre.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


public interface FileStorageService {
    
    
    String storeFile(String applicantId, MultipartFile file) throws IOException;
}