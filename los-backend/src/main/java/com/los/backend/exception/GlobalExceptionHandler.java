package com.los.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;




@RestControllerAdvice


@Slf4j
public class GlobalExceptionHandler {

    
    
    private Map<String, Object> buildBody(int status, String error,
                                          String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        
        body.put("timestamp", Instant.now().toString());
        
        body.put("status", status);
        
        body.put("error", error);
        
        body.put("message", message);
        
        body.put("path", path);
        return body;
    }

    
    
    
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        
        
        log.warn("[404] Resource not found: {} | Path: {}",
                 ex.getMessage(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        body.put("resourceName", ex.getResourceName());
        body.put("fieldName", ex.getFieldName());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    
    
    
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("[400] Validation failed on {} field(s) | Path: {} | Errors: {}",
                 fieldErrors.size(), request.getRequestURI(), fieldErrors);

        Map<String, Object> body = buildBody(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            String.format("Validation failed for %d field(s). Check 'fieldErrors' for details.",
                          fieldErrors.size()),
            request.getRequestURI()
        );
        
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    
    
    
    
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {

        log.warn("[422] Invalid state transition: {} → {} | Path: {}",
                 ex.getFromStatus(), ex.getToStatus(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Unprocessable Entity",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("fromStatus", ex.getFromStatus());
        body.put("toStatus",   ex.getToStatus());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    
    
    
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("[409] Duplicate resource: {} | Path: {}",
                 ex.getMessage(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("duplicateField", ex.getFieldName());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    
    
    
    
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request) {

        log.warn("[422] Business rule violation: {} | Code: {} | Path: {}",
                 ex.getMessage(), ex.getRuleCode(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Unprocessable Entity",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("ruleCode", ex.getRuleCode());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    
    
    
    
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleMongoUniqueConstraint(
            DuplicateKeyException ex,
            HttpServletRequest request) {

        
        log.error("[409] MongoDB unique key violation — possible race condition | Path: {} | {}",
                  request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "A resource with the same unique identifier already exists. " +
            "This may indicate a concurrent submission — please retry.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    
    
    
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format(
            "Parameter '%s' has invalid value '%s'. Expected type: %s",
            ex.getName(),
            ex.getValue(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("[400] Type mismatch: {} | Path: {}", message, request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(buildBody(400, "Bad Request", message, request.getRequestURI()));
    }

    
    
    
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllUncaught(
            Exception ex,
            HttpServletRequest request) {

        
        log.error("[500] Unhandled exception at path: {} | Type: {} | Message: {}",
                  request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        Map<String, Object> body = buildBody(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            
            "An unexpected error occurred. The engineering team has been notified. " +
            "Please try again later or contact support.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
