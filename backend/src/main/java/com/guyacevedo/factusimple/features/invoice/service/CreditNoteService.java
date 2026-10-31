package com.guyacevedo.factusimple.features.invoice.service;

import com.guyacevedo.factusimple.features.invoice.dto.CreditNoteResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.CreditNoteStatus;
import com.guyacevedo.factusimple.features.invoice.mapper.CreditNoteMapper;
import com.guyacevedo.factusimple.features.invoice.repository.CreditNoteRepository;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Read side of credit notes; they are issued by InvoiceService.cancelInvoice. */
@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteMapper creditNoteMapper;

    @Transactional(readOnly = true)
    public CreditNoteResponseDto getCreditNote(UUID userId, UUID id) {
        return creditNoteMapper.toDto(creditNoteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota crédito", "id", id.toString())));
    }

    @Transactional(readOnly = true)
    public Page<CreditNoteResponseDto> listCreditNotes(UUID userId, CreditNoteStatus status, UUID invoiceId,
                                                       Pageable pageable) {
        return creditNoteRepository.search(userId, status, invoiceId, pageable).map(creditNoteMapper::toDto);
    }
}
