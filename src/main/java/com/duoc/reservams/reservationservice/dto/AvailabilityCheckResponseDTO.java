package com.duoc.reservams.reservationservice.dto;

import lombok.Data;

// DTO que recibe la respuesta del availability-service
@Data
public class AvailabilityCheckResponseDTO {

    private Long roomId;
    private Boolean available;
    private String message;
}