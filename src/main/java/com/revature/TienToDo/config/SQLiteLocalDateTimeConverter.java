package com.revature.TienToDo.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter(autoApply = true)
public class SQLiteLocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : null;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String value) {
        if (value == null || value.isEmpty()) return null;

        // Handle Unix timestamp in milliseconds (e.g. "1771087438372")
        try {
            long millis = Long.parseLong(value);
            return Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (NumberFormatException ignored) {}

        // Handle text datetime formats
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Cannot parse datetime: " + value);
    }
}
