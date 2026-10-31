package com.guyacevedo.factusimple.features.customer.service;

import com.guyacevedo.factusimple.features.customer.dto.CustomerRequestDto;
import com.guyacevedo.factusimple.features.customer.dto.CustomerResponseDto;
import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.customer.mapper.CustomerMapper;
import com.guyacevedo.factusimple.features.customer.repository.CustomerRepository;
import com.guyacevedo.factusimple.features.invoice.repository.InvoiceRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.UnprocessableEntityException;
import com.guyacevedo.factusimple.shared.util.NitVerificationDigit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    /** DIAN identification type of a NIT. */
    private static final String NIT_DOCUMENT = "31";

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponseDto createCustomer(UUID userId, CustomerRequestDto requestDto) {
        if (customerRepository.existsByUserIdAndIdentification(userId, requestDto.identification())) {
            throw new ConflictException(
                    "Ya tienes un cliente con la identificación '" + requestDto.identification() + "'");
        }

        // Atomic check against the plan; rolled back with the transaction if the save fails.
        if (userRepository.incrementCustomerCountIfBelowLimit(userId) == 0) {
            throw new UnprocessableEntityException(
                    "Alcanzaste el máximo de clientes de tu plan", "PLAN_LIMIT_REACHED");
        }

        Customer customer = customerMapper.toEntity(requestDto);
        customer.setDv(verificationDigitOf(requestDto));
        // Reference only: no SELECT on users, the FK is enough.
        customer.setUser(userRepository.getReferenceById(userId));
        customer = customerRepository.save(customer);
        log.info("Customer created: id={}, userId={}", customer.getId(), userId);
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomer(UUID userId, UUID id) {
        return customerMapper.toDto(findCustomer(userId, id));
    }

    /** For invoicing and the POS: only customers that can be billed. */
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> listActiveCustomers(UUID userId, Pageable pageable) {
        return customerRepository.findAllByUserIdAndActiveTrue(userId, pageable).map(customerMapper::toDto);
    }

    /** Management list: includes inactive customers so they can be reactivated. */
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> listAllCustomers(UUID userId, Pageable pageable) {
        return customerRepository.findAllByUserId(userId, pageable).map(customerMapper::toDto);
    }

    @Transactional
    public CustomerResponseDto updateCustomer(UUID userId, UUID id, CustomerRequestDto requestDto) {
        Customer customer = findCustomer(userId, id);

        if (customerRepository.existsByUserIdAndIdentificationAndIdNot(userId, requestDto.identification(), id)) {
            throw new ConflictException(
                    "Ya tienes otro cliente con la identificación '" + requestDto.identification() + "'");
        }
        // The customer already owes creditBalance: the limit cannot go below it.
        if (requestDto.creditLimit() != null
                && requestDto.creditLimit().compareTo(customer.getCreditBalance()) < 0) {
            throw new BadRequestException(
                    "El cupo de crédito no puede ser menor al saldo pendiente (" + customer.getCreditBalance() + ")");
        }

        String dv = verificationDigitOf(requestDto);
        customerMapper.updateEntity(requestDto, customer);
        customer.setDv(dv);
        log.info("Customer updated: id={}", id);
        return customerMapper.toDto(customer);
    }

    /**
     * A customer with invoices cannot be deleted (invoices.customer_id is
     * ON DELETE RESTRICT, and every credit note points to one of those invoices).
     * Deactivate it instead with isActive=false.
     */
    @Transactional
    public void deleteCustomer(UUID userId, UUID id) {
        Customer customer = findCustomer(userId, id);

        if (invoiceRepository.existsByCustomerId(id)) {
            throw new ConflictException(
                    "El cliente tiene facturas y no puede eliminarse. Desactívalo con isActive=false");
        }

        customerRepository.delete(customer);
        // Frees the slot in the plan; flushAutomatically writes the DELETE first.
        userRepository.decrementCustomerCount(userId);
        log.info("Customer deleted: id={}", id);
    }

    /**
     * A NIT customer always ends up with its DV: calculated when missing (as
     * Factus does) and rejected when it does not match. Other documents have
     * no DV, so whatever was sent is kept as is.
     */
    private static String verificationDigitOf(CustomerRequestDto requestDto) {
        if (!NIT_DOCUMENT.equals(requestDto.idTypeCode())) {
            return requestDto.dv();
        }
        if (!requestDto.identification().matches("\\d{5,15}")) {
            throw new BadRequestException(
                    "La identificación de un NIT debe tener entre 5 y 15 dígitos, sin dígito de verificación ni guion");
        }
        return NitVerificationDigit.resolve(requestDto.identification(), requestDto.dv());
    }

    // Another user's customer answers 404, not 403, so its existence is not revealed.
    private Customer findCustomer(UUID userId, UUID id) {
        return customerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id.toString()));
    }
}
