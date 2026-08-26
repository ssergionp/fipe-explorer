package com.fipeexplorer.backend.repository;

import java.math.BigDecimal;

public interface BrandAveragePriceProjection {

    Long getBrandId();

    String getBrandName();

    BigDecimal getAvgPrice();

    long getModelCount();
}
