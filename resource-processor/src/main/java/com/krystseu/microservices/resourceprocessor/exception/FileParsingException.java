package com.krystseu.microservices.resourceprocessor.exception;

public class FileParsingException extends RuntimeException {
    public FileParsingException(String message, Exception e) {
        super(message);
    }

    public FileParsingException(String resourceDataCannotBeNull) {
    }
}