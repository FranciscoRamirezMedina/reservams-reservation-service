package com.duoc.reservams.reservationservice.service;

import com.duoc.reservams.reservationservice.client.AvailabilityClient;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckRequestDTO;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationRequestDTO;
import com.duoc.reservams.reservationservice.dto.ReservationResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationStatusUpdateDTO;
import com.duoc.reservams.reservationservice.model.Reservation;
import com.duoc.reservams.reservationservice.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// aqui va la logica de negocio de las reservas
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    // cliente Feign para consultar disponibilidad en availability-service
    private final AvailabilityClient availabilityClient;

    public ReservationService(ReservationRepository reservationRepository,
                              AvailabilityClient availabilityClient) {
        this.reservationRepository = reservationRepository;
        this.availabilityClient = availabilityClient;
    }

    public List<ReservationResponseDTO> findAll() {
        return reservationRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ReservationResponseDTO findById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        return toResponseDTO(reservation);
    }

    public List<ReservationResponseDTO> findByClientUserId(Long clientUserId) {
        return reservationRepository.findByClientUserId(clientUserId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ReservationResponseDTO> findByHotelId(Long hotelId) {
        return reservationRepository.findByHotelId(hotelId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ReservationResponseDTO> findByRoomId(Long roomId) {
        return reservationRepository.findByRoomId(roomId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ReservationResponseDTO> findByStatus(String status) {
        return reservationRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ReservationResponseDTO create(ReservationRequestDTO request) {
        // la fecha de salida siempre debe ser posterior a la fecha de entrada
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new RuntimeException("La fecha de salida debe ser posterior a la fecha de entrada");
        }

        try {
            // antes de guardar la reserva, consultamos disponibilidad a otro microservicio
            AvailabilityCheckRequestDTO availabilityRequest = new AvailabilityCheckRequestDTO(
                    request.getRoomId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );

            AvailabilityCheckResponseDTO availabilityResponse =
                    availabilityClient.checkAvailability(availabilityRequest);

            // si availability-service responde false, no se crea la reserva
            if (!availabilityResponse.getAvailable()) {
                throw new RuntimeException(availabilityResponse.getMessage());
            }

        } catch (Exception ex) {
            throw new RuntimeException("No se pudo validar la disponibilidad: " + ex.getMessage());
        }

        Reservation reservation = new Reservation();
        reservation.setClientUserId(request.getClientUserId());
        reservation.setHotelId(request.getHotelId());
        reservation.setRoomId(request.getRoomId());
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setTotalAmount(request.getTotalAmount());

        // toda reserva nueva parte pendiente de pago
        reservation.setStatus("PENDING_PAYMENT");
        reservation.setCreatedAt(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);

        return toResponseDTO(savedReservation);
    }

    public ReservationResponseDTO updateStatus(Long id, ReservationStatusUpdateDTO request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reservation.setStatus(request.getStatus());

        Reservation updatedReservation = reservationRepository.save(reservation);

        return toResponseDTO(updatedReservation);
    }

    public ReservationResponseDTO confirm(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getStatus().equals("PENDING_PAYMENT")) {
            throw new RuntimeException("Solo se puede confirmar una reserva pendiente de pago");
        }

        reservation.setStatus("CONFIRMED");

        Reservation updatedReservation = reservationRepository.save(reservation);

        return toResponseDTO(updatedReservation);
    }

    public ReservationResponseDTO cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (reservation.getStatus().equals("CANCELLED")) {
            throw new RuntimeException("La reserva ya se encuentra cancelada");
        }

        reservation.setStatus("CANCELLED");

        Reservation updatedReservation = reservationRepository.save(reservation);

        return toResponseDTO(updatedReservation);
    }

    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new RuntimeException("Reserva no encontrada");
        }

        reservationRepository.deleteById(id);
    }

    // convierte una entidad Reservation a DTO de respuesta
    private ReservationResponseDTO toResponseDTO(Reservation reservation) {
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getClientUserId(),
                reservation.getHotelId(),
                reservation.getRoomId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}