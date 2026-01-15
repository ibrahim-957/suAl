package com.delivery.SuAl.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StringToMapConverter implements Converter<String, Map<String, String>> {

    private final ObjectMapper objectMapper;

    public StringToMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, String> convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove any extra whitespace or newlines
            String cleanedSource = source.trim();

            return objectMapper.readValue(cleanedSource, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format for mineralComposition: " + e.getMessage(), e);
        }
    }
}