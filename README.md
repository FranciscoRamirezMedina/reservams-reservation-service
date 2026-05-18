\# ReservaMS - Reservation Service



\## Descripcion



Este microservicio administra las reservas del sistema ReservaMS.



Permite crear reservas, consultar reservas y cambiar sus estados.



\## Responsabilidades



\- Crear reservas.

\- Listar reservas.

\- Buscar reservas por cliente.

\- Buscar reservas por hotel.

\- Buscar reservas por habitacion.

\- Confirmar reservas.

\- Cancelar reservas.

\- Cambiar estado de una reserva.



\## Puerto



8086



\## Base de datos



reservams\_reservation\_db



\## Endpoints principales



\- GET /api/v1/reservations

\- GET /api/v1/reservations/{id}

\- GET /api/v1/reservations/client/{clientUserId}

\- GET /api/v1/reservations/hotel/{hotelId}

\- GET /api/v1/reservations/room/{roomId}

\- POST /api/v1/reservations

\- PUT /api/v1/reservations/{id}/confirm

\- PUT /api/v1/reservations/{id}/cancel



\## Ejecucion



1\. Crear la base de datos reservams\_reservation\_db.

2\. Ejecutar el script SQL ubicado en la carpeta database.

3\. Levantar Eureka Server.

4\. Ejecutar el reservation-service.

5\. Probar los endpoints desde Postman o desde el API Gateway.



