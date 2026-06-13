package com.duoc.reservams.reservationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// evento que se envia por Kafka cuando se crea una reserva
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedEvent {

    private Long reservationId;
    private Long clientUserId;
    private Long hotelId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private String status;
    private String message;
}