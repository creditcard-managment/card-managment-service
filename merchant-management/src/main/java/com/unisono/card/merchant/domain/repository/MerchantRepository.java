package com.unisono.card.merchant.domain.repository;


import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.MerchantStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository para el agregado Merchant
 * Puerto de salida (Output Port) según arquitectura hexagonal
 */
public interface MerchantRepository {

    /**
     * Guarda o actualiza un merchant
     */
    Merchant save(Merchant merchant);

    /**
     * Busca un merchant por su ID
     */
    Optional<Merchant> findById(MerchantId merchantId);

    /**
     * Busca un merchant por su Tax ID (CUIT)
     */
    Optional<Merchant> findByTaxId(String taxId);

    /**
     * Busca un merchant por email de contacto
     */
    Optional<Merchant> findByEmail(String email);

    /**
     * Busca merchants por categoría
     */
    List<Merchant> findByCategory(MerchantCategory category);

    /**
     * Busca merchants por estado
     */
    List<Merchant> findByStatus(MerchantStatus status);

    /**
     * Busca todos los merchants activos
     */
    List<Merchant> findAllActive();

    /**
     * Busca merchants por nombre legal (LIKE)
     */
    List<Merchant> findByLegalNameContaining(String namePart);

    /**
     * Verifica si existe un merchant con el Tax ID dado
     */
    boolean existsByTaxId(String taxId);

    /**
     * Verifica si existe un merchant con el email dado
     */
    boolean existsByEmail(String email);

    /**
     * Elimina un merchant (soft delete recomendado)
     */
    void delete(MerchantId merchantId);

    /**
     * Cuenta merchants por estado
     */
    long countByStatus(MerchantStatus status);

    /**
     * Encuentra merchants que requieren renovación de contrato
     */
    List<Merchant> findMerchantsWithExpiringContracts(int daysUntilExpiration);
}
