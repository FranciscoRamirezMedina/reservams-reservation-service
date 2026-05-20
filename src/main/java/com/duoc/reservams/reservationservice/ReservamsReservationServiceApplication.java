package com.duoc.reservams.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

//habilita los clientes feing para llamar a otros ms
@EnableFeignClients
@SpringBootApplication
public class ReservamsReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservamsReservationServiceApplication.class, args);
	}

}
