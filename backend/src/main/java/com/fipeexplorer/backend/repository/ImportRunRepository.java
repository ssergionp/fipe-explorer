package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.ImportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {

    boolean existsByReferenceMonthKey(LocalDate referenceMonthKey);
}
