package com.duoc.reservams.reservationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO para cambiar el estado de una reserva
@Data
public class ReservationStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    private String status;
}