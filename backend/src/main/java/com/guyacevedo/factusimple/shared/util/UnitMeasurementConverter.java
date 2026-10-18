package com.guyacevedo.factusimple.shared.util;

import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps UnitMeasurement to its DIAN code ("94", "KGM", ...) in the database. */
@Converter
public class UnitMeasurementConverter implements AttributeConverter<UnitMeasurement, String> {

    @Override
    public String convertToDatabaseColumn(UnitMeasurement unit) {
        return unit == null ? null : unit.code();
    }

    @Override
    public UnitMeasurement convertToEntityAttribute(String code) {
        return code == null ? null : UnitMeasurement.fromCode(code);
    }
}