package com.duoc.reservams.reservationservice.producer;

import com.duoc.reservams.reservationservice.event.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// clase encargada de publicar eventos de reservas en Kafka
@Component
public class ReservationEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(ReservationEventProducer.class);

    private static final String RESERVATION_CREATED_TOPIC = "reservation-created-topic";

    private final KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate;

    public ReservationEventProducer(KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // publica un evento cuando se crea una reserva
    public void publishReservationCreatedEvent(ReservationCreatedEvent event) {
        logger.info("Publishing reservation created event. reservationId={}", event.getReservationId());

        kafkaTemplate.send(
                RESERVATION_CREATED_TOPIC,
                String.valueOf(event.getReservationId()),
                event
        );

        logger.info("Reservation created event published. reservationId={}", event.getReservationId());
    }
}