package com.bookworm.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingResponse {

    private UUID shipmentId;
    private String carrierName;
    private String trackingNumber;
    private String status;
    private LocalDate estimatedDeliveryDate;
    private List<TrackingEventResponse> events;
}
