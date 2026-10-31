package com.guyacevedo.factusimple.features.receivable.mapper;

import com.guyacevedo.factusimple.features.receivable.dto.CustomerPaymentResponseDto;
import com.guyacevedo.factusimple.features.receivable.entity.CustomerPayment;
import com.guyacevedo.factusimple.shared.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity -> response only: ReceivableService builds the payments when it spreads them over the invoices. */
@Mapper(componentModel = "spring")
public interface CustomerPaymentMapper extends BaseMapper<CustomerPayment, CustomerPaymentResponseDto> {

    @Override
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(payment.getCustomer().displayName())")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceReferenceCode", source = "invoice.referenceCode")
    CustomerPaymentResponseDto toDto(CustomerPayment payment);
}
