package com.duoc.reservams.reservationservice.service;

import com.duoc.reservams.reservationservice.client.AvailabilityClient;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationRequestDTO;
import com.duoc.reservams.reservationservice.dto.ReservationResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationStatusUpdateDTO;
import com.duoc.reservams.reservationservice.model.Reservation;
import com.duoc.reservams.reservationservice.producer.ReservationEventProducer;
import com.duoc.reservams.reservationservice.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// pruebas unitarias para ReservationService
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AvailabilityClient availabilityClient;

    @Mock
    private ReservationEventProducer reservationEventProducer;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void findAll_shouldReturnReservations() {
        // Given
        when(reservationRepository.findAll()).thenReturn(List.of(
                buildReservation(1L, "PENDING_PAYMENT"),
                buildReservation(2L, "CONFIRMED")
        ));

        // When
        List<ReservationResponseDTO> response = reservationService.findAll();

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());

        verify(reservationRepository, times(1)).findAll();
    }

    @Test
    void findById_shouldReturnReservation_whenExists() {
        // Given
        Reservation reservation = buildReservation(1L, "PENDING_PAYMENT");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        // When
        ReservationResponseDTO response = reservationService.findById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PENDING_PAYMENT", response.getStatus());

        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    void findById_shouldThrowException_whenReservationNotFound() {
        // Given
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.findById(99L)
        );

        // Then
        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository, times(1)).findById(99L);
    }

    @Test
    void findByClientUserId_shouldReturnReservations() {
        // Given
        when(reservationRepository.findByClientUserId(1L)).thenReturn(List.of(
                buildReservation(1L, "PENDING_PAYMENT")
        ));

        // When
        List<ReservationResponseDTO> response = reservationService.findByClientUserId(1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getClientUserId());

        verify(reservationRepository, times(1)).findByClientUserId(1L);
    }

    @Test
    void findByHotelId_shouldReturnReservations() {
        // Given
        when(reservationRepository.findByHotelId(1L)).thenReturn(List.of(
                buildReservation(1L, "PENDING_PAYMENT")
        ));

        // When
        List<ReservationResponseDTO> response = reservationService.findByHotelId(1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getHotelId());

        verify(reservationRepository, times(1)).findByHotelId(1L);
    }

    @Test
    void findByRoomId_shouldReturnReservations() {
        // Given
        when(reservationRepository.findByRoomId(1L)).thenReturn(List.of(
                buildReservation(1L, "PENDING_PAYMENT")
        ));

        // When
        List<ReservationResponseDTO> response = reservationService.findByRoomId(1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getRoomId());

        verify(reservationRepository, times(1)).findByRoomId(1L);
    }

    @Test
    void findByStatus_shouldReturnReservations() {
        // Given
        when(reservationRepository.findByStatus("CONFIRMED")).thenReturn(List.of(
                buildReservation(1L, "CONFIRMED")
        ));

        // When
        List<ReservationResponseDTO> response = reservationService.findByStatus("CONFIRMED");

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("CONFIRMED", response.get(0).getStatus());

        verify(reservationRepository, times(1)).findByStatus("CONFIRMED");
    }

    @Test
    void create_shouldCreateReservation_whenAvailabilityIsTrue() {
        // Given
        ReservationRequestDTO request = buildReservationRequest();

        AvailabilityCheckResponseDTO availabilityResponse = new AvailabilityCheckResponseDTO();
        availabilityResponse.setRoomId(1L);
        availabilityResponse.setAvailable(true);
        availabilityResponse.setMessage("La habitacion esta disponible");

        when(availabilityClient.checkAvailability(any())).thenReturn(availabilityResponse);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        // When
        ReservationResponseDTO response = reservationService.create(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getClientUserId());
        assertEquals(1L, response.getHotelId());
        assertEquals(1L, response.getRoomId());
        assertEquals("PENDING_PAYMENT", response.getStatus());
        assertEquals(new BigDecimal("120000"), response.getTotalAmount());

        verify(availabilityClient, times(1)).checkAvailability(any());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(reservationEventProducer, times(1)).publishReservationCreatedEvent(any());
    }

    @Test
    void create_shouldThrowException_whenCheckOutDateIsBeforeCheckInDate() {
        // Given
        ReservationRequestDTO request = buildReservationRequest();
        request.setCheckInDate(LocalDate.of(2026, 7, 12));
        request.setCheckOutDate(LocalDate.of(2026, 7, 10));

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.create(request)
        );

        // Then
        assertEquals("La fecha de salida debe ser posterior a la fecha de entrada", exception.getMessage());

        verify(availabilityClient, never()).checkAvailability(any());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(reservationEventProducer, never()).publishReservationCreatedEvent(any());
    }

    @Test
    void create_shouldThrowException_whenAvailabilityIsFalse() {
        // Given
        ReservationRequestDTO request = buildReservationRequest();

        AvailabilityCheckResponseDTO availabilityResponse = new AvailabilityCheckResponseDTO();
        availabilityResponse.setRoomId(1L);
        availabilityResponse.setAvailable(false);
        availabilityResponse.setMessage("La habitacion no esta disponible");

        when(availabilityClient.checkAvailability(any())).thenReturn(availabilityResponse);

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.create(request)
        );

        // Then
        assertTrue(exception.getMessage().contains("No se pudo validar la disponibilidad"));
        assertTrue(exception.getMessage().contains("La habitacion no esta disponible"));

        verify(availabilityClient, times(1)).checkAvailability(any());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(reservationEventProducer, never()).publishReservationCreatedEvent(any());
    }

    @Test
    void updateStatus_shouldUpdateReservationStatus() {
        // Given
        Reservation reservation = buildReservation(1L, "PENDING_PAYMENT");

        ReservationStatusUpdateDTO request = new ReservationStatusUpdateDTO();
        request.setStatus("CONFIRMED");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation updatedReservation = invocation.getArgument(0);
            return updatedReservation;
        });

        // When
        ReservationResponseDTO response = reservationService.updateStatus(1L, request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CONFIRMED", response.getStatus());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void updateStatus_shouldThrowException_whenReservationNotFound() {
        // Given
        ReservationStatusUpdateDTO request = new ReservationStatusUpdateDTO();
        request.setStatus("CONFIRMED");

        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.updateStatus(99L, request)
        );

        // Then
        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository, times(1)).findById(99L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void confirm_shouldConfirmReservation_whenStatusIsPendingPayment() {
        // Given
        Reservation reservation = buildReservation(1L, "PENDING_PAYMENT");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation updatedReservation = invocation.getArgument(0);
            return updatedReservation;
        });

        // When
        ReservationResponseDTO response = reservationService.confirm(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CONFIRMED", response.getStatus());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void confirm_shouldThrowException_whenReservationNotFound() {
        // Given
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.confirm(99L)
        );

        // Then
        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository, times(1)).findById(99L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void confirm_shouldThrowException_whenStatusIsNotPendingPayment() {
        // Given
        Reservation reservation = buildReservation(1L, "CONFIRMED");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.confirm(1L)
        );

        // Then
        assertEquals("Solo se puede confirmar una reserva pendiente de pago", exception.getMessage());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void cancel_shouldCancelReservation_whenReservationExists() {
        // Given
        Reservation reservation = buildReservation(1L, "CONFIRMED");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation updatedReservation = invocation.getArgument(0);
            return updatedReservation;
        });

        // When
        ReservationResponseDTO response = reservationService.cancel(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CANCELLED", response.getStatus());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void cancel_shouldThrowException_whenReservationNotFound() {
        // Given
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.cancel(99L)
        );

        // Then
        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository, times(1)).findById(99L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void cancel_shouldThrowException_whenReservationAlreadyCancelled() {
        // Given
        Reservation reservation = buildReservation(1L, "CANCELLED");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.cancel(1L)
        );

        // Then
        assertEquals("La reserva ya se encuentra cancelada", exception.getMessage());

        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void delete_shouldDeleteReservation_whenReservationExists() {
        // Given
        when(reservationRepository.existsById(1L)).thenReturn(true);

        // When
        reservationService.delete(1L);

        // Then
        verify(reservationRepository, times(1)).existsById(1L);
        verify(reservationRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldThrowException_whenReservationNotFound() {
        // Given
        when(reservationRepository.existsById(99L)).thenReturn(false);

        // When
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.delete(99L)
        );

        // Then
        assertEquals("Reserva no encontrada", exception.getMessage());

        verify(reservationRepository, times(1)).existsById(99L);
        verify(reservationRepository, never()).deleteById(anyLong());
    }

    private ReservationRequestDTO buildReservationRequest() {
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setClientUserId(1L);
        request.setHotelId(1L);
        request.setRoomId(1L);
        request.setCheckInDate(LocalDate.of(2026, 7, 10));
        request.setCheckOutDate(LocalDate.of(2026, 7, 12));
        request.setTotalAmount(new BigDecimal("120000"));
        return request;
    }

    private Reservation buildReservation(Long id, String status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setClientUserId(1L);
        reservation.setHotelId(1L);
        reservation.setRoomId(1L);
        reservation.setCheckInDate(LocalDate.of(2026, 7, 10));
        reservation.setCheckOutDate(LocalDate.of(2026, 7, 12));
        reservation.setTotalAmount(new BigDecimal("120000"));
        reservation.setStatus(status);
        reservation.setCreatedAt(LocalDateTime.now());
        return reservation;
    }
}