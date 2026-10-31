package com.guyacevedo.factusimple.features.invoice.entity;

/**
 * Estado de la nota crédito ante la DIAN, igual que InvoiceStatus.
 * - PENDING: emitida localmente al cancelar la factura; pendiente de envío a la DIAN.
 * - VALIDATED: aceptada por la DIAN vía Factus.
 * - ERROR: el envío a la DIAN falló y debe reintentarse.
 */
public enum CreditNoteStatus {
    PENDING,
    VALIDATED,
    ERROR
}
