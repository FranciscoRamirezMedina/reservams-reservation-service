package com.duoc.reservams.reservationservice.client;

import com.duoc.reservams.reservationservice.dto.AvailabilityCheckRequestDTO;
import com.duoc.reservams.reservationservice.dto.AvailabilityCheckResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//cliente Feign para comunicarse con availability-service
@FeignClient(name = "reservams-availability-service")
public interface AvailabilityClient {

    //llama al endpoint que revisa si una habitacion esta disponible
    @PostMapping("/api/v1/availability/check")
    AvailabilityCheckResponseDTO checkAvailability(@RequestBody AvailabilityCheckRequestDTO request);
}
