package com.guyacevedo.factusimple.features.invoice.entity;

/**
 * Estado interno de la factura.
 * - PENDING: creada localmente, aún no enviada a Factus.
 * - VALIDATED: aceptada por Factus (cufe + factusNumber asignados).
 * - ERROR: el envío a Factus falló y debe reintentarse.
 * - CANCELLED: anulada localmente / nota crédito emitida (cancelledAt asignado).
 */
public enum InvoiceStatus {
    PENDING,
    VALIDATED,
    ERROR,
    CANCELLED
}