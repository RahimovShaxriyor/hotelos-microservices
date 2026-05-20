package com.hotelos.housekeeping.controller;

import com.hotelos.housekeeping.domain.CleaningTask;
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
        return housekeepingService.markClean(roomNumber);
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
