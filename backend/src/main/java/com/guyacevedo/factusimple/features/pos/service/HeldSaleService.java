package com.guyacevedo.factusimple.features.pos.service;

import com.guyacevedo.factusimple.features.pos.dto.HeldSaleRequestDto;
import com.guyacevedo.factusimple.features.pos.dto.HeldSaleResponseDto;
import com.guyacevedo.factusimple.features.pos.entity.HeldSale;
import com.guyacevedo.factusimple.features.pos.repository.HeldSaleRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/**
 * Sales put on hold at the POS to serve another customer.
 *
 * A held sale does not touch inventory or prices (see V6): the cart is stored
 * as the frontend sends it, and stock is only deducted when the sale is charged
 * with POST /v1/invoices, which removes it from the list through heldSaleId.
 */
@Service
@Slf4j
public class HeldSaleService {

    /** Serialized cart size; a POS cart is a few KB, this only stops abuse of the jsonb column. */
    private static final int MAX_CONTENT_LENGTH = 100_000;

    private final HeldSaleRepository heldSaleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final int maxHeldSales;

    public HeldSaleService(HeldSaleRepository heldSaleRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper,
                           @Value("${app.pos.max-held-sales:10}") int maxHeldSales) {
        this.heldSaleRepository = heldSaleRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.maxHeldSales = maxHeldSales;
    }

    @Transactional
    public HeldSaleResponseDto hold(UUID userId, HeldSaleRequestDto requestDto) {
        String label = requestDto.label().trim();
        if (heldSaleRepository.existsByUserIdAndLabelIgnoreCase(userId, label)) {
            throw new ConflictException("Ya tienes una venta en espera con la etiqueta '" + label + "'");
        }
        if (heldSaleRepository.countByUserId(userId) >= maxHeldSales) {
            throw new UnprocessableEntityException(String.format(
                    "Ya tienes %d ventas en espera, el máximo permitido. Cobra o descarta alguna", maxHeldSales),
                    "HELD_SALES_LIMIT");
        }

        HeldSale heldSale = heldSaleRepository.save(HeldSale.builder()
                .user(userRepository.getReferenceById(userId))
                .label(label)
                .content(serialize(requestDto.content()))
                .build());
        log.info("Sale held: id={}, userId={}", heldSale.getId(), userId);
        return toDto(heldSale);
    }

    @Transactional(readOnly = true)
    public List<HeldSaleResponseDto> list(UUID userId) {
        return heldSaleRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public HeldSaleResponseDto get(UUID userId, UUID id) {
        return toDto(findHeldSale(userId, id));
    }

    /** Replaces the label and the cart, e.g. after adding products to a resumed sale. */
    @Transactional
    public HeldSaleResponseDto update(UUID userId, UUID id, HeldSaleRequestDto requestDto) {
        HeldSale heldSale = findHeldSale(userId, id);
        String label = requestDto.label().trim();
        if (heldSaleRepository.existsByUserIdAndLabelIgnoreCaseAndIdNot(userId, label, id)) {
            throw new ConflictException("Ya tienes otra venta en espera con la etiqueta '" + label + "'");
        }

        heldSale.setLabel(label);
        heldSale.setContent(serialize(requestDto.content()));
        return toDto(heldSale);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        heldSaleRepository.delete(findHeldSale(userId, id));
        log.info("Held sale discarded: id={}", id);
    }

    /** Called by InvoiceService inside its transaction: if the invoice fails, the sale stays on hold. */
    @Transactional
    public void deleteForCheckout(UUID userId, UUID id) {
        heldSaleRepository.delete(findHeldSale(userId, id));
    }

    private String serialize(JsonNode content) {
        if (content.isNull() || content.isMissingNode() || !(content.isObject() || content.isArray())) {
            throw new BadRequestException("El contenido del carrito debe ser un objeto o un arreglo JSON");
        }
        String json = objectMapper.writeValueAsString(content);
        if (json.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("El carrito es demasiado grande para dejarlo en espera");
        }
        return json;
    }

    private HeldSaleResponseDto toDto(HeldSale heldSale) {
        return new HeldSaleResponseDto(heldSale.getId(), heldSale.getLabel(),
                objectMapper.readTree(heldSale.getContent()), heldSale.getCreatedAt(), heldSale.getUpdatedAt());
    }

    // Another user's held sale answers 404, not 403, so its existence is not revealed.
    private HeldSale findHeldSale(UUID userId, UUID id) {
        return heldSaleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta en espera", "id", id.toString()));
    }
}
