# File Storage Service

A distributed file upload and storage service built with Spring Boot, PostgreSQL, MinIO, and Docker.

## Architecture

```
Client → Spring Boot REST API → JWT Authentication → File Service → MinIO (Object Storage)
                                                                  → PostgreSQL (Metadata)
```

## Tech Stack

- **Java 21** + **Spring Boot 3.x**
- **Spring Security** + **JWT** for authentication
- **Spring Data JPA** + **PostgreSQL** for metadata
- **MinIO** for S3-compatible object storage
- **Docker** + **Docker Compose** for containerization

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive JWT |

### Files (require `Authorization: Bearer <token>` header)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/upload` | Upload a file (multipart/form-data) |
| GET | `/files` | List authenticated user's files |
| GET | `/files/{id}/download` | Get temporary signed download URL (5 min expiry) |
| DELETE | `/files/{id}` | Delete a file |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)

### Run with Docker Compose

```bash
docker-compose up -d
```

This starts PostgreSQL, MinIO, and the Spring Boot app.

### Run locally

```bash
# Start dependencies
docker-compose up -d postgres minio

# Run the app
./mvnw spring-boot:run
```

## Key Concepts

- **Object storage over filesystem**: Files stored in MinIO (S3-compatible), not on local disk
- **Metadata separation**: File metadata in PostgreSQL, actual files in MinIO
- **Signed URLs**: Temporary download URLs instead of exposing objects publicly
- **Ownership verification**: Users can only access their own files
- **Unique naming**: Files stored with UUID prefix to prevent collisions

## Project Structure

```
src/main/java/com/project/
├── config/        # Security, JWT filter, MinIO config
├── controller/    # REST endpoints
├── service/       # Business logic
├── repository/    # Data access
├── model/         # JPA entities
├── dto/           # Request/response objects
├── security/      # JWT utilities, user details
└── exception/     # Global error handling
```
