package com.unisono.card.merchant.infrastructure.adapter.persistence;

import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.model.*;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.MerchantEntity;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.mapper.MerchantEntityMapper;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.repository.JpaMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación JPA del puerto de persistencia de Merchant
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantPersistenceAdapter implements MerchantPersistencePort {

    private final JpaMerchantRepository repository;
    private final MerchantEntityMapper mapper;

    @Override
    @Transactional
    public Merchant save(Merchant merchant) {
        log.debug("Saving merchant: {}", merchant.getId());

        MerchantEntity entity = mapper.toEntity(merchant);
        MerchantEntity savedEntity = repository.save(entity);

        log.debug("Merchant saved successfully: {}", savedEntity.getId());

        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findById(MerchantId id) {
        log.debug("Finding merchant by id: {}", id.getValue());

        return repository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findByTaxId(String taxId) {
        log.debug("Finding merchant by tax id: {}", taxId);

        return repository.findByTaxId(taxId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findByEmail(String email) {
        log.debug("Finding merchant by email: {}", email);

        return repository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> findByCategory(MerchantCategory category) {
        log.debug("Finding merchants by category: {}", category);

        return repository.findByCategory(category).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> findByStatus(MerchantStatus status) {
        log.debug("Finding merchants by status: {}", status);

        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> findAll(int page, int size) {
        log.debug("Finding all merchants - page: {}, size: {}", page, size);

        return repository.findAll().stream()
                .skip((long) page * size)
                .limit(size)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTaxId(String taxId) {
        log.debug("Checking if merchant exists by tax id: {}", taxId);

        return repository.existsByTaxId(taxId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        log.debug("Checking if merchant exists by email: {}", email);

        return repository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void delete(MerchantId id) {
        log.debug("Deleting merchant: {}", id.getValue());

        repository.deleteById(id.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(MerchantStatus status) {
        return repository.countByStatus(status);
    }
}
