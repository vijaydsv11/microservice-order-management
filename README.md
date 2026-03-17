# Order Management Microservices

A comprehensive Spring Boot order management microservices platform demonstrating modern microservices architecture with API Gateway, service discovery, authentication, inter-service communication, event streaming, and distributed saga pattern.

## 🚀 Quick Start

**For detailed step-by-step startup instructions, see:**

- 📖 [QUICK-START.md](QUICK-START.md) — Quick reference (30 seconds)
- 📘 [STARTUP-GUIDE.md](STARTUP-GUIDE.md) — Comprehensive setup with examples

## 🔗 API Documentation — OpenAPI/Swagger

Each service exposes interactive **Swagger UI** for exploring and testing APIs:

| Service           | Swagger UI                            | Port |
| ----------------- | ------------------------------------- | ---- |
| API Gateway       | http://localhost:8080/swagger-ui.html | 8080 |
| Auth Service      | http://localhost:8081/swagger-ui.html | 8081 |
| User Service      | http://localhost:8082/swagger-ui.html | 8082 |
| Order Service     | http://localhost:8083/swagger-ui.html | 8083 |
| Inventory Service | http://localhost:8084/swagger-ui.html | 8084 |
| Payment Service   | http://localhost:8086/swagger-ui.html | 8086 |

## Architecture Overview

```
API Gateway (Port 8080)
        ↓
    [Routes]
        ↓
    ├─ Auth Service (Port 8081) ─→ MySQL (3307)
    ├─ User Service (Port 8082) ─→ MySQL (3308)
    └─ Order Service (Port 8083) ─→ MySQL (3309)
            ↓
        [Saga Pattern]
            ↓
    ├─ Inventory Service (Port 8084) ─→ MySQL (3310)
    └─ Payment Service (Port 8086) ─→ MySQL (3311)

Service Discovery: Eureka Server (Port 8761)
Centralized Config: Config Server (Port 8888)
Message Broker: Kafka (Port 9092) + Kafka UI (8085)
Distributed Tracing: Zipkin (Port 9411)
```

## Services

### 1. **Eureka Server** (Service Discovery)

- Port: 8761
- Enables service-to-service discovery
- Dashboard: http://localhost:8761

### 2. **API Gateway** (Spring Cloud Gateway)

- Port: 8080
- Routes requests to appropriate services
- JWT token validation
- Routes:
  - `/auth/**` → Auth Service
  - `/users/**` → User Service
  - `/orders/**` → Order Service

### 3. **Config Server** (Spring Cloud Config)

- Port: 8888
- Git-backed centralized configuration for all services
- Default sample repo: `https://github.com/spring-cloud-samples/config-repo`
- Override repository with env var: `CONFIG_REPO_URI`

### 4. **Auth Service** (JWT Authentication)

- Port: 8081
- Database: auth_db (Port 3307)
- Endpoints:
  - `POST /auth/register` - Register new user
  - `POST /auth/login` - Login and get JWT token
- Features:
  - User registration with email validation
  - JWT token generation
  - Password encryption with BCrypt

### 5. **User Service** (User Management)

- Port: 8082
- Database: user_db (Port 3308)
- Endpoints:
  - `GET /users` - Get all users
  - `GET /users/{id}` - Get user by ID
  - `GET /users/email/{email}` - Get user by email
  - `POST /users` - Create new user
  - `PUT /users/{id}` - Update user
  - `DELETE /users/{id}` - Delete user

### 6. **Order Service** (Order Management with Saga Pattern)

- Port: 8083
- Database: order_db (Port 3309)
- Endpoints:
  - `GET /orders` - Get all orders
  - `GET /orders/{id}` - Get order by ID
  - `GET /orders/user/{userId}` - Get orders by user
  - `POST /orders` - Create new order
  - `PUT /orders/{id}/status` - Update order status
  - `DELETE /orders/{id}` - Delete order
- Features:
  - Feign client for User Service with Resilience4j circuit breaker & retry
  - Kafka event publishing (OrderCreatedEvent)
  - **Distributed Saga Pattern**: Orchestrates multi-step order processing
  - Order status: PENDING → CONFIRMED or CANCELLED

### 7. **Inventory Service** (Inventory Management - Saga Step 2)

- Port: 8084
- Database: inventory_db (Port 3310)
- Endpoints:
  - `GET /inventory` - Get all inventory items
  - `GET /inventory/{id}` - Get inventory by ID
  - `GET /inventory/product/{productName}` - Get inventory by product name
  - `GET /inventory/low-stock/{threshold}` - Get low stock items
  - `POST /inventory/add?productName=X&quantity=Y` - Add inventory
  - `POST /inventory/reserve?productName=X&quantity=Y` - Reserve inventory
  - `POST /inventory/release?productName=X&quantity=Y` - Release reservation
- Features:
  - **Kafka Event Consumer**: Listens for OrderCreatedEvent (Saga Step 2)
  - Spring Cloud Stream functional beans for event processing
  - Publishes InventoryReservedEvent or InventoryReservationFailedEvent
  - **Saga Compensation**: Releases inventory on payment failure
  - Quantity tracking: Available, Reserved, Allocated

### 8. **Payment Service** (Payment Processing - Saga Step 3)

- Port: 8086
- Database: payment_db (Port 3311)
- Endpoints:
  - `GET /payments` - Get all payments
  - `GET /payments/{id}` - Get payment by ID
  - `GET /payments/order/{orderId}` - Get payment for order
- Features:
  - **Kafka Event Consumer**: Listens for InventoryReservedEvent (Saga Step 3)
  - Spring Cloud Stream functional beans for event processing
  - `@Transactional` payment processing with atomicity guarantees
  - Idempotency checks to prevent duplicate charges
  - Publishes PaymentCompletedEvent or PaymentFailedEvent
  - Payment status: PENDING → COMPLETED or FAILED
  - Pluggable payment gateway integration (stub for extension)

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- MySQL 8.0
- Kafka 7.5.0

## 📖 Getting Started

### ⭐ Quick Start (Recommended)

For a fast walkthrough with all commands and examples:

- **[QUICK-START.md](QUICK-START.md)** — 2-minute quick reference

For detailed step-by-step instructions:

- **[STARTUP-GUIDE.md](STARTUP-GUIDE.md)** — Complete setup with troubleshooting

### Manual Setup (Detailed)

#### 1. Start Infrastructure (Docker Compose)

```bash
cd microservices-platform
docker-compose up -d
```

This starts:

- **Databases**: MySQL × 5 (auth_db, user_db, order_db, inventory_db, payment_db)
- **Message Broker**: Zookeeper + Kafka
- **Monitoring**: Kafka UI (port 8085), Zipkin (port 9411)
- **Networks**: Service-to-service communication

#### 2. Build All Services

```bash
mvn clean install -DskipTests
```

**Output should show:**

```
[INFO] Reactor Build Order:
...
[INFO] Building Payment Service 1.0.0 [9/9]
...
[INFO] BUILD SUCCESS
```

#### 2.1 Fast Build (Development)

To rebuild only changed services:

```bash
# Rebuild order-service and dependencies
mvn install -DskipTests -am -pl order-service

# Rebuild multiple services
mvn install -DskipTests -pl order-service,payment-service -am
```

#### 3. Run Services (in startup order)

**📖 For detailed instructions, see [STARTUP-GUIDE.md](STARTUP-GUIDE.md)**

Start these services in **separate terminals** in this exact order:

1. **Eureka Server** (8761): `cd eureka-server && mvn spring-boot:run`
2. **Config Server** (8888): `cd config-server && mvn spring-boot:run`
3. **Auth Service** (8081): `cd auth-service && mvn spring-boot:run`
4. **User Service** (8082): `cd user-service && mvn spring-boot:run`
5. **Order Service** (8083): `cd order-service && mvn spring-boot:run`
6. **Inventory Service** (8084): `cd inventory-service && mvn spring-boot:run`
7. **Payment Service** (8086): `cd payment-service && mvn spring-boot:run`
8. **API Gateway** (8080): `cd api-gateway && mvn spring-boot:run` ← **START LAST**

#### 4. Verify Services Running

**Check Eureka Dashboard:** http://localhost:8761

Should see all 8 services registered:

- EUREKA-SERVER
- CONFIG-SERVER
- AUTHSERVICE
- USERSERVICE
- APIGATEWAY
- ORDERSERVICE
- INVENTORYSERVICE
- PAYMENTSERVICE

### 5. Test the APIs

**Swagger UI Available at Each Service:**

````

#### Terminal 6: Inventory Service (Kafka Consumer)

```bash
cd inventory-service
mvn spring-boot:run
````

#### Terminal 7: API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

### 5. Test the APIs

**Interactive Swagger/OpenAPI Documentation:**

- Auth Service: http://localhost:8081/swagger-ui.html
- User Service: http://localhost:8082/swagger-ui.html
- Order Service: http://localhost:8083/swagger-ui.html
- Inventory Service: http://localhost:8084/swagger-ui.html
- Payment Service: http://localhost:8086/swagger-ui.html
- API Gateway: http://localhost:8080/swagger-ui.html

**Using Swagger UI:**

1. Open any service's Swagger UI in browser
2. Find endpoint you want to test
3. Click "Try it out"
4. Enter parameters/body
5. Click "Execute"
6. View response

## API Usage Examples

### 1. Register User (via curl)

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "Secure123!"
  }'
```

### 2. Create Order (requires JWT token)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99,
    "totalAmount": 1999.98
  }'
```

### 3. Get Order Status (watch saga progression)

```bash
curl -X GET http://localhost:8080/orders/100 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Initial response:** `"status": "PENDING"`  
**After ~2-3 seconds:** `"status": "CONFIRMED"` (saga completed)

### 4. Monitor Events via Kafka UI

**Kafka UI:** http://localhost:8085

Watch topics:

- `order-created-topic` — order creation event
- `inventory-reserved-topic` — inventory step success
- `payment-completed-topic` — payment step success
- `payment-failed-topic` — payment step failure (triggers compensation)

## Distributed Saga Flow

**Order Creation → Inventory Reservation → Payment Processing:**

```
1. User creates order
   ↓
2. OrderService: Order saved as PENDING, OrderCreatedEvent published to Kafka
   ↓
3. InventoryService: Reserves stock from order
   - ✅ Success → InventoryReservedEvent published
   - ❌ Failure → InventoryReservationFailedEvent → OrderService cancels order
   ↓
4. PaymentService: Processes payment from InventoryReservedEvent
   - ✅ Success → PaymentCompletedEvent → OrderService confirms order (CONFIRMED)
   - ❌ Failure → PaymentFailedEvent → InventoryService releases stock → OrderService cancels (CANCELLED)
```

**Total flow time:** ~2-3 seconds end-to-end

## Database Schemas

### Auth Service Database

- `orders` table with userId, productName, quantity, price, totalAmount, status, createdAt, updatedAt

### Inventory Service Database

- `inventory` table with productName, quantityAvailable, quantityReserved, quantityAllocated

## Configuration

### JWT Secret

Located in:

- `auth-service`: `JwtProvider.java`
- `api-gateway`: `JwtAuthenticationFilter.java`

**Update for production:**

```bash
# Change this value and keep it secure
JWT_SECRET = "your-secret-key-change-this-in-production"
```

### Database Connections

Update credentials in each service's `application.yml`:

```yaml
spring:
  datasource:
    username: root
    password: root
```

### Kafka Configuration

Order Service publishes to topic: `order-created-topic`

## Kafka Topics

### Order Created Topic

```
topic: order-created-topic
key: orderId
value: OrderCreatedEvent (JSON)
  {
    "orderId": 1,
    "userId": 1,
    "productName": "Product Name",
    "quantity": 1,
    "totalAmount": 999.99,
    "userEmail": "user@example.com",
    "timestamp": 1234567890
  }
```

## Distributed Tracing (Zipkin)

This platform is configured with Micrometer Tracing and Zipkin export for all services.

- Zipkin UI: `http://localhost:9411`
- Export endpoint used by services: `${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}`
- Sampling probability (local dev): `1.0`

To view traces:

1. Start infrastructure with `docker-compose up -d`.
2. Start services and trigger a request through API Gateway.
3. Open Zipkin UI and search for traces by service name (e.g. `api-gateway`, `order-service`).

## Event-Driven Architecture (Kafka Integration)

### Order Flow with Kafka

1. **User creates order** via `POST /orders` on Order Service
2. **Order Service**:
   - Saves order to database
   - Publishes `OrderCreatedEvent` to Kafka topic "order-created-topic"
   - Responds with order details
3. **Kafka** retains the event for 7 days (default)
4. **Inventory Service Consumer**:
   - Listens to "order-created-topic" via Spring Cloud Stream consumer group
   - Receives `OrderCreatedEvent`
   - Allocates inventory quantity based on order
   - Updates inventory in inventory_db

### Spring Cloud Stream Configuration

**Order Service (Producer):**

```yaml
spring:
  cloud:
    stream:
      bindings:
        order-created-out-0:
          destination: order-created-topic
          content-type: application/json
```

**Inventory Service (Consumer):**

```yaml
spring:
  cloud:
    stream:
      function:
        definition: orderCreatedConsumer
      bindings:
        orderCreatedConsumer-in-0:
          destination: order-created-topic
          group: inventory-service-group
```

### Scaling Consumers

Multiple Inventory Service instances can process events concurrently:

```bash
# Terminal 1
cd inventory-service && java -Dserver.port=8084 -jar target/inventory-service-1.0.0.jar

# Terminal 2
cd inventory-service && java -Dserver.port=8085 -jar target/inventory-service-1.0.0.jar

# Both instances join the same consumer group and Kafka load-balances partitions
```

## Common Issues

### 1. Connection Refused to MySQL

```
Solution: Ensure Docker containers are running
docker-compose ps
```

### 2. Cannot register with Eureka

```
Solution: Ensure Eureka Server is running first on port 8761
Check: curl http://localhost:8761
```

### 3. Feign Client Connection Error

```
Solution: Ensure User Service is running and registered with Eureka
Check Eureka dashboard: http://localhost:8761
```

### 4. Kafka Connection Error

```
Solution: Ensure Kafka and Zookeeper are running
docker-compose ps
```

## Directory Structure

```
microservices-platform/
├── eureka-server/                 # Service Discovery
│   ├── src/main/java/...
│   └── pom.xml
├── api-gateway/                   # API Gateway
│   ├── src/main/java/...
│   └── pom.xml
├── auth-service/                  # Authentication
│   ├── src/main/java/...
│   └── pom.xml
├── user-service/                  # User Management
│   ├── src/main/java/...
│   └── pom.xml
├── order-service/                 # Order Management
│   ├── src/main/java/...
│   └── pom.xml
├── docker-compose.yml             # Infrastructure
└── pom.xml                        # Parent POM
```

## Technologies Used

- **Framework**: Spring Boot 3.2.0
- **Cloud**: Spring Cloud 2023.0.0 (Gateway, Eureka, OpenFeign)
- **Authentication**: JWT (JJWT 0.12.3)
- **Database**: MySQL 8.0
- **Messaging**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Build**: Maven
- **Language**: Java 17

## Next Steps / Enhancements

1. **Inventory Service** - Consume Kafka events to update inventory
2. **Notification Service** - Send emails/notifications on order creation
3. **Resilience4j** - Add circuit breaker for Feign calls
4. **Spring Config Server** - Centralized configuration management
5. **ELK Stack** - Distributed logging and monitoring
6. **Prometheus & Grafana** - Metrics and monitoring
7. **Docker Images** - Create Dockerfiles for each service
8. **Kubernetes** - Deploy on K8s with Helm charts
9. **Integration Tests** - Add comprehensive test cases
10. **API Documentation** - Swagger/OpenAPI integration

## Troubleshooting & Tips

### Reset Docker Containers

```bash
docker-compose down -v
docker-compose up -d
```

### View Docker Logs

```bash
docker-compose logs -f kafka
docker-compose logs -f zookeeper
```

### Check Service Health

```bash
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Order Service
```

### Access MySQL Databases

```bash
mysql -h localhost -P 3307 -u root -p  # Auth DB
mysql -h localhost -P 3308 -u root -p  # User DB
mysql -h localhost -P 3309 -u root -p  # Order DB
```

## License

MIT License

## Support

For issues and questions, please check the documentation or create an issue in the repository.
