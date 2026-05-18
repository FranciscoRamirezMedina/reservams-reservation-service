package com.duoc.reservams.reservationservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO para crear una reserva
@Data
public class ReservationRequestDTO {

    @NotNull(message = "El clientUserId es obligatorio")
    private Long clientUserId;

    @NotNull(message = "El hotelId es obligatorio")
    private Long hotelId;

    @NotNull(message = "El roomId es obligatorio")
    private Long roomId;

    @NotNull(message = "La fecha de entrada es obligatoria")
    private LocalDate checkInDate;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate checkOutDate;

    @NotNull(message = "El total de la reserva es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El total debe ser mayor a 0")
    private BigDecimal totalAmount;
}