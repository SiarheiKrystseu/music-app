package com.krystseu.microservices.resourceprocessor.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.exception.TikaException;
import java.io.IOException;

public interface ResourceProcessor {
    Metadata processResource(byte[] resourceData) throws IOException, TikaException;
}