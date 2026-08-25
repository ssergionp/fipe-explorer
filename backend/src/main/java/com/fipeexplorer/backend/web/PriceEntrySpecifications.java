package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

final class PriceEntrySpecifications {

    private PriceEntrySpecifications() {
    }

    static Specification<PriceEntry> hasVehicleType(String vehicleType) {
        return (root, query, cb) -> cb.equal(root.get("vehicleModel").get("vehicleType"), vehicleType);
    }

    static Specification<PriceEntry> hasBrandId(Long brandId) {
        return (root, query, cb) -> cb.equal(root.get("vehicleModel").get("brand").get("id"), brandId);
    }

    static Specification<PriceEntry> hasModelId(Long modelId) {
        return (root, query, cb) -> cb.equal(root.get("vehicleModel").get("id"), modelId);
    }

    /**
     * year_code segue o padrão FIPE "AAAA-N" (N = índice do combustível); casa pelo prefixo
     * numérico do ano, então year=1991 encontra "1991-1", "1991-3" etc.
     */
    static Specification<PriceEntry> hasYear(Integer year) {
        return (root, query, cb) -> cb.like(root.get("yearCode"), year + "-%");
    }

    static Specification<PriceEntry> hasFuel(String fuelName) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("fuelType").get("name")), fuelName.toLowerCase());
    }

    static Specification<PriceEntry> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    static Specification<PriceEntry> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
