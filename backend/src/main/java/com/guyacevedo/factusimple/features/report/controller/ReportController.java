package com.guyacevedo.factusimple.features.report.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.report.dto.DailySalesDto;
import com.guyacevedo.factusimple.features.report.dto.SalesSummaryDto;
import com.guyacevedo.factusimple.features.report.dto.SlowMovingProductDto;
import com.guyacevedo.factusimple.features.report.dto.TopProductDto;
import com.guyacevedo.factusimple.features.report.service.ReportService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Reports", description = """
        Cifras para el dashboard del establecimiento autenticado.

        - `from` / `to` son días inclusivos; si se omiten, el periodo es el mes en curso hasta hoy (máximo 366 días).
        - Cada evento cuenta en su fecha: una venta el día de la factura y una cancelación el día que se
          canceló (su nota crédito). Neto = facturado − cancelado, así un periodo cerrado nunca cambia.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "Resumen de ventas",
            description = "Facturado, cancelado y neto del periodo (contado vs crédito, ticket promedio) y, a hoy, cartera pendiente, cartera vencida y productos con stock bajo.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen del periodo"),
            @ApiResponse(responseCode = "400", description = "Periodo inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponseDto<SalesSummaryDto>> summary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Desde (inclusive)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (inclusive)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponseDto.success("Resumen obtenido",
                reportService.summary(principal.getUserId(), from, to)));
    }

    @Operation(
            summary = "Ventas por día",
            description = "Un punto por cada día del periodo con lo facturado, lo cancelado ese día y el neto; cero en los días sin movimiento.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventas diarias"),
            @ApiResponse(responseCode = "400", description = "Periodo inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/sales-by-day")
    public ResponseEntity<ApiResponseDto<List<DailySalesDto>>> salesByDay(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Desde (inclusive)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (inclusive)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponseDto.success("Ventas por día obtenidas",
                reportService.salesByDay(principal.getUserId(), from, to)));
    }

    @Operation(
            summary = "Productos más vendidos",
            description = "Ordenados por monto neto de cancelaciones del periodo (las cantidades de unidades y kilos no se pueden sumar entre sí).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos más vendidos"),
            @ApiResponse(responseCode = "400", description = "Periodo inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponseDto<List<TopProductDto>>> topProducts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Desde (inclusive)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (inclusive)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Cuántos productos (1-50)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponseDto.success("Productos más vendidos obtenidos",
                reportService.topProducts(principal.getUserId(), from, to, limit)));
    }

    @Operation(
            summary = "Rotación lenta",
            description = """
                    Productos activos con stock que no se venden hace más de `days` días (1-365, por defecto 30),
                    con el capital detenido (cantidad × costo actual).

                    Primero los que nunca se vendieron; luego los que más dinero tienen parado.
                    Una venta cancelada no cuenta como venta.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Productos de rotación lenta")
    @GetMapping("/slow-moving")
    public ResponseEntity<ApiResponseDto<List<SlowMovingProductDto>>> slowMoving(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Días sin ventas (1-365)", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponseDto.success("Productos de rotación lenta obtenidos",
                reportService.slowMoving(principal.getUserId(), days)));
    }
}
