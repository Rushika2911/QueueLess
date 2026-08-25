# QueueLess - Intelligent Virtual Queue & Appointment Engine

QueueLess is a high-concurrency Spring Boot REST backend for remote queue token management, slot availability calculation, double-booking prevention, and service provider scheduling.

## Architectural Overview

```mermaid
graph TD
    Client[Mobile / Web Client / Swagger UI] --> SecurityFilter[JwtAuthenticationFilter]
    SecurityFilter --> AuthCtrl[AuthController]
    SecurityFilter --> ProviderCtrl[ProviderController / ServiceController / WorkingHourController]
    SecurityFilter --> AvailCtrl[AvailabilityController]
    SecurityFilter --> ApptCtrl[AppointmentController]
    SecurityFilter --> QueueCtrl[QueueController]

    AuthCtrl --> AuthService[AuthService]
    ProviderCtrl --> ProviderService[ProviderService & WorkingHourService]
    AvailCtrl --> AvailabilityService[AvailabilityService]
    ApptCtrl --> AppointmentService[AppointmentService]
    QueueCtrl --> QueueService[QueueService]

    NoShowSched[NoShowScheduler @Scheduled] --> QueueRepo[QueueEntryRepository]
    AppointmentService --> ApptRepo[AppointmentRepository]
    QueueService --> QueueRepo

    ApptRepo --> Postgres[(PostgreSQL Database)]
    QueueRepo --> Postgres
```

## Entity Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--o{ PROVIDERS : "owns"
    USERS ||--o{ APPOINTMENTS : "books"
    USERS ||--o{ QUEUE_ENTRIES : "joins"
    USERS ||--o{ AUDIT_LOGS : "logs"

    PROVIDERS ||--o{ SERVICES : "offers"
    PROVIDERS ||--o{ WORKING_HOURS : "operates"
    PROVIDERS ||--o{ APPOINTMENTS : "schedules"
    PROVIDERS ||--o{ QUEUE_ENTRIES : "manages"

    APPOINTMENTS }o--|| SERVICES : "for service"
    QUEUE_ENTRIES }o--o| APPOINTMENTS : "linked to"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar role
        boolean enabled
    }

    PROVIDERS {
        bigint id PK
        bigint user_id FK
        varchar specialization
        text description
        varchar status
    }

    SERVICES {
        bigint id PK
        bigint provider_id FK
        varchar name
        integer duration_minutes
        decimal price
        boolean active
    }

    WORKING_HOURS {
        bigint id PK
        bigint provider_id FK
        varchar day_of_week
        time start_time
        time end_time
    }

    APPOINTMENTS {
        bigint id PK
        bigint customer_id FK
        bigint provider_id FK
        bigint service_id FK
        date appointment_date
        time start_time
        time end_time
        varchar status
    }

    QUEUE_ENTRIES {
        bigint id PK
        bigint provider_id FK
        bigint customer_id FK
        bigint appointment_id FK
        date queue_date
        integer token_number
        integer position
        varchar status
        timestamp called_at
        timestamp completed_at
    }
```

## Default Seed Accounts

When launched locally, `DataInitializer` pre-populates the database with demo accounts:

| Role | Email | Password | Details |
| --- | --- | --- | --- |
| **ADMIN** | `admin@queueless.com` | `AdminPassword123` | Platform Administrator |
| **PROVIDER** | `sarah@clinic.com` | `DoctorPassword123` | Dr. Sarah Connor (Dental Care) |
| **PROVIDER** | `evans@clinic.com` | `DoctorPassword123` | Dr. James Evans (Cardiology) |
| **CUSTOMER** | `alice@example.com` | `CustomerPassword123` | Customer User |
| **CUSTOMER** | `bob@example.com` | `CustomerPassword123` | Customer User |

## Interactive API Documentation

Run the application and navigate to:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/api/health`

## Build & Test Commands

```bash
# Run unit, controller, and concurrency test suites
mvn clean test

# Run application locally
mvn spring-boot:run
```
