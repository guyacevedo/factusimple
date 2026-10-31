package com.guyacevedo.factusimple.features.invoice.service;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.customer.repository.CustomerRepository;
import com.guyacevedo.factusimple.features.inventory.service.InventoryService;
import com.guyacevedo.factusimple.features.invoice.dto.CancelInvoiceRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.CancelInvoiceResponseDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceItemRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoicePaymentRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.CreditNote;
import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItem;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItemTax;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceItemWithholdingTax;
import com.guyacevedo.factusimple.features.invoice.entity.InvoicePayment;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus;
import com.guyacevedo.factusimple.features.invoice.mapper.CreditNoteMapper;
import com.guyacevedo.factusimple.features.invoice.mapper.InvoiceMapper;
import com.guyacevedo.factusimple.features.invoice.repository.CreditNoteRepository;
import com.guyacevedo.factusimple.features.invoice.repository.InvoiceRepository;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.InvoiceTotals;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.LineCalculation;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.LineInput;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.TaxInput;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.WithholdingTaxInput;
import com.guyacevedo.factusimple.features.pos.service.HeldSaleService;
import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.features.product.repository.ProductRepository;
import com.guyacevedo.factusimple.features.receivable.dto.PaidByInvoice;
import com.guyacevedo.factusimple.features.receivable.repository.CustomerPaymentRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.UnprocessableEntityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Invoices are created and validated here and stay PENDING until the DIAN
 * integration exists. Cancelling one issues a PENDING credit note for the total.
 *
 * The bulk UPDATEs used for stock, plan counter and credit clear the persistence
 * context (clearAutomatically), so each operation follows the same order:
 * load and calculate first, then save the invoice and build the response, and
 * run those atomic UPDATEs last.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    /** DIAN payment forms and the cash payment method. */
    private static final String CASH_SALE = "1";
    private static final String CREDIT_SALE = "2";
    private static final String CASH_METHOD = "10";

    /** DIAN defaults: sales invoice, standard operation; concept 2 = cancellation of the invoice. */
    private static final String SALES_INVOICE = "01";
    private static final String STANDARD_OPERATION = "10";
    private static final String CANCELLATION_CONCEPT = "2";

    // Bounds for an open date filter, so the query never gets a null date.
    private static final LocalDateTime NO_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime NO_END = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final InvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final InventoryService inventoryService;
    private final HeldSaleService heldSaleService;
    private final InvoiceMapper invoiceMapper;
    private final CreditNoteMapper creditNoteMapper;

    @Transactional
    public InvoiceResponseDto createInvoice(UUID userId, InvoiceRequestDto requestDto) {
        Customer customer = customerRepository.findByIdAndUserId(requestDto.customerId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", requestDto.customerId().toString()));
        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BadRequestException("El cliente '" + customer.displayName() + "' está desactivado");
        }

        // 1. Calculate everything while the products and their taxes are loaded.
        Map<UUID, Product> products = loadProducts(userId, requestDto.items());
        List<LineCalculation> lines = new ArrayList<>();
        for (InvoiceItemRequestDto item : requestDto.items()) {
            Product product = products.get(item.productId());
            if (!product.getSaleType().validQuantity(item.quantity())) {
                throw new BadRequestException(
                        "La cantidad de '" + product.getName() + "' debe ser un número entero: se vende por unidad");
            }
            lines.add(InvoiceCalculator.calculateLine(toLineInput(product, item)));
        }
        InvoiceTotals totals = InvoiceCalculator.totals(lines);

        String paymentForm = checkPayments(requestDto.payments(), totals.total());
        boolean creditSale = CREDIT_SALE.equals(paymentForm);
        BigDecimal changeAmount = checkCashReceived(requestDto, paymentForm);
        if (creditSale) {
            // Early and friendly; reserveCredit below is the check that holds under concurrency.
            checkCreditFits(customer, totals.total());
        }

        if (requestDto.referenceCode() != null
                && invoiceRepository.existsByUserIdAndReferenceCode(userId, requestDto.referenceCode())) {
            throw new ConflictException("Ya tienes una factura con el código '" + requestDto.referenceCode() + "'");
        }

        // 2. Plan limit. From here on the persistence context gets cleared, so only references are used.
        if (userRepository.incrementInvoiceCountIfBelowLimit(userId) == 0) {
            throw new UnprocessableEntityException(
                    "Alcanzaste el máximo de facturas de tu plan", "PLAN_LIMIT_REACHED");
        }
        String referenceCode = requestDto.referenceCode() != null
                ? requestDto.referenceCode()
                : "FV-" + userRepository.findInvoiceCountById(userId);

        // 3. Save the invoice with its frozen lines and build the response.
        Invoice invoice = Invoice.builder()
                .user(userRepository.getReferenceById(userId))
                .customer(customerRepository.getReferenceById(customer.getId()))
                .referenceCode(referenceCode)
                .documentType(SALES_INVOICE)
                .operationType(STANDARD_OPERATION)
                .numberingRangeId(requestDto.numberingRangeId())
                .sendEmail(Boolean.TRUE.equals(requestDto.sendEmail()))
                .observation(requestDto.observation())
                .subtotal(totals.subtotal())
                .totalDiscounts(totals.totalDiscounts())
                .totalTaxes(totals.totalTaxes())
                .totalHoldingTaxes(totals.totalHoldingTaxes())
                .total(totals.total())
                .cashReceived(requestDto.cashReceived())
                .changeAmount(changeAmount)
                .balanceDue(creditSale ? totals.total() : BigDecimal.ZERO)
                .build();
        for (int i = 0; i < requestDto.items().size(); i++) {
            InvoiceItemRequestDto item = requestDto.items().get(i);
            invoice.addItem(toItem(products.get(item.productId()), item, lines.get(i)));
        }
        requestDto.payments().forEach(payment -> invoice.addPayment(toPayment(payment)));
        Invoice saved = invoiceRepository.save(invoice);
        InvoiceResponseDto response = invoiceMapper.toDto(saved);

        // 4. Atomic UPDATEs: any failure rolls back the invoice, the counter and the stock taken so far.
        for (InvoiceItemRequestDto item : requestDto.items()) {
            Product product = products.get(item.productId());
            inventoryService.deductStock(product.getId(), product.getName(), item.quantity());
        }
        if (creditSale && customerRepository.reserveCredit(customer.getId(), totals.total()) == 0) {
            throw creditLimitExceeded(customer, totals.total());
        }
        // A held sale being charged leaves the POS list with the same commit.
        if (requestDto.heldSaleId() != null) {
            heldSaleService.deleteForCheckout(userId, requestDto.heldSaleId());
        }

        log.info("Invoice created: id={}, userId={}, reference={}, total={}",
                saved.getId(), userId, referenceCode, totals.total());
        return response;
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoice(UUID userId, UUID id) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> invoiceNotFound(id));
        return withAmountPaid(invoiceMapper.toDto(invoice), customerPaymentRepository.totalPaidByInvoice(id));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponseDto> listInvoices(UUID userId, InvoiceStatus status, UUID customerId,
                                                 String paymentForm, boolean withBalance,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        Page<InvoiceResponseDto> invoices = invoiceRepository.search(userId, status, customerId,
                        paymentForm == null ? "" : paymentForm, withBalance,
                        from == null ? NO_START : from.atStartOfDay(),
                        to == null ? NO_END : to.plusDays(1).atStartOfDay(),
                        pageable)
                .map(invoiceMapper::toDto);
        if (invoices.isEmpty()) {
            return invoices;
        }

        // One query for the whole page instead of one per invoice.
        Map<UUID, BigDecimal> paid = customerPaymentRepository
                .paidByInvoices(invoices.map(InvoiceResponseDto::id).toList()).stream()
                .collect(Collectors.toMap(PaidByInvoice::invoiceId, PaidByInvoice::amount));
        return invoices.map(dto -> withAmountPaid(dto, paid.getOrDefault(dto.id(), BigDecimal.ZERO)));
    }

    /**
     * Cancels the invoice and issues a PENDING credit note for the total. The
     * stock comes back, the unpaid balance is released from the customer's
     * credit and the plan counter does not go down. Payments already made stay
     * as history: amountToRefund tells the cashier how much money goes back.
     */
    @Transactional
    public CancelInvoiceResponseDto cancelInvoice(UUID userId, UUID id, CancelInvoiceRequestDto requestDto) {
        Invoice invoice = invoiceRepository.findForUpdate(id, userId)
                .orElseThrow(() -> invoiceNotFound(id));
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("La factura '" + invoice.getReferenceCode() + "' ya está cancelada");
        }

        // Read everything the atomic UPDATEs below need before they clear the context.
        UUID customerId = invoice.getCustomer().getId();
        List<InvoiceItem> soldProducts = invoice.getItems().stream()
                .filter(item -> item.getProduct() != null)
                .toList();
        List<UUID> productIds = soldProducts.stream().map(item -> item.getProduct().getId()).toList();
        List<BigDecimal> quantities = soldProducts.stream().map(InvoiceItem::getQuantity).toList();
        BigDecimal unpaidBalance = invoice.getBalanceDue();
        BigDecimal paidByCustomer = customerPaymentRepository.totalPaidByInvoice(id);
        BigDecimal paidAtSale = invoice.getPayments().stream()
                .filter(payment -> CASH_SALE.equals(payment.getPaymentForm()))
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.cancel(requestDto.reason());
        invoice.setBalanceDue(BigDecimal.ZERO);
        CreditNote creditNote = creditNoteRepository.save(CreditNote.builder()
                .user(userRepository.getReferenceById(userId))
                .customer(invoice.getCustomer())
                .invoice(invoice)
                .numberingRangeId(requestDto.numberingRangeId())
                .referenceCode("NC-" + invoice.getReferenceCode())
                .correctionConceptCode(CANCELLATION_CONCEPT)
                .observation(requestDto.reason())
                .build());
        CancelInvoiceResponseDto response = new CancelInvoiceResponseDto(
                withAmountPaid(invoiceMapper.toDto(invoice), paidByCustomer),
                creditNoteMapper.toDto(creditNote),
                paidAtSale.add(paidByCustomer));

        for (int i = 0; i < productIds.size(); i++) {
            inventoryService.restoreStock(productIds.get(i), quantities.get(i));
        }
        if (unpaidBalance.signum() > 0 && customerRepository.releaseCredit(customerId, unpaidBalance) == 0) {
            // creditBalance must always equal the balances of the customer's pending invoices.
            throw new IllegalStateException("El saldo del cliente " + customerId
                    + " es menor que el pendiente de la factura " + id);
        }

        log.info("Invoice cancelled: id={}, creditNote={}", id, creditNote.getId());
        return response;
    }

    /** All payments share one payment form and add up exactly to the total. */
    private static String checkPayments(List<InvoicePaymentRequestDto> payments, BigDecimal total) {
        String paymentForm = payments.getFirst().paymentForm();
        if (payments.stream().anyMatch(payment -> !paymentForm.equals(payment.paymentForm()))) {
            throw new BadRequestException("Todos los pagos de una factura deben tener la misma forma de pago");
        }

        BigDecimal paid = payments.stream()
                .map(InvoicePaymentRequestDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(total) != 0) {
            throw new BadRequestException(String.format(
                    "Los pagos suman %s y el total de la factura es %s", paid.toPlainString(), total.toPlainString()));
        }

        if (CREDIT_SALE.equals(paymentForm)) {
            LocalDate today = LocalDate.now();
            boolean missingDueDate = payments.stream()
                    .anyMatch(payment -> payment.dueDate() == null || payment.dueDate().isBefore(today));
            if (missingDueDate) {
                throw new BadRequestException("Las ventas a crédito requieren una fecha de vencimiento desde hoy");
            }
        }
        return paymentForm;
    }

    /** @return the change to hand back, or null when no cash amount was given */
    private static BigDecimal checkCashReceived(InvoiceRequestDto requestDto, String paymentForm) {
        if (requestDto.cashReceived() == null) {
            return null;
        }
        BigDecimal paidInCash = requestDto.payments().stream()
                .filter(payment -> CASH_METHOD.equals(payment.paymentMethodCode()))
                .map(InvoicePaymentRequestDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!CASH_SALE.equals(paymentForm) || paidInCash.signum() == 0) {
            throw new BadRequestException("El efectivo recibido solo aplica a ventas de contado pagadas en efectivo (medio 10)");
        }
        if (requestDto.cashReceived().compareTo(paidInCash) < 0) {
            throw new BadRequestException(String.format(
                    "El efectivo recibido (%s) no alcanza para lo pagado en efectivo (%s)",
                    requestDto.cashReceived().toPlainString(), paidInCash.toPlainString()));
        }
        return requestDto.cashReceived().subtract(paidInCash);
    }

    private static void checkCreditFits(Customer customer, BigDecimal total) {
        if (customer.getCreditBalance().add(total).compareTo(customer.getCreditLimit()) > 0) {
            throw creditLimitExceeded(customer, total);
        }
    }

    private static UnprocessableEntityException creditLimitExceeded(Customer customer, BigDecimal total) {
        BigDecimal available = customer.getCreditLimit().subtract(customer.getCreditBalance()).max(BigDecimal.ZERO);
        return new UnprocessableEntityException(String.format(
                "La venta (%s) supera el cupo de crédito disponible de '%s' (%s)",
                total.toPlainString(), customer.displayName(), available.toPlainString()), "CREDIT_LIMIT_EXCEEDED");
    }

    /** The products of the invoice, all of them the user's own and active. */
    private Map<UUID, Product> loadProducts(UUID userId, List<InvoiceItemRequestDto> items) {
        Set<UUID> ids = items.stream().map(InvoiceItemRequestDto::productId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Product> products = productRepository.findAllByUserIdAndIdIn(userId, ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (UUID id : ids) {
            Product product = products.get(id);
            if (product == null) {
                throw new ResourceNotFoundException("Producto", "id", id.toString());
            }
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("El producto '" + product.getName() + "' está desactivado");
            }
        }
        return products;
    }

    private static LineInput toLineInput(Product product, InvoiceItemRequestDto item) {
        return new LineInput(
                item.quantity(),
                product.getRetailPrice(),
                product.getWholesalePrice(),
                product.getMinStockWholesale(),
                item.discountRate(),
                product.getTaxes().stream()
                        .map(tax -> new TaxInput(tax.getTaxCode(), tax.getTaxRate(), Boolean.TRUE.equals(tax.getExcluded())))
                        .toList(),
                product.getWithholdingTaxes().stream()
                        .map(tax -> new WithholdingTaxInput(tax.getWithholdingTaxCode(), tax.getWithholdingTaxRate()))
                        .toList());
    }

    /** Copies what was sold into the line, so the invoice no longer depends on the product. */
    private InvoiceItem toItem(Product product, InvoiceItemRequestDto request, LineCalculation line) {
        InvoiceItem item = InvoiceItem.builder()
                .product(productRepository.getReferenceById(product.getId()))
                .codeReference(product.getSku())
                .name(product.getName())
                .unitMeasure(product.getUnitMeasure())
                .standardCode(product.getStandardCode())
                .quantity(request.quantity())
                .unitPrice(line.unitPrice())
                .discountRate(line.discountRate())
                .discountAmount(line.discountAmount())
                .baseAmount(line.baseAmount())
                .taxAmount(line.taxAmount())
                .withholdingTaxAmount(line.withholdingTaxAmount())
                .total(line.total())
                .note(request.note())
                .build();
        line.taxes().forEach(tax -> item.addTax(InvoiceItemTax.builder()
                .taxCode(tax.code())
                .taxRate(tax.rate())
                .excluded(tax.excluded())
                .baseAmount(tax.baseAmount())
                .amount(tax.amount())
                .build()));
        line.withholdingTaxes().forEach(tax -> item.addWithholdingTax(InvoiceItemWithholdingTax.builder()
                .withholdingTaxCode(tax.code())
                .withholdingTaxRate(tax.rate())
                .baseAmount(tax.baseAmount())
                .amount(tax.amount())
                .build()));
        return item;
    }

    private static InvoicePayment toPayment(InvoicePaymentRequestDto payment) {
        return InvoicePayment.builder()
                .paymentForm(payment.paymentForm())
                .paymentMethodCode(payment.paymentMethodCode())
                .referenceCode(payment.referenceCode())
                .amount(payment.amount())
                .dueDate(payment.dueDate())
                .build();
    }

    /** amountPaid is not stored in the invoice: it is the sum of its customer payments. */
    private static InvoiceResponseDto withAmountPaid(InvoiceResponseDto d, BigDecimal paid) {
        return new InvoiceResponseDto(d.id(), d.referenceCode(), d.status(), d.customerId(), d.customerName(),
                d.customerIdentification(), d.numberingRangeId(), d.documentType(), d.operationType(), d.sendEmail(),
                d.observation(), d.subtotal(), d.totalDiscounts(), d.totalTaxes(), d.totalHoldingTaxes(),
                d.cashRounding(), d.total(), d.cashReceived(), d.changeAmount(), d.balanceDue(), paid,
                d.cancelledAt(), d.cancellationReason(), d.cufe(), d.factusNumber(), d.items(), d.payments(),
                d.createdAt(), d.updatedAt());
    }

    // Another user's invoice answers 404, not 403, so its existence is not revealed.
    private static ResourceNotFoundException invoiceNotFound(UUID id) {
        return new ResourceNotFoundException("Factura", "id", id.toString());
    }
}
