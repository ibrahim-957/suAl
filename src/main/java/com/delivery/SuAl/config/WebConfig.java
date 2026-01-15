package com.delivery.SuAl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StringToMapConverter stringToMapConverter;
    private final JsonRequestPartResolver jsonRequestPartResolver;

    public WebConfig(StringToMapConverter stringToMapConverter, JsonRequestPartResolver jsonRequestPartResolver) {
        this.stringToMapConverter = stringToMapConverter;
        this.jsonRequestPartResolver = jsonRequestPartResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(jsonRequestPartResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }
}