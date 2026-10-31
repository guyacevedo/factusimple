package com.guyacevedo.factusimple.features.invoice.entity;

/**
 * Estado de la factura ante la DIAN.
 * - PENDING: creada y validada localmente (totales, stock, cupo); pendiente de
 *   envío a la DIAN. Es el estado de toda factura hasta que exista la integración.
 * - VALIDATED: aceptada por la DIAN vía Factus (cufe + factusNumber asignados).
 * - ERROR: el envío a la DIAN falló y debe reintentarse.
 * - CANCELLED: cancelada; se emitió una nota crédito (cancelledAt asignado).
 */
public enum InvoiceStatus {
    PENDING,
    VALIDATED,
    ERROR,
    CANCELLED
}
