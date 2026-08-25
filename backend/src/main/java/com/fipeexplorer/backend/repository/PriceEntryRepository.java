package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.PriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceEntryRepository extends JpaRepository<PriceEntry, Long>, JpaSpecificationExecutor<PriceEntry> {

    List<PriceEntry> findByVehicleModel_IdOrderByYearCodeAsc(Long vehicleModelId);

    /**
     * year_code segue o padrão FIPE "AAAA-N" (N = índice do combustível), com "32000" para
     * zero km. split_part extrai o prefixo numérico do ano; é nativa (Postgres) porque JPQL não
     * tem equivalente a split_part.
     */
    @Query(value = """
            select distinct cast(split_part(pe.year_code, '-', 1) as integer) as vehicle_year
            from price_entry pe
            join vehicle_model vm on vm.id = pe.vehicle_model_id
            where vm.vehicle_type = :vehicleType
            order by vehicle_year desc
            """, nativeQuery = true)
    List<Integer> findDistinctYearsByVehicleType(@Param("vehicleType") String vehicleType);
}
