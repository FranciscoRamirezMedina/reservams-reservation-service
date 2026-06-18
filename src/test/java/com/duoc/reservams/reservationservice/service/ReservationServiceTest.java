package com.duoc.reservams.reservationservice.service;

import com.duoc.reservams.reservationservice.client.AvailabilityClient;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationRequestDTO;
import com.duoc.reservams.reservationservice.dto.ReservationResponseDTO;
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
}