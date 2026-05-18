package com.duoc.reservams.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO para responder datos de una reserva
@Data
@AllArgsConstructor
public class ReservationResponseDTO {

    private Long id;
    private Long clientUserId;
    private Long hotelId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
}