package com.delivery.SuAl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StringToMapConverter stringToMapConverter;
    private final ObjectMapper objectMapper;

    public WebConfig(StringToMapConverter stringToMapConverter, ObjectMapper objectMapper) {
        this.stringToMapConverter = stringToMapConverter;
        this.objectMapper = objectMapper;

        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                .map(c -> (MappingJackson2HttpMessageConverter) c)
                .findFirst()
                .ifPresent(jsonConverter -> {
                    List<MediaType> supportedTypes = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
                    supportedTypes.add(new MediaType("application", "octet-stream"));
                    jsonConverter.setSupportedMediaTypes(supportedTypes);
                });
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }
}