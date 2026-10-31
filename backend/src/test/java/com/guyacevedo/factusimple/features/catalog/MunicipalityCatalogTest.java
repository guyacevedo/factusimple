package com.guyacevedo.factusimple.features.catalog;

import com.guyacevedo.factusimple.features.catalog.dto.MunicipalityDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MunicipalityCatalogTest {

    private static MunicipalityCatalog catalog;

    @BeforeAll
    static void load() {
        catalog = new MunicipalityCatalog(JsonMapper.builder().build());
    }

    @Test
    void loadsEveryMunicipalityAndDepartment() {
        assertEquals(1122, catalog.municipalities(null).size());
        assertEquals(33, catalog.departments().size());
    }

    @Test
    void knowsDaneCodes() {
        assertTrue(catalog.contains("11001"));   // Bogotá
        assertTrue(catalog.contains("13430"));   // Magangué
        assertFalse(catalog.contains("980"));    // a Factus id, not a DANE code
        assertFalse(catalog.contains("99999"));
    }

    @Test
    void filtersByDepartment() {
        List<MunicipalityDto> bolivar = catalog.municipalities("13");

        assertFalse(bolivar.isEmpty());
        assertTrue(bolivar.stream().allMatch(m -> m.departmentCode().equals("13")));
        assertTrue(bolivar.stream().anyMatch(m -> m.code().equals("13430")));
    }
}
