package com.unisono.card.merchant.domain.repository;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.ContractId;
import com.unisono.card.merchant.domain.model.contract.ContractStatus;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para contratos de merchants
 */
public interface MerchantContractRepository {

    /**
     * Guarda o actualiza un contrato
     */
    MerchantContract save(MerchantContract contract);

    /**
     * Busca un contrato por su ID
     */
    Optional<MerchantContract> findById(ContractId contractId);

    /**
     * Busca el contrato activo de un merchant
     */
    Optional<MerchantContract> findActiveByMerchantId(MerchantId merchantId);

    /**
     * Busca todos los contratos de un merchant
     */
    List<MerchantContract> findByMerchantId(MerchantId merchantId);

    /**
     * Busca contratos por estado
     */
    List<MerchantContract> findByStatus(ContractStatus status);

    /**
     * Busca contratos que expiran antes de una fecha
     */
    List<MerchantContract> findExpiringBefore(LocalDate date);

    /**
     * Busca contratos que expiran en un rango de días
     */
    List<MerchantContract> findExpiringInDays(int days);

    /**
     * Verifica si un merchant tiene un contrato activo
     */
    boolean hasActiveContract(MerchantId merchantId);

    /**
     * Elimina un contrato
     */
    void delete(ContractId contractId);

}

