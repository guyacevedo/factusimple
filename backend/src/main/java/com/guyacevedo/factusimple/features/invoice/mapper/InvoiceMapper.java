package com.guyacevedo.factusimple.features.invoice.mapper;

import com.guyacevedo.factusimple.features.invoice.dto.InvoiceItemResponseDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceItemTaxDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceItemWithholdingTaxDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoicePaymentResponseDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItem;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItemTax;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItemWithholdingTax;
import com.guyacevedo.factusimple.features.invoice.entity.InvoicePayment;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity -> response only: invoices are built by InvoiceService from the
 * calculation, never from a DTO. amountPaid comes out as 0 and the service
 * fills it in with the customer payments.
 */
@Mapper(componentModel = "spring")
public interface InvoiceMapper extends BaseMapper<Invoice, InvoiceResponseDto> {

    @Override
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(invoice.getCustomer().displayName())")
    @Mapping(target = "customerIdentification", source = "customer.identification")
    @Mapping(target = "amountPaid", constant = "0")
    InvoiceResponseDto toDto(Invoice invoice);

    @Mapping(target = "productId", source = "product.id")
    InvoiceItemResponseDto toItemDto(InvoiceItem item);

    @Mapping(target = "isExcluded", source = "excluded")
    InvoiceItemTaxDto toTaxDto(InvoiceItemTax tax);

    InvoiceItemWithholdingTaxDto toWithholdingTaxDto(InvoiceItemWithholdingTax tax);

    InvoicePaymentResponseDto toPaymentDto(InvoicePayment payment);
}
