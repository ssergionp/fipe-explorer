package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.external.CalendarHistoryService;
import com.fipeexplorer.backend.web.dto.CalendarHistoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class CalendarHistoryController {

    private final CalendarHistoryService calendarHistoryService;

    public CalendarHistoryController(CalendarHistoryService calendarHistoryService) {
        this.calendarHistoryService = calendarHistoryService;
    }

    @GetMapping("/calendar-history")
    public CalendarHistoryResponse getCalendarHistory(
            @RequestParam VehicleType type,
            @RequestParam String fipeCode,
            @RequestParam String yearCode) {
        return calendarHistoryService.getHistory(type, fipeCode, yearCode);
    }
}
