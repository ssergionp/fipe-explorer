package com.fipeexplorer.backend.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceMonthParserTest {

    @ParameterizedTest
    @CsvSource({
            "agosto de 2026, 2026-08-01",
            "janeiro de 2020, 2020-01-01",
            "dezembro de 1999, 1999-12-01",
            "março de 2015, 2015-03-01",
    })
    void parsesKnownMonths(String referenceMonth, String expectedIsoDate) {
        assertThat(ReferenceMonthParser.parse(referenceMonth)).isEqualTo(LocalDate.parse(expectedIsoDate));
    }

    @Test
    void sortsPortugueseMonthNamesChronologicallyEvenThoughAlphabeticalOrderWouldBeWrong() {
        // Ordem alfabética destes textos seria "agosto de 2025, dezembro de 2025, janeiro de
        // 2026" por coincidência - troquei a entrada pra não bater com a ordem cronológica certa
        // por acidente e mascarar um bug.
        List<String> shuffled = List.of("janeiro de 2026", "agosto de 2025", "dezembro de 2025", "fevereiro de 2025");

        List<String> sortedByParsedDate = shuffled.stream()
                .sorted(Comparator.comparing(ReferenceMonthParser::parse))
                .toList();

        assertThat(sortedByParsedDate).containsExactly(
                "fevereiro de 2025", "agosto de 2025", "dezembro de 2025", "janeiro de 2026");
    }

    @Test
    void rejectsBlankInput() {
        assertThatThrownBy(() -> ReferenceMonthParser.parse("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnrecognizedMonthName() {
        assertThatThrownBy(() -> ReferenceMonthParser.parse("jenuary de 2026"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnexpectedFormat() {
        assertThatThrownBy(() -> ReferenceMonthParser.parse("2026-08"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
