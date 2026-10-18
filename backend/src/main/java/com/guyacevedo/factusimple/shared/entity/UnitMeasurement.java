package com.guyacevedo.factusimple.shared.entity;

import java.util.Arrays;
import com.guyacevedo.factusimple.features.product.entity.SaleType;

public enum UnitMeasurement {
    UNIDAD("und", "94", SaleType.BY_UNIT),
    SET("set", "SET", SaleType.BY_UNIT),
    DOCENA("docena", "DZN", SaleType.BY_UNIT),
    DOBLE_DOCENA("veinticuatro", "DPR", SaleType.BY_UNIT),

    KILOGRAMO("kg", "KGM", SaleType.BY_WEIGHT),
    LIBRA("lb", "LBR", SaleType.BY_WEIGHT),
    GRAMO("gr", "GRM", SaleType.BY_WEIGHT),
    ONZA("onz", "ONZ", SaleType.BY_WEIGHT),

    LITRO("litro", "LTR", SaleType.BY_VOLUME),
    MILILITRO("ml", "MLT", SaleType.BY_VOLUME),
    GALON("galon", "GLL", SaleType.BY_VOLUME);

    private final String abbreviation;
    private final String code;
    private final SaleType saleType;

    UnitMeasurement(String abbreviation, String code, SaleType saleType) {
        this.abbreviation = abbreviation;
        this.code = code;
        this.saleType = saleType;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String code() {
        return code;
    }

    public SaleType saleType() {
        return saleType;
    }

    public boolean isConsistentWith(SaleType mode) {
        return this.saleType == mode;
    }

    public static UnitMeasurement fromCode(String code) {
        return Arrays.stream(values())
                .filter(u -> u.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida desconocida: " + code));
    }
}