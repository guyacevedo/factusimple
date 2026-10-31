package com.guyacevedo.factusimple.features.catalog;

import com.guyacevedo.factusimple.features.catalog.dto.DepartmentDto;
import com.guyacevedo.factusimple.features.catalog.dto.MunicipalityDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Colombian municipalities with their DANE codes, read once at startup from
 * classpath:catalogs/municipalities.json. It is static data, so it lives in
 * memory; a missing or broken file stops the application at startup instead of
 * failing on the first registration.
 */
@Component
public class MunicipalityCatalog {

    static final String RESOURCE = "catalogs/municipalities.json";

    private static final Comparator<MunicipalityDto> BY_DEPARTMENT_AND_NAME =
            Comparator.comparing(MunicipalityDto::departmentName).thenComparing(MunicipalityDto::name);

    private final List<MunicipalityDto> municipalities;
    private final Map<String, MunicipalityDto> byCode;
    private final List<DepartmentDto> departments;

    public MunicipalityCatalog(ObjectMapper objectMapper) {
        this.municipalities = load(objectMapper).stream().sorted(BY_DEPARTMENT_AND_NAME).toList();
        this.byCode = municipalities.stream()
                .collect(Collectors.toMap(MunicipalityDto::code, Function.identity()));
        this.departments = municipalities.stream()
                .collect(Collectors.toMap(MunicipalityDto::departmentCode,
                        m -> new DepartmentDto(m.departmentCode(), m.departmentName()),
                        (first, second) -> first, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(DepartmentDto::name))
                .toList();
    }

    public boolean contains(String code) {
        return byCode.containsKey(code);
    }

    /** All of them, or those of one department when its code is given. */
    public List<MunicipalityDto> municipalities(String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) {
            return municipalities;
        }
        return municipalities.stream().filter(m -> m.departmentCode().equals(departmentCode)).toList();
    }

    public List<DepartmentDto> departments() {
        return departments;
    }

    private static List<MunicipalityDto> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            List<MunicipalityDto> list = root.path("municipalities").valueStream()
                    .map(node -> new MunicipalityDto(
                            node.path("code").asString(),
                            node.path("name").asString(),
                            node.path("department").path("code").asString(),
                            node.path("department").path("name").asString()))
                    .toList();
            if (list.isEmpty()) {
                throw new IllegalStateException(RESOURCE + " no tiene municipios");
            }
            return list;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RESOURCE, e);
        }
    }
}
