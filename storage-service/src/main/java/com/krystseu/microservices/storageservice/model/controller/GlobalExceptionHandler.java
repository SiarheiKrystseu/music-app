package com.krystseu.microservices.storageservice.model.controller;

import com.krystseu.microservices.storageservice.exception.InvalidStorageException;
import com.krystseu.microservices.storageservice.exception.StorageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.InvalidParameterException;
import java.util.Collections;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StorageNotFoundException.class)
    public ResponseEntity<?> handleStorageNotFoundException(StorageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStorageException.class)
    public ResponseEntity<?> handleInvalidStorageException(InvalidStorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<?> handleInvalidParameterException(InvalidParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Internal server error occurred."));
    }
}