# HotelOS — Real-Time Hotel Operations Microservices

HotelOS is a simplified but working microservice project for the BTEC Programming Practice assignment. It models a hotel operations system with four operational services, a RabbitMQ message broker, a live WebSocket dashboard and a Swagger API gateway.

## Architecture

```text
HotelOS
├── gateway-service        :8090  Swagger + unified API entry point
├── reception-service      :8081  rooms, check-in, check-out, billing, guests
├── housekeeping-service   :8082  cleaning queue and cleaner workflow
├── room-service           :8083  food/drink orders and room charges
├── maintenance-service    :8084  maintenance issues and priority queue
├── dashboard-service      :8085  WebSocket dashboard + event history
└── rabbitmq               :5672  event broker, management UI on :15672
```

Services communicate through RabbitMQ events for hotel operations. The gateway is used for easy testing and Swagger documentation.

## Run the project

```bash
cd HotelOS
docker compose up --build
```

Open:

```text
Swagger UI: http://localhost:8090/swagger-ui.html
Dashboard:  http://localhost:8085
RabbitMQ:    http://localhost:15672
```

RabbitMQ login:

```text
username: guest
password: guest
```

Dashboard token:

```text
hotelos-demo-token
```

## Demo login

```http
POST /api/auth/login
```

Body:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response contains:

```json
{
  "token": "hotelos-demo-token"
}
```

## Final enhanced endpoints

### Gateway

```http
GET  /api/gateway/health
GET  /api/gateway/info
GET  /api/gateway/routes
```

### Auth

```http
POST /api/auth/login
GET  /api/auth/validate?token=hotelos-demo-token
```

### Reception

```http
GET   /api/reception/rooms
GET   /api/reception/rooms/{roomNumber}
GET   /api/reception/rooms/available?roomType=DOUBLE&floor=3
POST  /api/reception/check-in
POST  /api/reception/check-out/{roomNumber}
GET   /api/reception/guests
GET   /api/reception/guests/by-room/{roomNumber}
PATCH /api/reception/guests/{guestId}/archive
POST  /api/reception/bills/{roomNumber}/calculate
```

### Housekeeping

```http
GET   /api/housekeeping/queue
POST  /api/housekeeping/rooms/{roomNumber}/start
POST  /api/housekeeping/rooms/{roomNumber}/clean
PATCH /api/housekeeping/queue/{roomNumber}/cancel
GET   /api/housekeeping/cleaners
```

### Room Service

```http
GET   /api/room-service/orders
GET   /api/room-service/orders/{orderId}
POST  /api/room-service/orders
PATCH /api/room-service/orders/{orderId}/next
PATCH /api/room-service/orders/{orderId}/cancel
GET   /api/room-service/charges/{roomNumber}
```

### Maintenance

```http
GET   /api/maintenance/issues
GET   /api/maintenance/issues/{issueId}
POST  /api/maintenance/issues
GET   /api/maintenance/queue
POST  /api/maintenance/queue/process-next
PATCH /api/maintenance/issues/{issueId}/resolve
PATCH /api/maintenance/issues/{issueId}/cancel
GET   /api/maintenance/technicians
```

### Dashboard

```http
GET    /api/dashboard/snapshot
GET    /api/dashboard/events
DELETE /api/dashboard/events
WS     /ws/dashboard?token=hotelos-demo-token
```

### Demo scenarios

```http
POST /api/demo/reset
POST /api/demo/seed
POST /api/demo/run/ts-01
POST /api/demo/run/ts-02
POST /api/demo/run/ts-03
POST /api/demo/run/ts-04
POST /api/demo/run/ts-05
POST /api/demo/run/ts-06
POST /api/demo/run/ts-07
POST /api/demo/run/ts-08
```

## Quick test commands

```bash
curl http://localhost:8090/api/gateway/health
curl http://localhost:8090/api/gateway/routes
curl http://localhost:8090/api/reception/rooms
```

Check-in:

```bash
curl -X POST http://localhost:8090/api/reception/check-in \
  -H "Content-Type: application/json" \
  -d '{"guestName":"Diana Otayeva","roomType":"DOUBLE","nights":2,"preferredFloor":3,"proximityPreference":"LIFT"}'
```

Check-out:

```bash
curl -X POST http://localhost:8090/api/reception/check-out/204
```

Room service order:

```bash
curl -X POST http://localhost:8090/api/room-service/orders \
  -H "Content-Type: application/json" \
  -d '{"roomNumber":"301","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Sandwich","quantity":1,"unitPrice":8.00}]}'
```

Maintenance issue:

```bash
curl -X POST http://localhost:8090/api/maintenance/issues \
  -H "Content-Type: application/json" \
  -d '{"roomNumber":"115","description":"Broken shower","priority":"CRITICAL"}'
```

Dashboard snapshot:

```bash
curl http://localhost:8090/api/dashboard/snapshot
```

## Assignment test scenarios

The file `test-scenarios-gateway.http` contains all Swagger/gateway requests and the official TS-01 to TS-08 scenarios.

You can run demo scenarios directly from Swagger:

```text
POST /api/demo/run/ts-01
POST /api/demo/run/ts-02
POST /api/demo/run/ts-03
POST /api/demo/run/ts-04
POST /api/demo/run/ts-05
POST /api/demo/run/ts-06
POST /api/demo/run/ts-07
POST /api/demo/run/ts-08
```

## Data structures used

| Area | Data structure | Reason |
|---|---|---|
| Room inventory | `ConcurrentHashMap<String, Room>` | Fast lookup by room number |
| Room assignment | `ReentrantLock` + filtered lists | Prevents double booking during concurrent check-in |
| Guests | `ConcurrentHashMap<String, GuestStay>` | Fast active stay lookup by room |
| Housekeeping queue | `ConcurrentLinkedQueue<CleaningTask>` | FIFO cleaning tasks |
| Room service orders | `ConcurrentLinkedQueue<RoomOrder>` + map | Queue workflow + fast order lookup |
| Maintenance queue | `PriorityQueue<MaintenanceIssue>` | Critical/High/Normal/Low priority ordering |
| Dashboard events | `CopyOnWriteArrayList` | Thread-safe live event history |

## Events

| Event | Publisher | Subscriber |
|---|---|---|
| `room.vacated` | Reception | Housekeeping, Dashboard |
| `room.status.changed` | Reception, Housekeeping, Maintenance | Reception, Dashboard |
| `room.service.order.updated` | Room Service | Dashboard |
| `room.service.charge` | Room Service | Reception |
| `maintenance.issue.updated` | Maintenance | Dashboard |

## Git log sample

```text
f70a1e9 add gateway swagger and route proxy
c42b8aa add dashboard event history endpoints
b18ff19 add demo scenario endpoints
9c71f4a add reception guest archive and billing endpoints
813fe32 add housekeeping cancel and cleaner endpoints
70e19d0 add room service order lookup cancel and charges
5df40f2 add maintenance issue lookup cancel and technician endpoints
4572baa add dashboard snapshot aggregation
3092a6d add auth token validation
1be92ca polish README and HTTP test scenarios
```
