package com.guyacevedo.factusimple.shared.entity;

import java.util.Arrays;
import com.guyacevedo.factusimple.features.product.entity.SaleType;

public enum UnitMeasurement {
    UNIDAD("und", "94", "unidad", SaleType.BY_UNIT),
    SET("set", "SET", "conjunto/set", SaleType.BY_UNIT),
    DOCENA("docena", "DZN", "docena", SaleType.BY_UNIT),
    DOBLE_DOCENA("veinticuatro", "DPR", "docena par (24 unidades)", SaleType.BY_UNIT),

    KILOGRAMO("kg", "KGM", "kilogramo", SaleType.BY_WEIGHT),
    LIBRA("lb", "LBR", "libra", SaleType.BY_WEIGHT),
    GRAMO("gr", "GRM", "gramo", SaleType.BY_WEIGHT),
    ONZA("onz", "ONZ", "onza", SaleType.BY_WEIGHT),

    LITRO("litro", "LTR", "litro", SaleType.BY_VOLUME),
    MILILITRO("ml", "MLT", "mililitro", SaleType.BY_VOLUME),
    GALON("galon", "GLL", "galón", SaleType.BY_VOLUME);

    private final String abbreviation;
    private final String code;
    /** Name as Factus lists the unit (unit measures catalog). */
    private final String label;
    private final SaleType saleType;

    UnitMeasurement(String abbreviation, String code, String label, SaleType saleType) {
        this.abbreviation = abbreviation;
        this.code = code;
        this.label = label;
        this.saleType = saleType;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
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