package com.krystseu.microservices.resourceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileParsingException extends RuntimeException {
    public FileParsingException(String message, Exception e) {
        super(message);
    }
}