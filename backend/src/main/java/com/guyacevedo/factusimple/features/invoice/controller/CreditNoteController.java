package com.guyacevedo.factusimple.features.invoice.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.invoice.dto.CreditNoteResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.CreditNoteStatus;
import com.guyacevedo.factusimple.features.invoice.service.CreditNoteService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import com.guyacevedo.factusimple.shared.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/credit-notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Credit Notes", description = """
        Notas crédito del establecimiento autenticado.

        Se emiten al cancelar una factura (`POST /v1/invoices/{id}/cancel`) y quedan en **PENDING**
        hasta que exista la sincronización con la DIAN. Aquí solo se consultan.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    @Operation(
            summary = "Listar notas crédito",
            description = "De la más reciente a la más antigua, con filtros opcionales por estado y factura.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de notas crédito")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<CreditNoteResponseDto>>> listCreditNotes(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Estado de la nota crédito") @RequestParam(required = false) CreditNoteStatus status,
            @Parameter(description = "UUID de la factura") @RequestParam(required = false) UUID invoiceId,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Notas crédito obtenidas",
                PageResponseDto.from(creditNoteService.listCreditNotes(principal.getUserId(), status, invoiceId, pageable))));
    }

    @Operation(
            summary = "Obtener nota crédito por ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota crédito encontrada"),
            @ApiResponse(responseCode = "404", description = "Nota crédito no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CreditNoteResponseDto>> getCreditNote(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la nota crédito", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Nota crédito obtenida exitosamente",
                creditNoteService.getCreditNote(principal.getUserId(), id)));
    }
}
