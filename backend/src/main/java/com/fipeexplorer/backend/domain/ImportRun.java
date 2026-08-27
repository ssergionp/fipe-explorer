package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** Um registro por mês de referência já importado - dedup é por reference_month_key, não filename. */
@Entity
@Table(name = "import_run")
public class ImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "reference_month", nullable = false)
    private String referenceMonth;

    @Column(name = "reference_month_key", nullable = false, unique = true)
    private LocalDate referenceMonthKey;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected ImportRun() {
    }

    public ImportRun(String filename, String referenceMonth, LocalDate referenceMonthKey, int rowCount) {
        this.filename = filename;
        this.referenceMonth = referenceMonth;
        this.referenceMonthKey = referenceMonthKey;
        this.rowCount = rowCount;
    }

    @PrePersist
    protected void onCreate() {
        this.importedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public LocalDate getReferenceMonthKey() {
        return referenceMonthKey;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
