package com.duoc.reservams.reservationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// esta clase representa una reserva en la base de datos
@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    // ID principal de la reserva
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID logico del cliente que viene desde user-service
    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    // ID logico del hotel que viene desde hotel-service
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    // ID logico de la habitacion que viene desde room-service
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // fecha de entrada del cliente
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    // fecha de salida del cliente
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    // total de la reserva
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // estado, PENDING_PAYMENT, CONFIRMED, CANCELLED o REJECTED
    @Column(nullable = false, length = 30)
    private String status;

    // fecha en que se creo la reserva
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}