package com.guyacevedo.factusimple.features.user.service;

import com.guyacevedo.factusimple.features.auth.dto.EstablishmentRequestDto;
import com.guyacevedo.factusimple.features.auth.service.TokenService;
import com.guyacevedo.factusimple.features.plan.entity.Plan;
import com.guyacevedo.factusimple.features.plan.repository.PlanRepository;
import com.guyacevedo.factusimple.features.user.dto.ChangePasswordRequestDto;
import com.guyacevedo.factusimple.features.user.dto.ChangePlanRequestDto;
import com.guyacevedo.factusimple.features.user.dto.UpdateProfileRequestDto;
import com.guyacevedo.factusimple.features.user.dto.UserResponseDto;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.features.user.mapper.UserMapper;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.util.NitVerificationDigit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(UUID userId) {
        return userMapper.toDto(findUser(userId));
    }

    /**
     * Updates the user and the establishment's fiscal data. The NIT and DV
     * identify the issuer of the invoices, so they are frozen once the first
     * invoice exists: changing them would rewrite who issued those invoices.
     */
    @Transactional
    public UserResponseDto updateProfile(UUID userId, UpdateProfileRequestDto request) {
        User user = findUser(userId);
        EstablishmentRequestDto establishment = request.establishment();
        String dv = NitVerificationDigit.resolve(establishment.nit(), establishment.dv());

        boolean issuerChanged = !Objects.equals(user.getNit(), establishment.nit())
                || !Objects.equals(user.getDv(), dv);
        if (issuerChanged && user.getInvoiceCount() > 0) {
            throw new ConflictException(
                    "El NIT y el DV no se pueden cambiar porque el establecimiento ya emitió facturas");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setCompanyName(establishment.companyName().trim());
        user.setNit(establishment.nit());
        user.setDv(dv);
        user.setLegalOrgCode(establishment.legalOrgCode());
        user.setTributeCode(establishment.tributeCode());
        user.setFiscalResponsibility(establishment.fiscalResponsibility());
        user.setAddress(establishment.address());
        user.setMunicipalityCode(establishment.municipalityCode());
        log.info("Profile updated for user id={}", user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDto request) {
        User user = findUser(userId);

        // 400, not 401: a 401 makes the frontend think the session expired and log out.
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("La nueva contraseña no puede ser igual a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        // No save(): the user is managed, so the change is written on commit.
        // Close the other sessions: refresh tokens issued before the change stop working.
        tokenService.revokeAllSessions(user);
        log.info("Password changed for user id={}", user.getId());
    }

    @Transactional
    public void changePlan(UUID userId, ChangePlanRequestDto request) {
        User user = findUser(userId);

        if (user.getPlan().getId().equals(request.planId())) {
            throw new BadRequestException("Ya tienes este plan");
        }

        Plan newPlan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.planId().toString()));

        if (!Boolean.TRUE.equals(newPlan.getActive())) {
            throw new BadRequestException("Este plan no está disponible");
        }

        if (Boolean.TRUE.equals(newPlan.getRequiredInvitedCode())) {
            if (request.inviteCode() == null || request.inviteCode().isBlank()) {
                throw new BadRequestException("Este plan requiere un código de invitación");
            }
            if (!request.inviteCode().trim().equals(newPlan.getCode())) {
                throw new BadRequestException("El código de invitación es incorrecto");
            }
        }

        // Being exactly at the new limit is allowed; only going over it is not.
        checkUsageFits("productos", user.getProductCount(), newPlan.getMaxProducts());
        checkUsageFits("clientes", user.getCustomerCount(), newPlan.getMaxCustomers());
        checkUsageFits("facturas", user.getInvoiceCount(), newPlan.getMaxInvoices());

        user.setPlan(newPlan);
        log.info("Plan changed for user id={} to plan={}", user.getId(), newPlan.getName());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
    }

    private static void checkUsageFits(String resource, int current, int max) {
        if (current > max) {
            throw new BadRequestException(String.format(
                    "Tienes %d %s y el nuevo plan permite máximo %d", current, resource, max));
        }
    }
}