package com.guyacevedo.factusimple.features.receivable.service;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.customer.repository.CustomerRepository;
import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.invoice.entity.InvoicePayment;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus;
import com.guyacevedo.factusimple.features.invoice.repository.InvoiceRepository;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerDebtResponseDto;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerPaymentRequestDto;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerPaymentResponseDto;
import com.guyacevedo.factusimple.features.receivable.dto.PendingInvoiceDto;
import com.guyacevedo.factusimple.features.receivable.entity.CustomerPayment;
import com.guyacevedo.factusimple.features.receivable.mapper.CustomerPaymentMapper;
import com.guyacevedo.factusimple.features.receivable.repository.CustomerPaymentRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Receivables (cartera): payments towards credit invoices.
 *
 * Invariant: a customer's creditBalance equals the balanceDue of its pending
 * invoices. A credit sale raises both, a payment or a cancellation lowers both.
 * The invoice rows are locked while a payment is spread over them, the same
 * lock cancelInvoice takes, so a payment and a cancellation cannot overlap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceivableService {

    private final CustomerPaymentRepository customerPaymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerPaymentMapper customerPaymentMapper;

    /**
     * Pays towards one invoice, or towards the customer's whole debt when no
     * invoice is given: then it settles the oldest invoices first, which is the
     * order debt is collected in, leaving one row per invoice it touched.
     */
    @Transactional
    public List<CustomerPaymentResponseDto> registerPayment(UUID userId, CustomerPaymentRequestDto requestDto) {
        Customer customer = findCustomer(userId, requestDto.customerId());
        List<Invoice> invoices = requestDto.invoiceId() != null
                ? List.of(lockInvoiceToPay(userId, customer, requestDto))
                : lockDebtToPay(userId, customer, requestDto.amount());

        BigDecimal remaining = requestDto.amount();
        List<CustomerPayment> payments = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal applied = remaining.min(invoice.getBalanceDue());
            invoice.setBalanceDue(invoice.getBalanceDue().subtract(applied));
            payments.add(CustomerPayment.builder()
                    .user(userRepository.getReferenceById(userId))
                    .customer(customer)
                    .invoice(invoice)
                    .amount(applied)
                    .paymentMethodCode(requestDto.paymentMethodCode())
                    .referenceCode(requestDto.referenceCode())
                    .notes(requestDto.notes())
                    .build());
            remaining = remaining.subtract(applied);
        }
        List<CustomerPaymentResponseDto> response = customerPaymentRepository.saveAll(payments).stream()
                .map(customerPaymentMapper::toDto)
                .toList();

        // Last: it clears the persistence context (the changes above are flushed first).
        if (customerRepository.releaseCredit(customer.getId(), requestDto.amount()) == 0) {
            throw new IllegalStateException("El saldo del cliente " + customer.getId()
                    + " es menor que el pendiente de sus facturas");
        }
        log.info("Customer payment registered: customerId={}, amount={}, invoices={}",
                customer.getId(), requestDto.amount(), payments.size());
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CustomerPaymentResponseDto> listPayments(UUID userId, UUID customerId, UUID invoiceId,
                                                         Pageable pageable) {
        return customerPaymentRepository.search(userId, customerId, invoiceId, pageable)
                .map(customerPaymentMapper::toDto);
    }

    /** What the customer owes and on which invoices, oldest first. */
    @Transactional(readOnly = true)
    public CustomerDebtResponseDto getCustomerDebt(UUID userId, UUID customerId) {
        Customer customer = findCustomer(userId, customerId);
        List<PendingInvoiceDto> pending = invoiceRepository.findWithBalance(customerId, userId).stream()
                .map(invoice -> new PendingInvoiceDto(invoice.getId(), invoice.getReferenceCode(),
                        invoice.getTotal(), invoice.getBalanceDue(), dueDateOf(invoice), invoice.getCreatedAt()))
                .toList();
        return new CustomerDebtResponseDto(customer.getId(), customer.displayName(),
                customer.getCreditLimit(), customer.getCreditBalance(),
                customer.getCreditLimit().subtract(customer.getCreditBalance()).max(BigDecimal.ZERO),
                pending);
    }

    private Invoice lockInvoiceToPay(UUID userId, Customer customer, CustomerPaymentRequestDto requestDto) {
        Invoice invoice = invoiceRepository.findForUpdate(requestDto.invoiceId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "id", requestDto.invoiceId().toString()));
        if (!invoice.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("La factura '" + invoice.getReferenceCode() + "' no es de este cliente");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("La factura '" + invoice.getReferenceCode() + "' está cancelada");
        }
        if (invoice.getBalanceDue().signum() == 0) {
            throw new ConflictException("La factura '" + invoice.getReferenceCode() + "' no tiene saldo pendiente");
        }
        if (requestDto.amount().compareTo(invoice.getBalanceDue()) > 0) {
            throw new BadRequestException(String.format("El abono (%s) supera el saldo de la factura (%s)",
                    requestDto.amount().toPlainString(), invoice.getBalanceDue().toPlainString()));
        }
        return invoice;
    }

    private List<Invoice> lockDebtToPay(UUID userId, Customer customer, BigDecimal amount) {
        List<Invoice> invoices = invoiceRepository.findWithBalanceForUpdate(customer.getId(), userId);
        BigDecimal debt = invoices.stream().map(Invoice::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debt.signum() == 0) {
            throw new ConflictException("'" + customer.displayName() + "' no tiene deuda pendiente");
        }
        if (amount.compareTo(debt) > 0) {
            throw new BadRequestException(String.format("El abono (%s) supera la deuda del cliente (%s)",
                    amount.toPlainString(), debt.toPlainString()));
        }
        return invoices;
    }

    /** Credit invoices carry the due date in their payments. */
    private static LocalDate dueDateOf(Invoice invoice) {
        return invoice.getPayments().stream()
                .map(InvoicePayment::getDueDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    // Another user's customer answers 404, not 403, so its existence is not revealed.
    private Customer findCustomer(UUID userId, UUID customerId) {
        return customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", customerId.toString()));
    }
}
