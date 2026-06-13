package com.duoc.reservams.reservationservice.service;

import com.duoc.reservams.reservationservice.client.AvailabilityClient;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckRequestDTO;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationRequestDTO;
import com.duoc.reservams.reservationservice.dto.ReservationResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationStatusUpdateDTO;
import com.duoc.reservams.reservationservice.event.ReservationCreatedEvent;
import com.duoc.reservams.reservationservice.model.Reservation;
import com.duoc.reservams.reservationservice.producer.ReservationEventProducer;
import com.duoc.reservams.reservationservice.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// aqui va la logica de negocio de las reservas
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;

    // cliente Feign para consultar disponibilidad en availability-service
    private final AvailabilityClient availabilityClient;

    // producer Kafka para publicar eventos de reservas
    private final ReservationEventProducer reservationEventProducer;

    public ReservationService(ReservationRepository reservationRepository,
                              AvailabilityClient availabilityClient,
                              ReservationEventProducer reservationEventProducer) {
        this.reservationRepository = reservationRepository;
        this.availabilityClient = availabilityClient;
        this.reservationEventProducer = reservationEventProducer;
    }

    public List<ReservationResponseDTO> findAll() {
        logger.info("Listando todas las reservas");

        List<ReservationResponseDTO> reservations = reservationRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();

        logger.info("Total de reservas encontradas: {}", reservations.size());

        return reservations;
    }

    public ReservationResponseDTO findById(Long id) {
        logger.info("Buscando reserva por ID {}", id);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No se encontro reserva con ID {}", id);
                    return new RuntimeException("Reserva no encontrada");
                });

        logger.info("Reserva encontrada con ID {} y estado {}", reservation.getId(), reservation.getStatus());

        return toResponseDTO(reservation);
    }

    public List<ReservationResponseDTO> findByClientUserId(Long clientUserId) {
        logger.info("Listando reservas del cliente ID {}", clientUserId);

        List<ReservationResponseDTO> reservations = reservationRepository.findByClientUserId(clientUserId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        logger.info("Total de reservas encontradas para cliente ID {}: {}", clientUserId, reservations.size());

        return reservations;
    }

    public List<ReservationResponseDTO> findByHotelId(Long hotelId) {
        logger.info("Listando reservas del hotel ID {}", hotelId);

        List<ReservationResponseDTO> reservations = reservationRepository.findByHotelId(hotelId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        logger.info("Total de reservas encontradas para hotel ID {}: {}", hotelId, reservations.size());

        return reservations;
    }

    public List<ReservationResponseDTO> findByRoomId(Long roomId) {
        logger.info("Listando reservas de la habitacion ID {}", roomId);

        List<ReservationResponseDTO> reservations = reservationRepository.findByRoomId(roomId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        logger.info("Total de reservas encontradas para habitacion ID {}: {}", roomId, reservations.size());

        return reservations;
    }

    public List<ReservationResponseDTO> findByStatus(String status) {
        logger.info("Listando reservas con estado {}", status);

        List<ReservationResponseDTO> reservations = reservationRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        logger.info("Total de reservas encontradas con estado {}: {}", status, reservations.size());

        return reservations;
    }

    public ReservationResponseDTO create(ReservationRequestDTO request) {
        logger.info("Iniciando creacion de reserva para cliente ID {}, hotel ID {}, habitacion ID {}",
                request.getClientUserId(),
                request.getHotelId(),
                request.getRoomId());

        logger.info("Validando fechas de reserva. Check-in: {}, Check-out: {}",
                request.getCheckInDate(),
                request.getCheckOutDate());

        // la fecha de salida siempre debe ser posterior a la fecha de entrada
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            logger.warn("Fechas invalidas para reserva. Check-in: {}, Check-out: {}",
                    request.getCheckInDate(),
                    request.getCheckOutDate());

            throw new RuntimeException("La fecha de salida debe ser posterior a la fecha de entrada");
        }

        try {
            // antes de guardar la reserva, consultamos disponibilidad a otro microservicio
            AvailabilityCheckRequestDTO availabilityRequest = new AvailabilityCheckRequestDTO(
                    request.getRoomId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );

            logger.info("Consultando availability-service para habitacion ID {} entre {} y {}",
                    request.getRoomId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate());

            AvailabilityCheckResponseDTO availabilityResponse =
                    availabilityClient.checkAvailability(availabilityRequest);

            logger.info("Respuesta de availability-service para habitacion ID {}: disponible={}, mensaje={}",
                    request.getRoomId(),
                    availabilityResponse.getAvailable(),
                    availabilityResponse.getMessage());

            // si availability-service responde false, no se crea la reserva
            if (!availabilityResponse.getAvailable()) {
                logger.warn("Disponibilidad rechazada para habitacion ID {}. Motivo: {}",
                        request.getRoomId(),
                        availabilityResponse.getMessage());

                throw new RuntimeException(availabilityResponse.getMessage());
            }

            logger.info("Disponibilidad validada correctamente para habitacion ID {}", request.getRoomId());

        } catch (Exception ex) {
            logger.error("Error al validar disponibilidad para habitacion ID {}. Detalle: {}",
                    request.getRoomId(),
                    ex.getMessage());

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

        logger.info("Guardando reserva con estado inicial {}", reservation.getStatus());

        Reservation savedReservation = reservationRepository.save(reservation);

        logger.info("Reserva creada correctamente con ID {} y estado {}",
                savedReservation.getId(),
                savedReservation.getStatus());

        ReservationCreatedEvent event = new ReservationCreatedEvent(
                savedReservation.getId(),
                savedReservation.getClientUserId(),
                savedReservation.getHotelId(),
                savedReservation.getRoomId(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate(),
                savedReservation.getTotalAmount(),
                savedReservation.getStatus(),
                "Reserva creada correctamente"
        );

        reservationEventProducer.publishReservationCreatedEvent(event);

        return toResponseDTO(savedReservation);
    }

    public ReservationResponseDTO updateStatus(Long id, ReservationStatusUpdateDTO request) {
        logger.info("Iniciando actualizacion de estado para reserva ID {}. Nuevo estado: {}",
                id,
                request.getStatus());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No se encontro reserva con ID {} para actualizar estado", id);
                    return new RuntimeException("Reserva no encontrada");
                });

        logger.info("Reserva ID {} encontrada para actualizar estado. Estado anterior: {}, estado nuevo solicitado: {}",
                id,
                reservation.getStatus(),
                request.getStatus());

        reservation.setStatus(request.getStatus());

        Reservation updatedReservation = reservationRepository.save(reservation);

        logger.info("Estado de reserva ID {} actualizado correctamente a {}",
                updatedReservation.getId(),
                updatedReservation.getStatus());

        return toResponseDTO(updatedReservation);
    }

    public ReservationResponseDTO confirm(Long id) {
        logger.info("Iniciando confirmacion de reserva ID {}", id);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No se encontro reserva con ID {} para confirmar", id);
                    return new RuntimeException("Reserva no encontrada");
                });

        logger.info("Reserva ID {} encontrada para confirmacion. Estado actual antes de confirmar: {}",
                id,
                reservation.getStatus());

        if (!reservation.getStatus().equals("PENDING_PAYMENT")) {
            logger.warn("No se puede confirmar reserva ID {} porque su estado actual es {}",
                    id,
                    reservation.getStatus());

            throw new RuntimeException("Solo se puede confirmar una reserva pendiente de pago");
        }

        reservation.setStatus("CONFIRMED");

        Reservation updatedReservation = reservationRepository.save(reservation);

        logger.info("Reserva ID {} confirmada correctamente",
                updatedReservation.getId());

        return toResponseDTO(updatedReservation);
    }

    public ReservationResponseDTO cancel(Long id) {
        logger.info("Iniciando cancelacion de reserva ID {}", id);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No se encontro reserva con ID {} para cancelar", id);
                    return new RuntimeException("Reserva no encontrada");
                });

        logger.info("Reserva ID {} encontrada para cancelacion. Estado actual antes de cancelar: {}",
                id,
                reservation.getStatus());

        if (reservation.getStatus().equals("CANCELLED")) {
            logger.warn("La reserva ID {} ya se encuentra cancelada", id);

            throw new RuntimeException("La reserva ya se encuentra cancelada");
        }

        reservation.setStatus("CANCELLED");

        Reservation updatedReservation = reservationRepository.save(reservation);

        logger.info("Reserva ID {} cancelada correctamente",
                updatedReservation.getId());

        return toResponseDTO(updatedReservation);
    }

    public void delete(Long id) {
        logger.info("Iniciando eliminacion de reserva ID {}", id);

        if (!reservationRepository.existsById(id)) {
            logger.warn("No se encontro reserva con ID {} para eliminar", id);

            throw new RuntimeException("Reserva no encontrada");
        }

        reservationRepository.deleteById(id);

        logger.info("Reserva ID {} eliminada correctamente", id);
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