package com.krystseu.microservices.resourceprocessor.service;

public class SongMetadataSavedEvent {

    private final Long resourceId;

    public SongMetadataSavedEvent(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getResourceId() {
        return resourceId;
    }
}

