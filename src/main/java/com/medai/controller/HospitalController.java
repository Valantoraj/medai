package com.medai.controller;

import com.medai.dto.HospitalResult;
import com.medai.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * GET /api/hospitals/nearby?lat=12.9716&lon=77.5946&radius=5000
     * Returns hospitals sorted by distance from the user's coordinates.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<HospitalResult>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") int radius) {

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ResponseEntity.badRequest().build();
        }
        if (radius < 500 || radius > 20000) {
            radius = 5000;
        }

        List<HospitalResult> hospitals = hospitalService.findNearby(lat, lon, radius);
        return ResponseEntity.ok(hospitals);
    }
}
