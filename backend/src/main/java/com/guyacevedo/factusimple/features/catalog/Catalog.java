package com.guyacevedo.factusimple.features.catalog;

import com.guyacevedo.factusimple.features.catalog.dto.CatalogEntry;

import java.util.List;

/**
 * DIAN code lists the API accepts, served by GET /v1/catalogs and enforced with
 * {@link com.guyacevedo.factusimple.features.catalog.validation.ValidCatalogCode}.
 *
 * These are DIAN codes, not Factus IDs: where Factus uses its own identifiers
 * (e.g. identification documents, municipalities) the Factus integration maps
 * them. Check every list against the Factus documentation before integrating.
 * Municipalities are not here: there are more than 1100 and they come from Factus.
 */
public enum Catalog {

    IDENTIFICATION_TYPE("tipos de identificación", List.of(
            entry("11", "Registro civil"),
            entry("12", "Tarjeta de identidad"),
            entry("13", "Cédula de ciudadanía"),
            entry("21", "Tarjeta de extranjería"),
            entry("22", "Cédula de extranjería"),
            entry("31", "NIT"),
            entry("41", "Pasaporte"),
            entry("42", "Documento de identificación extranjero"),
            entry("47", "PEP"),
            entry("48", "PPT"),
            entry("50", "NIT de otro país"),
            entry("91", "NUIP"))),

    LEGAL_ORGANIZATION("tipos de organización", List.of(
            entry("1", "Persona jurídica"),
            entry("2", "Persona natural"))),

    TRIBUTE("tributos del cliente o del emisor", List.of(
            entry("01", "IVA"),
            entry("ZZ", "No aplica"))),

    /** Written with the letter O, as the DIAN publishes them (O-13, not 0-13). */
    FISCAL_RESPONSIBILITY("responsabilidades fiscales", List.of(
            entry("O-13", "Gran contribuyente"),
            entry("O-15", "Autorretenedor"),
            entry("O-23", "Agente de retención IVA"),
            entry("O-47", "Régimen simple de tributación"),
            entry("R-99-PN", "No responsable"))),

    TAX("impuestos del producto", List.of(
            entry("01", "IVA"),
            entry("02", "Impuesto al consumo departamental nominal"),
            entry("03", "Industria, comercio y aviso"),
            entry("04", "Impuesto nacional al consumo"),
            entry("08", "Impuesto al consumo departamental porcentual"),
            entry("21", "Timbre"),
            entry("22", "Impuesto nacional a las bolsas plásticas"),
            entry("23", "Impuesto nacional al carbono"),
            entry("24", "Impuesto nacional a los combustibles"),
            entry("30", "Impuesto al consumo de datos"),
            entry("35", "Impuesto a los productos ultraprocesados"))),

    WITHHOLDING_TAX("retenciones", List.of(
            entry("05", "ReteIVA"),
            entry("06", "ReteRenta"))),

    PAYMENT_FORM("formas de pago", List.of(
            entry("1", "Contado"),
            entry("2", "Crédito"))),

    PAYMENT_METHOD("medios de pago", List.of(
            entry("1", "No definido"),
            entry("10", "Efectivo"),
            entry("20", "Cheque"),
            entry("42", "Consignación"),
            entry("47", "Transferencia"),
            entry("48", "Tarjeta crédito"),
            entry("49", "Tarjeta débito"),
            entry("71", "Bonos"),
            entry("72", "Vales"),
            entry("ZZZ", "Otro"))),

    PRODUCT_STANDARD("estándares de identificación del producto", List.of(
            entry("001", "UNSPSC"),
            entry("010", "GTIN"),
            entry("020", "Partida arancelaria"),
            entry("999", "Adopción del contribuyente"))),

    INVOICE_DOCUMENT_TYPE("tipos de documento", List.of(
            entry("01", "Factura electrónica de venta"))),

    /** Mandates (11) and transport (12) exist in Factus but are not supported yet. */
    INVOICE_OPERATION_TYPE("tipos de operación soportados", List.of(
            entry("10", "Estándar"))),

    CREDIT_NOTE_OPERATION_TYPE("tipos de operación de nota crédito", List.of(
            entry("20", "Nota crédito que referencia una factura"))),

    /** Only cancellation is issued today (InvoiceService.cancelInvoice). */
    CREDIT_NOTE_CONCEPT("conceptos de nota crédito", List.of(
            entry("2", "Anulación de factura electrónica"))),

    /** For invoice-level allowances and charges, not used yet. */
    ALLOWANCE_CHARGE_CONCEPT("conceptos de descuento o recargo", List.of(
            entry("03", "Recargo condicionado")));

    private final String label;
    private final List<CatalogEntry> entries;

    Catalog(String label, List<CatalogEntry> entries) {
        this.label = label;
        this.entries = entries;
    }

    /** Human name of the list, for validation messages. */
    public String label() {
        return label;
    }

    public List<CatalogEntry> entries() {
        return entries;
    }

    public boolean contains(String code) {
        return entries.stream().anyMatch(entry -> entry.code().equals(code));
    }

    private static CatalogEntry entry(String code, String name) {
        return new CatalogEntry(code, name);
    }
}
