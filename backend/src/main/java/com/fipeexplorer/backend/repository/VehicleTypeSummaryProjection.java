package com.fipeexplorer.backend.repository;

import java.math.BigDecimal;

public interface VehicleTypeSummaryProjection {

    long getTotalPriceEntries();

    long getDistinctModels();

    BigDecimal getMinPrice();

    BigDecimal getAvgPrice();

    BigDecimal getMaxPrice();
}
