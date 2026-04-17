package com.delivery.SuAl.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface DateTimeMapper {

    @Named("utcToBaku")
    default LocalDateTime utcToBaku(LocalDateTime utcTime) {
        if (utcTime == null) return null;

        return utcTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of("Asia/Baku"))
                .toLocalDateTime();
    }
}

