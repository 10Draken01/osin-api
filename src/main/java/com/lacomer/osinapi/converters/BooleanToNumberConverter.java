package com.lacomer.osinapi.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanToNumberConverter implements AttributeConverter <Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }

    @Override
    public Boolean convertToEntityAttribute(Integer value) {
        return value == null ? null : value == 1;
    }
}
