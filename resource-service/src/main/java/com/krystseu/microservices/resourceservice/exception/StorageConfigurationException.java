package com.krystseu.microservices.resourceservice.exception;

public class StorageConfigurationException extends RuntimeException {
    public StorageConfigurationException(String message) {
        super(message);
    }

    public StorageConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

