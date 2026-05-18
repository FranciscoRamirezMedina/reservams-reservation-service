package com.duoc.reservams.reservationservice.repository;

import com.duoc.reservams.reservationservice.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// repository para acceder a la tabla reservations
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // lista reservas de un cliente
    List<Reservation> findByClientUserId(Long clientUserId);

    // lista reservas de un hotel
    List<Reservation> findByHotelId(Long hotelId);

    // lista reservas de una habitacion
    List<Reservation> findByRoomId(Long roomId);

    // lista reservas por estado
    List<Reservation> findByStatus(String status);
}