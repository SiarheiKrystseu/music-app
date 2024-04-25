package com.krystseu.microservices.resourceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AudioUploadingException extends RuntimeException {
    public AudioUploadingException(String message, Exception e) {
        super(message);
    }

    public AudioUploadingException(String failedToUploadFile) {
    }
}
