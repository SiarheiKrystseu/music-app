package com.krystseu.microservices.resourceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FileUploadingException extends RuntimeException {
    public FileUploadingException(String message, IOException e) {
        super(message);
    }

    public FileUploadingException(String failedToUploadFile) {
    }
}
