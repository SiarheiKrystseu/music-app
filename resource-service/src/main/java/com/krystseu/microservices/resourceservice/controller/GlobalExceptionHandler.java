package com.krystseu.microservices.resourceservice.controller;

import com.krystseu.microservices.resourceservice.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.FieldError;


import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<String> handleInvalidFileException(InvalidFileException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return new ResponseEntity<>("An internal server error has occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage).toList();
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNoResourceFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The requested resource was not found");
    }

    @ExceptionHandler(FileParsingException.class)
    public ResponseEntity<String> handleFileParsingException(FileParsingException e) {
        return new ResponseEntity<>("Error while parsing the file", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileUploadingException.class)
    public ResponseEntity<String> handleFileUploadingException(FileUploadingException e) {
        String errorMessage = "Failed to upload file. " + e.getMessage();
        return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SongServiceException.class)
    public ResponseEntity<String> handleSongServiceException(SongServiceException e) {
        return new ResponseEntity<>("Error while calling the song service", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}