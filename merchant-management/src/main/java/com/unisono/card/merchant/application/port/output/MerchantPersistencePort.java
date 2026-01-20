package com.unisono.card.merchant.application.port.output;

import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.MerchantStatus;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de Merchant
 * Implementado por el adaptador de infraestructura
 */
public interface MerchantPersistencePort {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(MerchantId merchantId);

    Optional<Merchant> findByTaxId(String taxId);

    Optional<Merchant> findByEmail(String email);

    List<Merchant> findByCategory(MerchantCategory category);

    List<Merchant> findByStatus(MerchantStatus status);

    List<Merchant> findAll(int page, int size);

    boolean existsByTaxId(String taxId);

    boolean existsByEmail(String email);

    void delete(MerchantId merchantId);

    long count();

    long countByStatus(MerchantStatus status);
}
