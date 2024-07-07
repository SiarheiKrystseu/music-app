package com.krystseu.microservices.resourceservice.service;

import org.springframework.context.ApplicationEvent;

public class ResourceSavedEvent extends ApplicationEvent {
    private final String resourceId;

    public ResourceSavedEvent(Object source, String resourceId) {
        super(source);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}