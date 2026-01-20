package com.unisono.card.merchant.infrastructure.adapter.persistence;

import com.unisono.card.merchant.application.port.output.TerminalPersistencePort;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.*;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.PaymentTerminalEntity;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.mapper.TerminalEntityMapper;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.repository.JpaTerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Terminal
 * Implementa el puerto de salida TerminalPersistencePort
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalPersistenceAdapter implements TerminalPersistencePort {

    private final JpaTerminalRepository jpaTerminalRepository;
    private final TerminalEntityMapper mapper;

    @Override
    public PaymentTerminal save(PaymentTerminal terminal) {
        log.debug("Saving terminal: {}", terminal.getId());
        PaymentTerminalEntity entity = mapper.toEntity(terminal);
        PaymentTerminalEntity savedEntity = jpaTerminalRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PaymentTerminal> findById(TerminalId terminalId) {
        log.debug("Finding terminal by ID: {}", terminalId);
        return jpaTerminalRepository.findById(terminalId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentTerminal> findBySerialNumber(String serialNumber) {
        log.debug("Finding terminal by serial number: {}", serialNumber);
        return jpaTerminalRepository.findBySerialNumber(serialNumber)
                .map(mapper::toDomain);
    }

    @Override
    public List<PaymentTerminal> findByMerchantId(MerchantId merchantId) {
        log.debug("Finding terminals by merchant ID: {}", merchantId);
        return jpaTerminalRepository.findByMerchantId(merchantId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentTerminal> findActiveByMerchantId(MerchantId merchantId) {
        log.debug("Finding active terminals by merchant ID: {}", merchantId);
        return jpaTerminalRepository.findByMerchantIdAndStatus(
                        merchantId.getValue(),
                        TerminalStatus.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentTerminal> findByType(TerminalType type) {
        log.debug("Finding terminals by type: {}", type);
        return jpaTerminalRepository.findByTerminalType(type)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentTerminal> findByStatus(TerminalStatus status) {
        log.debug("Finding terminals by status: {}", status);
        return jpaTerminalRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        log.debug("Checking if terminal exists by serial number: {}", serialNumber);
        return jpaTerminalRepository.existsBySerialNumber(serialNumber);
    }

    @Override
    public long countActiveByMerchantId(MerchantId merchantId) {
        log.debug("Counting active terminals for merchant: {}", merchantId);
        return jpaTerminalRepository.countByMerchantIdAndStatus(
                merchantId.getValue(),
                TerminalStatus.ACTIVE
        );
    }

    @Override
    public void delete(TerminalId terminalId) {
        log.debug("Deleting terminal: {}", terminalId);
        jpaTerminalRepository.deleteById(terminalId.getValue());
    }
}
