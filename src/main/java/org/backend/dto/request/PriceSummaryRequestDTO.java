package org.backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PriceSummaryRequestDTO {

    private Long salonId;

    private Long packageId;

    private List<Long> serviceIds;
}