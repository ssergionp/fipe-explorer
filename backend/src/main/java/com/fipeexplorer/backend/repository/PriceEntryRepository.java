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

    @Query(value = """
            select
                count(*) as "totalPriceEntries",
                count(distinct pe.vehicle_model_id) as "distinctModels",
                min(pe.price) as "minPrice",
                round(avg(pe.price), 2) as "avgPrice",
                max(pe.price) as "maxPrice"
            from price_entry pe
            join vehicle_model vm on vm.id = pe.vehicle_model_id
            where vm.vehicle_type = :vehicleType
            """, nativeQuery = true)
    VehicleTypeSummaryProjection findSummaryByVehicleType(@Param("vehicleType") String vehicleType);

    @Query(value = """
            select ft.name as "fuel", count(*) as "count"
            from price_entry pe
            join vehicle_model vm on vm.id = pe.vehicle_model_id
            join fuel_type ft on ft.id = pe.fuel_type_id
            where vm.vehicle_type = :vehicleType
            group by ft.name
            order by count(*) desc
            """, nativeQuery = true)
    List<FuelCountProjection> findFuelDistributionByVehicleType(@Param("vehicleType") String vehicleType);

    /**
     * Preço médio por marca, usando só o ano mais recente de cada modelo (não todos os
     * price_entries) — uma marca com modelos mais antigos tem mais linhas no CSV (um ano de
     * fabricação = uma linha), o que infla/distorce a média se somarmos o histórico inteiro.
     * Ex. real: sem esse filtro, "Saturn" (1 modelo, só 2 anos históricos muito antigos)
     * aparecia entre as mais baratas; com "ano mais recente", ela sai do ranking porque o
     * histórico velho não pesa mais.
     * <p>
     * O mesmo cuidado vale dentro de um único ano: se o ano mais recente do modelo tem mais de
     * um combustível (comum — ex. Fiat Elba com Gasolina e Álcool no mesmo ano), a CTE
     * {@code model_latest_price} agrupa por modelo e tira a média entre os combustíveis antes de
     * subir pro nível de marca — cada modelo contribui exatamente UMA vez pra média da marca,
     * nunca uma vez por combustível.
     */
    @Query(value = """
            with model_latest_year as (
                select vm.id as model_id, max(split_part(pe.year_code, '-', 1)::int) as latest_year
                from price_entry pe
                join vehicle_model vm on vm.id = pe.vehicle_model_id
                where vm.vehicle_type = :vehicleType
                group by vm.id
            ),
            model_latest_price as (
                select mly.model_id, avg(pe.price) as model_price
                from price_entry pe
                join model_latest_year mly
                    on mly.model_id = pe.vehicle_model_id
                   and split_part(pe.year_code, '-', 1)::int = mly.latest_year
                group by mly.model_id
            )
            select
                b.id as "brandId",
                b.name as "brandName",
                round(avg(mlp.model_price), 2) as "avgPrice",
                count(*) as "modelCount"
            from model_latest_price mlp
            join vehicle_model vm on vm.id = mlp.model_id
            join brand b on b.id = vm.brand_id
            group by b.id, b.name
            order by "avgPrice" desc
            limit :limit
            """, nativeQuery = true)
    List<BrandAveragePriceProjection> findTopBrandsByAvgPriceDesc(@Param("vehicleType") String vehicleType,
                                                                    @Param("limit") int limit);

    @Query(value = """
            with model_latest_year as (
                select vm.id as model_id, max(split_part(pe.year_code, '-', 1)::int) as latest_year
                from price_entry pe
                join vehicle_model vm on vm.id = pe.vehicle_model_id
                where vm.vehicle_type = :vehicleType
                group by vm.id
            ),
            model_latest_price as (
                select mly.model_id, avg(pe.price) as model_price
                from price_entry pe
                join model_latest_year mly
                    on mly.model_id = pe.vehicle_model_id
                   and split_part(pe.year_code, '-', 1)::int = mly.latest_year
                group by mly.model_id
            )
            select
                b.id as "brandId",
                b.name as "brandName",
                round(avg(mlp.model_price), 2) as "avgPrice",
                count(*) as "modelCount"
            from model_latest_price mlp
            join vehicle_model vm on vm.id = mlp.model_id
            join brand b on b.id = vm.brand_id
            group by b.id, b.name
            order by "avgPrice" asc
            limit :limit
            """, nativeQuery = true)
    List<BrandAveragePriceProjection> findTopBrandsByAvgPriceAsc(@Param("vehicleType") String vehicleType,
                                                                   @Param("limit") int limit);
}
