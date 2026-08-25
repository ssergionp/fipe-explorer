package com.fipeexplorer.backend.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FipePriceParserTest {

    @Test
    void parsesPriceWithThousandsSeparator() {
        assertThat(FipePriceParser.parse("R$ 10.063,00")).isEqualByComparingTo(new BigDecimal("10063.00"));
    }

    @Test
    void parsesPriceWithoutThousandsSeparator() {
        assertThat(FipePriceParser.parse("R$ 999,00")).isEqualByComparingTo(new BigDecimal("999.00"));
    }

    @Test
    void parsesPriceWithMultipleThousandsSeparators() {
        assertThat(FipePriceParser.parse("R$ 1.234.567,89")).isEqualByComparingTo(new BigDecimal("1234567.89"));
    }

    @Test
    void parsesLowValuePriceWithoutSeparator() {
        assertThat(FipePriceParser.parse("R$ 73,50")).isEqualByComparingTo(new BigDecimal("73.50"));
    }

    @Test
    void rejectsBlankPrice() {
        assertThatThrownBy(() -> FipePriceParser.parse("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
