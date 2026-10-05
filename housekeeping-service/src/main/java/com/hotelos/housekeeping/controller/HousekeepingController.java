package com.hotelos.housekeeping.controller;

import com.hotelos.housekeeping.domain.CleaningTask;
import com.hotelos.housekeeping.domain.MarkCleanResult;
import com.hotelos.housekeeping.domain.MarkCleanStatus;
import com.hotelos.housekeeping.dto.ReportDefectRequest;
import com.hotelos.housekeeping.exception.ConflictException;
import com.hotelos.housekeeping.service.HousekeepingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/housekeeping")
public class HousekeepingController {
    private final HousekeepingService housekeepingService;

    public HousekeepingController(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @PostMapping("/rooms/{roomNumber}/report-defect")
    public Map<String, Object> reportDefect(
            @PathVariable String roomNumber,
            @RequestBody ReportDefectRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return housekeepingService.reportDefect(roomNumber, request, idempotencyKey);
    }

    @GetMapping("/queue")
    public List<CleaningTask> queue() {
        return housekeepingService.getQueue();
    }

    @PostMapping("/queue/{roomNumber}")
    public CleaningTask addToQueue(@PathVariable String roomNumber) {
        return housekeepingService.addToQueue(roomNumber);
    }

    @PostMapping("/rooms/{roomNumber}/start")
    public CleaningTask start(@PathVariable String roomNumber) {
        return housekeepingService.startCleaning(roomNumber);
    }

    @PostMapping("/rooms/{roomNumber}/clean")
    public CleaningTask clean(@PathVariable String roomNumber) {
        MarkCleanResult result = housekeepingService.markClean(roomNumber);
        if (result.status() == MarkCleanStatus.RESTART_REQUIRED) {
            throw new ConflictException("MAINTENANCE_CLEANUP_NOT_COVERED",
                    "Maintenance work was completed after current cleaning started. Room must be re-cleaned.");
        }
        return result.task();
    }

    @PatchMapping("/queue/{roomNumber}/cancel")
    public CleaningTask cancel(@PathVariable String roomNumber) {
        return housekeepingService.cancel(roomNumber);
    }

    @GetMapping("/cleaners")
    public List<String> cleaners() {
        return housekeepingService.getCleaners();
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return housekeepingService.reset();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return housekeepingService.reset();
    }
}
