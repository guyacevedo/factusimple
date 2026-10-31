package com.guyacevedo.factusimple.features.invoice.mapper;

import com.guyacevedo.factusimple.features.invoice.dto.CreditNoteResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.CreditNote;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity -> response only: credit notes are issued by InvoiceService when an invoice is cancelled. */
@Mapper(componentModel = "spring")
public interface CreditNoteMapper extends BaseMapper<CreditNote, CreditNoteResponseDto> {

    @Override
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceReferenceCode", source = "invoice.referenceCode")
    @Mapping(target = "customerId", source = "customer.id")
    CreditNoteResponseDto toDto(CreditNote creditNote);
}
