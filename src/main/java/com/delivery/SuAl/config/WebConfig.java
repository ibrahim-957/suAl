package com.delivery.SuAl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StringToMapConverter stringToMapConverter;
    private final ObjectMapper objectMapper;

    public WebConfig(StringToMapConverter stringToMapConverter, ObjectMapper objectMapper) {
        this.stringToMapConverter = stringToMapConverter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public MappingJackson2HttpMessageConverter octetStreamConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        converter.setSupportedMediaTypes(List.of(
                new MediaType("application", "octet-stream"),
                new MediaType("application", "json")
        ));
        return converter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }
}