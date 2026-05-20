package com.duoc.reservams.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

// DTO que se envia al availability-service para consultar disponibilidad
@Data
@AllArgsConstructor
public class AvailabilityCheckRequestDTO {

    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}