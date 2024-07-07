package com.krystseu.microservices.resourceprocessor.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.exception.TikaException;
import java.io.IOException;

public interface ResourceProcessor {
    void processResourceMessage(String resourceId) throws IOException, TikaException;
}