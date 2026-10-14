package com.bookworm.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EDDResponse {

    private String pinCode;
    private LocalDate tentativeDeliveryDate;
    private Integer deliveryDays;
    private boolean isServiceable;
}
