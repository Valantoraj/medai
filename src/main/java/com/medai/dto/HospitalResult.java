package com.medai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalResult {

    private Long osmId;
    private String name;
    private Double lat;
    private Double lon;
    private String phone;
    private String address;
    private String website;
    private boolean emergency;
    private String specialty;
    private Double distanceKm;
}
