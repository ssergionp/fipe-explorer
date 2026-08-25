package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.FuelType;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.FuelTypeRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FipeCsvImportService {

    private static final Logger log = LoggerFactory.getLogger(FipeCsvImportService.class);
    private static final int BATCH_SIZE = 500;
    private static final String INSERT_PRICE_ENTRY_SQL = """
            INSERT INTO price_entry
                (vehicle_model_id, fuel_type_id, year_code, year_value, price, reference_month)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final BrandRepository brandRepository;
    private final FuelTypeRepository fuelTypeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final JdbcTemplate jdbcTemplate;

    public FipeCsvImportService(BrandRepository brandRepository,
                                 FuelTypeRepository fuelTypeRepository,
                                 VehicleModelRepository vehicleModelRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.brandRepository = brandRepository;
        this.fuelTypeRepository = fuelTypeRepository;
        this.vehicleModelRepository = vehicleModelRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void importCsv(Path csvPath) {
        log.info("Iniciando importação do CSV da Tabela FIPE: {}", csvPath.toAbsolutePath());

        Map<String, Brand> brandCache = new HashMap<>();
        Map<String, FuelType> fuelTypeCache = new HashMap<>();
        Map<String, VehicleModel> modelCache = new HashMap<>();
        List<Object[]> pendingPriceEntries = new ArrayList<>(BATCH_SIZE);

        int imported = 0;
        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Brand brand = brandCache.computeIfAbsent(record.get("Brand Code"),
                        code -> resolveBrand(code, record.get("Brand Value")));

                FuelType fuelType = fuelTypeCache.computeIfAbsent(record.get("Fuel Type"),
                        name -> resolveFuelType(record.get("Fuel Letter"), name));

                String modelCacheKey = brand.getId() + "|" + record.get("Model Code");
                VehicleModel vehicleModel = modelCache.computeIfAbsent(modelCacheKey,
                        key -> resolveVehicleModel(brand, record));

                BigDecimal price = FipePriceParser.parse(record.get("Price"));

                pendingPriceEntries.add(new Object[] {
                        vehicleModel.getId(),
                        fuelType.getId(),
                        record.get("Year Code"),
                        record.get("Year Value"),
                        price,
                        record.get("Month")
                });

                imported++;
                if (pendingPriceEntries.size() >= BATCH_SIZE) {
                    flushPriceEntries(pendingPriceEntries);
                }
            }

            flushPriceEntries(pendingPriceEntries);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o CSV da Tabela FIPE: " + csvPath, e);
        }

        log.info("Importação concluída: {} marcas, {} combustíveis, {} modelos, {} registros de preço",
                brandCache.size(), fuelTypeCache.size(), modelCache.size(), imported);
    }

    private void flushPriceEntries(List<Object[]> pendingPriceEntries) {
        if (pendingPriceEntries.isEmpty()) {
            return;
        }
        int[] argTypes = {Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.VARCHAR};
        jdbcTemplate.batchUpdate(INSERT_PRICE_ENTRY_SQL, pendingPriceEntries, argTypes);
        pendingPriceEntries.clear();
    }

    private Brand resolveBrand(String fipeCode, String name) {
        return brandRepository.findByFipeCode(fipeCode)
                .orElseGet(() -> brandRepository.save(new Brand(fipeCode, name)));
    }

    private FuelType resolveFuelType(String code, String name) {
        return fuelTypeRepository.findByName(name)
                .orElseGet(() -> fuelTypeRepository.save(new FuelType(code, name)));
    }

    private VehicleModel resolveVehicleModel(Brand brand, CSVRecord record) {
        String modelCode = record.get("Model Code");
        return vehicleModelRepository.findByBrandIdAndFipeModelCode(brand.getId(), modelCode)
                .orElseGet(() -> vehicleModelRepository.save(new VehicleModel(
                        brand,
                        modelCode,
                        record.get("Model Value"),
                        record.get("Type"),
                        record.get("Fipe Code"))));
    }
}
