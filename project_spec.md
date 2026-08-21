# QueueLess — Project Specification

## 1. Project Overview

QueueLess is a Spring Boot REST API for appointment scheduling and virtual queue management.

### Problem

Traditional service centers such as clinics, salons, repair centers, and consultation offices often require customers to wait physically, provide uncertain wait times, and manually manage appointments and no-shows.

QueueLess allows customers to book appointments or join a virtual queue remotely while providers manage availability, appointments, and customer flow.

### Primary goals

- Provide secure REST APIs for customers, providers, and admins.
- Prevent appointment double-booking.
- Manage real-time queue state transitions safely.
- Calculate basic estimated waiting time.
- Handle cancellations and no-shows through explicit business rules.
- Demonstrate production-style Spring Boot architecture and engineering practices.

### MVP boundary

Build a backend-first monolithic application. Do not add microservices, Kafka, Kubernetes, AI/ML, or a frontend in the MVP.

---

## 2. Technology Stack

### Required

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT authentication
- BCrypt password hashing
- Jakarta Bean Validation
- PostgreSQL
- Maven
- JUnit 5
- Mockito
- MockMvc
- Testcontainers
- OpenAPI / Swagger

### Optional infrastructure

- Docker
- Docker Compose

### Future extensions, not MVP

- Redis caching
- WebSockets for live queue updates
- Email/SMS notifications
- Google Calendar integration
- Provider analytics
- Historical wait-time prediction
- Multi-location businesses
- Rate limiting
- Distributed queue management

---

## 3. User Roles

### CUSTOMER

Can:

- register/login
- browse active providers
- view provider services
- view availability
- book appointments
- cancel eligible appointments
- view own appointments
- join a queue
- view queue position
- leave a waiting queue
- view own queue history

### PROVIDER

Can:

- manage own profile
- create/update/deactivate own services
- configure working hours
- view own appointments
- manage own queue
- call next customer
- mark customers completed
- mark customers as no-show
- view basic daily queue statistics

### ADMIN

Can:

- view users
- activate/deactivate providers
- manage providers
- view system-level information
- inspect audit logs

Role must be enforced server-side using Spring Security.

---

# 4. Domain Model

## 4.1 User

Table: `users`

Fields:

```text
id              BIGINT PK
name            VARCHAR
email           VARCHAR UNIQUE NOT NULL
passwordHash    VARCHAR NOT NULL
role            ENUM(CUSTOMER, PROVIDER, ADMIN)
enabled         BOOLEAN
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Rules:

- Email must be unique.
- Password is never returned through an API.
- Password must be BCrypt hashed.
- Disabled users cannot authenticate.
- A user has exactly one role in the MVP.

---

## 4.2 Provider

Table: `providers`

Fields:

```text
id              BIGINT PK
userId          BIGINT FK -> users.id UNIQUE
specialization  VARCHAR
description     TEXT
status          ENUM(ACTIVE, INACTIVE)
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Rules:

- Provider must reference a user with role PROVIDER.
- Only ACTIVE providers can accept new appointments/queue entries.
- Provider can manage only their own resources unless ADMIN.

---

## 4.3 Service

Table: `services`

Fields:

```text
id              BIGINT PK
providerId      BIGINT FK -> providers.id
name            VARCHAR
description     TEXT
durationMinutes INTEGER
price           DECIMAL
active          BOOLEAN
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Rules:

- Duration must be positive.
- Price must be non-negative.
- An inactive service cannot be booked.
- Provider can modify only their own services.

---

## 4.4 WorkingHour

Table: `working_hours`

Fields:

```text
id              BIGINT PK
providerId      BIGINT FK -> providers.id
dayOfWeek       ENUM(MONDAY ... SUNDAY)
startTime       TIME
endTime         TIME
```

Rules:

- `startTime < endTime`.
- Provider cannot have overlapping working-hour intervals for the same day.
- Appointment must fall completely inside working hours.

---

## 4.5 Appointment

Table: `appointments`

Fields:

```text
id              BIGINT PK
customerId      BIGINT FK -> users.id
providerId      BIGINT FK -> providers.id
serviceId       BIGINT FK -> services.id
appointmentDate DATE
startTime       TIME
endTime         TIME
status          ENUM(
                  BOOKED,
                  CHECKED_IN,
                  COMPLETED,
                  CANCELLED,
                  NO_SHOW
                )
createdAt       TIMESTAMP
updatedAt       TIMESTAMP
```

Rules:

1. Customer must exist and have role CUSTOMER.
2. Provider must be ACTIVE.
3. Service must be active and belong to the selected provider.
4. Appointment cannot be in the past.
5. Appointment must fall within provider working hours.
6. Appointment duration must match the service duration.
7. Customer cannot have overlapping appointments.
8. Provider cannot have overlapping appointments.
9. Appointment slot must be concurrency-safe.
10. Cancellation is allowed only until 30 minutes before start time.
11. Completed/cancelled/no-show appointments cannot be cancelled again.

---

## 4.6 QueueEntry

Table: `queue_entries`

Fields:

```text
id              BIGINT PK
providerId      BIGINT FK -> providers.id
customerId      BIGINT FK -> users.id
appointmentId   BIGINT FK -> appointments.id NULLABLE
queueDate       DATE
tokenNumber     INTEGER
position        INTEGER
status          ENUM(
                  WAITING,
                  CALLED,
                  SERVING,
                  COMPLETED,
                  NO_SHOW,
                  LEFT
                )
joinedAt        TIMESTAMP
calledAt        TIMESTAMP NULLABLE
completedAt     TIMESTAMP NULLABLE
updatedAt       TIMESTAMP
```

Rules:

- Queue entries are associated with a provider and date.
- Token numbers reset for each provider each day.
- Token numbers must be unique per provider/date.
- A customer cannot have multiple active WAITING/CALLED/SERVING entries for the same provider/date.
- Only one entry may be SERVING for a provider/date.
- Only WAITING entries can be called next.
- Only CALLED entries can transition to SERVING.
- Only SERVING entries can become COMPLETED.
- CALLED entries can become NO_SHOW.
- WAITING entries can become LEFT.
- A completed/no-show/left entry is terminal.
- Queue position is determined from active WAITING entries; do not rely blindly on a stored position.

---

## 4.7 AuditLog

Table: `audit_logs`

Fields:

```text
id              BIGINT PK
userId          BIGINT FK -> users.id NULLABLE
action          VARCHAR
entityType      VARCHAR
entityId        BIGINT
metadata        TEXT/JSON
createdAt       TIMESTAMP
```

Examples:

```text
REGISTER
LOGIN
BOOK_APPOINTMENT
CANCEL_APPOINTMENT
JOIN_QUEUE
LEAVE_QUEUE
CALL_NEXT
MARK_NO_SHOW
COMPLETE_APPOINTMENT
PROVIDER_STATUS_CHANGED
```

Audit logs are append-only.

---

# 5. Relationships

```text
User 1 ─── 0..1 Provider

Provider 1 ─── * Service
Provider 1 ─── * WorkingHour
Provider 1 ─── * Appointment
Provider 1 ─── * QueueEntry

Customer(User) 1 ─── * Appointment
Customer(User) 1 ─── * QueueEntry

Appointment 1 ─── 0..1 QueueEntry
```

Use JPA relationships carefully. Avoid exposing entities directly through controllers.

---

# 6. API Contract

Base URL:

```text
/api
```

Use JSON request/response bodies.

## Authentication

### POST `/api/auth/register`

Request:

```json
{
  "name": "Rushika",
  "email": "rushika@example.com",
  "password": "Password123",
  "role": "CUSTOMER"
}
```

Response: `201 Created`

Do not return password/passwordHash.

### POST `/api/auth/login`

Request:

```json
{
  "email": "rushika@example.com",
  "password": "Password123"
}
```

Response:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

# 7. Provider APIs

### GET `/api/providers`

Public.

Supports pagination/filtering:

```text
?page=0&size=10
?status=ACTIVE
?specialization=dentist
```

### GET `/api/providers/{id}`

Public.

### POST `/api/providers`

ADMIN only.

### PUT `/api/providers/{id}`

Provider owner or ADMIN.

### PATCH `/api/providers/{id}/status`

ADMIN only.

Request:

```json
{
  "status": "ACTIVE"
}
```

---

# 8. Service APIs

### GET `/api/providers/{providerId}/services`

Public; return active services for normal customers.

### POST `/api/providers/{providerId}/services`

PROVIDER owner only.

### PUT `/api/services/{id}`

PROVIDER owner only.

### DELETE `/api/services/{id}`

PROVIDER owner only.

Prefer soft deactivation (`active=false`) rather than physical deletion where historical appointments reference the service.

---

# 9. Working Hours APIs

### GET `/api/providers/{providerId}/working-hours`

Public.

### PUT `/api/providers/{providerId}/working-hours`

PROVIDER owner only.

Validate:

- valid day
- start < end
- no overlapping intervals

---

# 10. Availability API

### GET `/api/providers/{providerId}/availability`

Query parameters:

```text
date=2026-08-25
serviceId=4
```

Return available appointment slots.

Slot generation rules:

1. Load provider working hours for requested date.
2. Load service duration.
3. Generate slots according to service duration.
4. Remove slots overlapping existing appointments.
5. Return remaining slots.
6. Do not return slots in the past.
7. Do not return slots if provider/service is inactive.

Example response:

```json
{
  "date": "2026-08-25",
  "serviceId": 4,
  "slots": [
    {
      "startTime": "10:00",
      "endTime": "10:30",
      "available": true
    },
    {
      "startTime": "10:30",
      "endTime": "11:00",
      "available": true
    }
  ]
}
```

Availability is advisory. Final booking must re-check availability inside the transactional booking operation.

---

# 11. Appointment APIs

### POST `/api/appointments`

CUSTOMER only.

Request:

```json
{
  "providerId": 1,
  "serviceId": 4,
  "appointmentDate": "2026-08-25",
  "startTime": "11:30"
}
```

Response: `201 Created`.

### GET `/api/appointments`

Return appointments belonging to the authenticated customer/provider.

Support:

```text
?page=0
&size=10
&status=BOOKED
&date=2026-08-25
```

### GET `/api/appointments/{id}`

Owner/provider/admin access according to authorization rules.

### PATCH `/api/appointments/{id}/cancel`

Customer owner or authorized provider/admin.

Cancellation must enforce the 30-minute rule.

---

# 12. Queue APIs

## Join Queue

### POST `/api/queues/{providerId}/join`

CUSTOMER only.

Optional request:

```json
{
  "appointmentId": 42
}
```

Rules:

- Provider must be ACTIVE.
- Customer must not already have an active queue entry for the provider/date.
- If appointmentId is supplied, appointment must belong to customer/provider and be valid.
- Generate next token atomically.
- Return token and position.

Response:

```json
{
  "queueEntryId": 100,
  "tokenNumber": 24,
  "position": 4,
  "status": "WAITING",
  "estimatedWaitMinutes": 45
}
```

---

## Get Queue

### GET `/api/queues/{providerId}`

Provider owner/admin.

Optional:

```text
?date=2026-08-25
&status=WAITING
&page=0
&size=20
```

---

## My Position

### GET `/api/queues/my-position`

CUSTOMER only.

Return:

```json
{
  "tokenNumber": 24,
  "position": 4,
  "peopleAhead": 3,
  "estimatedWaitMinutes": 45,
  "status": "WAITING"
}
```

---

## Call Next

### POST `/api/queues/{providerId}/next`

PROVIDER owner only.

Rules:

- If a customer is currently SERVING, do not call another customer.
- Find earliest WAITING entry.
- Transition it to CALLED.
- Set `calledAt`.
- Operation must be transactional/concurrency-safe.

---

## Start Serving

### POST `/api/queues/{entryId}/serve`

PROVIDER owner only.

Rules:

- Entry must be CALLED.
- No other entry may be SERVING.
- Transition CALLED → SERVING.

---

## Complete

### PATCH `/api/queues/{entryId}/complete`

PROVIDER owner only.

Rules:

- Entry must be SERVING.
- Transition SERVING → COMPLETED.
- Set completedAt.

---

## No Show

### PATCH `/api/queues/{entryId}/no-show`

PROVIDER owner only.

Rules:

- Entry must be CALLED.
- Transition CALLED → NO_SHOW.
- Set completedAt or an appropriate terminal timestamp.

---

## Leave Queue

### POST `/api/queues/{entryId}/leave`

CUSTOMER owner only.

Rules:

- Only WAITING entries may leave.
- Transition WAITING → LEFT.

---

# 13. Queue State Machine

Valid transitions:

```text
WAITING
   ├──> CALLED
   └──> LEFT

CALLED
   ├──> SERVING
   └──> NO_SHOW

SERVING
   └──> COMPLETED

COMPLETED  → terminal
NO_SHOW    → terminal
LEFT       → terminal
```

Reject invalid transitions with HTTP `409 Conflict`.

---

# 14. Estimated Wait Time

MVP formula:

```text
peopleAhead × averageServiceDuration
```

For the initial implementation, use the selected service's duration as the estimate.

Example:

```text
3 people ahead
×
15 minute service
=
45 minute estimate
```

Return `estimatedWaitMinutes`.

Future version can use historical provider/service duration.

---

# 15. Authentication & Authorization

Use Spring Security with stateless JWT authentication.

Flow:

```text
Login
 ↓
Validate credentials
 ↓
Generate JWT
 ↓
Client sends:
Authorization: Bearer <token>
 ↓
JWT filter validates token
 ↓
SecurityContext populated
 ↓
Controller authorization
```

Use BCrypt for passwords.

Recommended route rules:

```text
/api/auth/**             PUBLIC

GET /api/providers/**    PUBLIC
GET /api/services/**     PUBLIC

/api/appointments/**     AUTHENTICATED
/api/queues/**           AUTHENTICATED

Provider management      PROVIDER or ADMIN
Admin management         ADMIN
```

Enforce resource ownership in the service layer even when role authorization passes.

---

# 16. DTO Design

Never expose JPA entities directly.

Use DTOs such as:

```text
RegisterRequest
LoginRequest
AuthResponse

ProviderResponse
UpdateProviderRequest

ServiceRequest
ServiceResponse

WorkingHourRequest
WorkingHourResponse

AvailabilityResponse
TimeSlotResponse

CreateAppointmentRequest
AppointmentResponse

JoinQueueRequest
QueueEntryResponse
QueuePositionResponse
```

Map Entity ↔ DTO in a dedicated mapper layer or explicit mapping classes.

---

# 17. Validation

Use Jakarta Validation.

Examples:

```text
@NotBlank
@Email
@Size
@NotNull
@Positive
@DecimalMin
```

Business validation belongs in services, not only DTO annotations.

---

# 18. Error Handling

Use:

```java
@RestControllerAdvice
```

Standard error response:

```json
{
  "timestamp": "2026-08-25T10:00:00Z",
  "status": 409,
  "error": "SLOT_UNAVAILABLE",
  "message": "The requested appointment slot is no longer available",
  "path": "/api/appointments"
}
```

Required custom exceptions:

```text
ResourceNotFoundException
DuplicateResourceException
SlotUnavailableException
InvalidAppointmentException
InvalidQueueTransitionException
QueueConflictException
UnauthorizedResourceAccessException
ProviderInactiveException
```

Handle validation failures separately.

---

# 19. HTTP Status Conventions

Use:

```text
200 OK       successful GET/update
201 Created  successful creation
204 No Content successful deletion where appropriate
400 Bad Request validation/business input error
401 Unauthorized missing/invalid authentication
403 Forbidden authenticated but insufficient permission
404 Not Found resource does not exist
409 Conflict state/concurrency/business conflict
500 Internal Server Error unexpected error
```

---

# 20. Concurrency Requirements

This is a core technical requirement.

## Appointment race condition

Multiple users may attempt to book the same slot simultaneously.

The system must guarantee that at most one valid appointment occupies the same provider/time interval.

Use a combination of:

- transactional service method
- database constraints where applicable
- appropriate JPA/PostgreSQL locking strategy
- final availability check inside the transaction

Do not rely only on frontend availability.

## Queue race condition

Multiple provider requests must not result in two simultaneous SERVING entries.

Queue operations that mutate state must be transactional and concurrency-safe.

## Token generation

Token numbers must be unique per:

```text
provider + queueDate
```

Token assignment must be safe under concurrent joins.

---

# 21. Transaction Boundaries

Use `@Transactional` for operations that modify multiple related records.

Required examples:

```text
bookAppointment()
joinQueue()
callNext()
startServing()
completeQueueEntry()
cancelAppointment()
```

Transactions should preserve consistency across validation, state changes, and persistence.

---

# 22. Scheduled Jobs

Use Spring Scheduling.

## No-show job

Run approximately every minute.

Find:

```text
status = CALLED
calledAt <= now - 5 minutes
```

Transition:

```text
CALLED → NO_SHOW
```

## Appointment reminder job

Optional MVP implementation.

Find appointments occurring within a configurable time window and create a log/audit event.

Do not integrate external email/SMS in MVP.

---

# 23. Pagination & Filtering

Use Spring Data `Pageable`.

Example:

```text
GET /api/providers?page=0&size=10&status=ACTIVE
GET /api/appointments?page=0&size=20&status=BOOKED
GET /api/queues/1?page=0&size=20&status=WAITING
```

Return page metadata where appropriate:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

# 24. Database Constraints

Use database-level integrity wherever practical.

Required:

- `users.email UNIQUE`
- foreign keys
- non-null constraints
- positive duration
- non-negative price
- unique provider/date/token for queue tokens
- appropriate indexes

Recommended indexes:

```text
users(email)

appointments(provider_id, appointment_date, start_time)
appointments(customer_id, appointment_date)

queue_entries(provider_id, queue_date, status)
queue_entries(provider_id, queue_date, token_number)

services(provider_id, active)
working_hours(provider_id, day_of_week)
```

---

# 25. Suggested Project Structure

```text
src/main/java/com/queueless
│
├── QueueLessApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── ProviderController.java
│   ├── ServiceController.java
│   ├── WorkingHourController.java
│   ├── AvailabilityController.java
│   ├── AppointmentController.java
│   └── QueueController.java
│
├── service/
│   ├── AuthService.java
│   ├── ProviderService.java
│   ├── ServiceManagementService.java
│   ├── WorkingHourService.java
│   ├── AvailabilityService.java
│   ├── AppointmentService.java
│   ├── QueueService.java
│   └── AuditService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── ProviderRepository.java
│   ├── ServiceRepository.java
│   ├── WorkingHourRepository.java
│   ├── AppointmentRepository.java
│   ├── QueueEntryRepository.java
│   └── AuditLogRepository.java
│
├── entity/
│   ├── User.java
│   ├── Provider.java
│   ├── Service.java
│   ├── WorkingHour.java
│   ├── Appointment.java
│   ├── QueueEntry.java
│   └── AuditLog.java
│
├── dto/
│   ├── auth/
│   ├── provider/
│   ├── service/
│   ├── appointment/
│   └── queue/
│
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── custom exceptions
│
├── mapper/
│
└── scheduler/
    ├── NoShowScheduler.java
    └── AppointmentReminderScheduler.java
```

Tests:

```text
src/test/java/com/queueless
├── controller/
├── service/
├── repository/
├── security/
└── integration/
```

---

# 26. Testing Requirements

## Unit tests

Use JUnit + Mockito.

Test:

- AuthService
- AppointmentService
- QueueService
- AvailabilityService
- ProviderService

## Controller tests

Use MockMvc.

Verify:

- authentication
- authorization
- validation
- response status
- JSON response
- error handling

## Integration tests

Use Spring Boot Test + Testcontainers PostgreSQL.

Test real persistence and critical workflows.

## Minimum important scenarios

### Authentication

- register valid user
- duplicate email
- login valid credentials
- login invalid credentials
- protected endpoint without JWT
- customer accessing provider-only endpoint
- provider accessing admin endpoint

### Appointment

- successful booking
- duplicate slot
- overlapping appointment
- provider inactive
- service inactive
- outside working hours
- past appointment
- valid cancellation
- late cancellation
- already cancelled appointment

### Queue

- successful join
- duplicate active queue entry
- token generation
- position calculation
- leave queue
- call next
- serve
- complete
- no-show
- invalid state transition
- empty queue
- two concurrent `call next` requests

### Concurrency

Create an integration test where multiple requests attempt to book the same slot and verify that exactly one succeeds.

---

# 27. API Documentation

Use Springdoc OpenAPI.

Document:

- endpoint description
- authentication requirements
- request DTOs
- response DTOs
- error responses
- examples

Swagger UI should expose the complete API.

---

# 28. Configuration

Use environment variables.

Example:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
```

Never commit real secrets.

Provide:

```text
.env.example
```

or documented environment configuration.

---

# 29. Docker

Provide optional Docker Compose for:

```text
PostgreSQL
QueueLess application
```

The application should still be runnable locally without Docker.

---

# 30. Seed Data

Provide development seed data for:

- one ADMIN
- two PROVIDERS
- several CUSTOMERS
- provider services
- working hours

Never hard-code production credentials.

Clearly document development credentials if seed data creates them.

---

# 31. README Requirements

README must contain:

1. Project overview
2. Problem statement
3. Features
4. Architecture
5. Tech stack
6. Database/ER diagram
7. API documentation link/instructions
8. Authentication flow
9. Queue algorithm
10. Concurrency strategy
11. Setup instructions
12. Environment variables
13. Running tests
14. Docker instructions
15. Future improvements

---

# 32. Architecture Diagram

Document this architecture:

```text
Client
  |
  | HTTP + JWT
  v
Controller
  |
  v
Service
  |
  +---- Business Rules
  +---- Transactions
  +---- Queue Logic
  |
  v
Repository
  |
  v
PostgreSQL

Spring Security
  |
  v
JWT Authentication Filter

Schedulers
  |
  +---- No-show processing
  +---- Reminder processing
```

---

# 33. Development Order

Implement in this order.

### Phase 1 — Project setup

- Create Spring Boot project
- Configure Maven
- Configure PostgreSQL
- Configure JPA
- Create base package structure
- Add Swagger
- Add global configuration

### Phase 2 — Database/domain

- User
- Provider
- Service
- WorkingHour
- Appointment
- QueueEntry
- AuditLog
- repositories
- migrations/schema

### Phase 3 — Security

- registration
- BCrypt
- login
- JWT generation
- JWT validation
- Spring Security configuration
- RBAC

### Phase 4 — Provider/service management

- provider APIs
- service APIs
- working-hour APIs
- validation

### Phase 5 — Appointment engine

- availability calculation
- booking
- cancellation
- transactions
- concurrency protection

### Phase 6 — Queue engine

- join
- token generation
- position calculation
- call next
- serve
- complete
- no-show
- leave
- state validation

### Phase 7 — Automation

- no-show scheduler
- reminder scheduler
- audit logging

### Phase 8 — Quality

- global exceptions
- pagination
- filtering
- database indexes
- integration tests
- concurrency tests

### Phase 9 — Documentation

- Swagger
- ER diagram
- architecture diagram
- README
- API examples

### Phase 10 — Optional infrastructure

- Docker
- Docker Compose

---

# 34. Definition of Done

QueueLess MVP is complete when:

- [ ] Application starts successfully.
- [ ] PostgreSQL connection works.
- [ ] Users can register/login.
- [ ] JWT authentication works.
- [ ] RBAC works for CUSTOMER/PROVIDER/ADMIN.
- [ ] Providers can manage services.
- [ ] Providers can configure working hours.
- [ ] Customers can view availability.
- [ ] Customers can book appointments.
- [ ] Double booking is prevented.
- [ ] Customers can cancel according to business rules.
- [ ] Customers can join queues.
- [ ] Token numbers are unique per provider/day.
- [ ] Queue position is calculated correctly.
- [ ] Providers can call the next customer.
- [ ] Only one customer can be SERVING.
- [ ] Providers can complete/no-show customers.
- [ ] Customers can leave waiting queues.
- [ ] Invalid queue state transitions are rejected.
- [ ] No-show scheduler works.
- [ ] Global exception handling works.
- [ ] Pagination/filtering works.
- [ ] Audit logs are generated.
- [ ] Unit tests exist.
- [ ] Controller tests exist.
- [ ] PostgreSQL integration tests exist.
- [ ] Concurrent booking test exists.
- [ ] Swagger documentation works.
- [ ] README contains setup and architecture documentation.
- [ ] No secrets are committed.

---

# 35. Engineering Principles

The implementation should prioritize:

1. Clean layered architecture.
2. Business logic in services, not controllers.
3. DTOs instead of exposing entities.
4. Explicit state transitions.
5. Database integrity in addition to application validation.
6. Transactional consistency.
7. Concurrency safety for booking and queue operations.
8. Meaningful HTTP status codes.
9. Centralized exception handling.
10. Automated testing of business-critical paths.
11. Secure authentication and authorization.
12. Simple architecture over unnecessary complexity.

## Most important implementation constraint

Do not add technologies merely to make the project look impressive.

Every technology must solve an actual problem in QueueLess.

The project should remain a **single Spring Boot monolith with PostgreSQL** for the MVP. Redis, WebSockets, Kafka, microservices, and cloud infrastructure are future extensions only.
