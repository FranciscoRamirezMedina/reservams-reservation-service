package com.duoc.reservams.reservationservice.controller;

import com.duoc.reservams.reservationservice.dto.ReservationRequestDTO;
import com.duoc.reservams.reservationservice.dto.ReservationResponseDTO;
import com.duoc.reservams.reservationservice.dto.ReservationStatusUpdateDTO;
import com.duoc.reservams.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// controlador REST para manejar reservas
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // lista todas las reservas
    @GetMapping
    public ResponseEntity<List<ReservationResponseDTO>> findAll() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    // busca una reserva por ID
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    // lista reservas de un cliente
    @GetMapping("/client/{clientUserId}")
    public ResponseEntity<List<ReservationResponseDTO>> findByClientUserId(@PathVariable Long clientUserId) {
        return ResponseEntity.ok(reservationService.findByClientUserId(clientUserId));
    }

    // lista reservas de un hotel
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<ReservationResponseDTO>> findByHotelId(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reservationService.findByHotelId(hotelId));
    }

    // lista reservas de una habitacion
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ReservationResponseDTO>> findByRoomId(@PathVariable Long roomId) {
        return ResponseEntity.ok(reservationService.findByRoomId(roomId));
    }

    // lista reservas por estado
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReservationResponseDTO>> findByStatus(@PathVariable String status) {
        return ResponseEntity.ok(reservationService.findByStatus(status));
    }

    // crea una nueva reserva
    @PostMapping
    public ResponseEntity<ReservationResponseDTO> create(@Valid @RequestBody ReservationRequestDTO request) {
        return ResponseEntity.ok(reservationService.create(request));
    }

    // cambia el estado de una reserva de forma general
    @PutMapping("/{id}/status")
    public ResponseEntity<ReservationResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateDTO request) {

        return ResponseEntity.ok(reservationService.updateStatus(id, request));
    }

    // confirma una reserva pendiente de pago
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponseDTO> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    // cancela una reserva
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    // elimina una reserva fisicamente, util solo para pruebas
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}