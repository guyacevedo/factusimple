package com.guyacevedo.factusimple.features.plan.service;

import com.guyacevedo.factusimple.features.plan.dto.PlanRequestDto;
import com.guyacevedo.factusimple.features.plan.dto.PlanResponseDto;
import com.guyacevedo.factusimple.features.plan.entity.Plan;
import com.guyacevedo.factusimple.features.plan.mapper.PlanMapper;
import com.guyacevedo.factusimple.features.plan.repository.PlanRepository;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
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
public class PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PlanMapper planMapper;

    @Transactional
    public PlanResponseDto createPlan(PlanRequestDto requestDto) {
        if (planRepository.existsByName(requestDto.name())) {
            throw new ConflictException("Ya existe un plan con el nombre '" + requestDto.name() + "'");
        }

        Plan plan = planRepository.save(planMapper.toEntity(requestDto));
        log.info("Plan created: id={}, name={}", plan.getId(), plan.getName());
        return planMapper.toDto(plan);
    }

    @Transactional(readOnly = true)
    public PlanResponseDto getPlan(UUID id) {
        return planMapper.toDto(findPlan(id));
    }

    /** Public list (landing page): only plans open for subscription. */
    @Transactional(readOnly = true)
    public Page<PlanResponseDto> listActivePlans(Pageable pageable) {
        return planRepository.findAllByActiveTrue(pageable).map(planMapper::toDto);
    }

    /** Admin list: includes inactive plans so they can be reactivated. */
    @Transactional(readOnly = true)
    public Page<PlanResponseDto> listAllPlans(Pageable pageable) {
        return planRepository.findAll(pageable).map(planMapper::toDto);
    }

    @Transactional
    public PlanResponseDto updatePlan(UUID id, PlanRequestDto requestDto) {
        Plan plan = findPlan(id);

        if (planRepository.existsByNameAndIdNot(requestDto.name(), id)) {
            throw new ConflictException("Ya existe un plan con el nombre '" + requestDto.name() + "'");
        }

        planMapper.updateEntity(requestDto, plan);
        log.info("Plan updated: id={}", id);
        return planMapper.toDto(plan);
    }

    /**
     * A plan with subscribed users cannot be deleted (users.plan_id has no
     * ON DELETE). Deactivate it instead with isActive=false.
     */
    @Transactional
    public void deletePlan(UUID id) {
        Plan plan = findPlan(id);

        if (userRepository.existsByPlanId(id)) {
            throw new ConflictException(
                    "El plan tiene usuarios suscritos y no puede eliminarse. Desactívalo con isActive=false");
        }

        planRepository.delete(plan);
        log.info("Plan deleted: id={}", id);
    }

    private Plan findPlan(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id.toString()));
    }
}