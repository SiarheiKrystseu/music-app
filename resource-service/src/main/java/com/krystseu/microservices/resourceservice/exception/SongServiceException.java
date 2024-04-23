package com.krystseu.microservices.resourceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SongServiceException extends RuntimeException {
    public SongServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SongServiceException(String songServiceIsNotAvailable) {
    }
}
