package com.krystseu.microservices.storageservice.exception;

public class InvalidStorageException extends RuntimeException {

    public InvalidStorageException(String message) {
        super(message);
    }
}