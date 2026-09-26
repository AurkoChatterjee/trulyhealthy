package com.trulyhealthy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {}

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    public static Map<String, Object> readBody(InputStream body) throws IOException {
        String text = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        if (text.isBlank()) return Map.of();
        return MAPPER.readValue(text, Map.class);
    }

    public static <T> T readBody(InputStream body, Class<T> type) throws IOException {
        return MAPPER.readValue(body, type);
    }
}
