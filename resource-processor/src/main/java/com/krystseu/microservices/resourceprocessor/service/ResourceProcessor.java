package com.krystseu.microservices.resourceprocessor.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.exception.TikaException;
import org.springframework.amqp.core.Message;

import java.io.IOException;

public interface ResourceProcessor {
    void processResourceMessage(Message message) throws IOException, TikaException;
    Metadata extractMetadata(byte[] audioData);
}