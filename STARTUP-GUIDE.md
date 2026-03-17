# Microservices Platform — Complete Setup & Startup Guide

This guide walks through starting the entire microservices platform step-by-step with all infrastructure, services, and monitoring tools.

## Prerequisites

- **Java 17+** installed
- **Maven 3.6+** installed
- **Docker** & **Docker Compose** installed (for MySQL, Kafka, Zookeeper, etc.)
- **Postman** or **curl** for API testing (optional but recommended)

### Verify Prerequisites

```powershell
# Check Java version (should be 17+)
java -version

# Check Maven version
mvn -version

# Check Docker
docker --version
docker-compose --version
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│         Client Applications / Postman           │
└────────────────────┬────────────────────────────┘
                     │ HTTP/REST (Port 8080)
                     ▼
        ┌────────────────────────────┐
        │   API GATEWAY (8080)       │  ← JWT Route Security
        │ Routes: /auth, /users,     │
        │         /orders, /inventory│
        └────────────┬───────────────┘
                     │
    ┌────────────────┼────────────────┬──────────────┐
    │                │                │              │
    ▼                ▼                ▼              ▼
  (8081)          (8082)           (8083)        (8084)
┌────────┐    ┌─────────┐    ┌──────────┐    ┌──────────────┐
│ AUTH   │    │  USER   │    │  ORDER   │    │ INVENTORY    │
│SERVICE │    │SERVICE  │    │ SERVICE  │    │ SERVICE      │
│ +JWT   │    │ +CRUD   │    │+Saga+Fei│    │ +Saga        │
└───┬────┘    └─────────┘    │gn+Kafka │    │ +Kafka       │
    │                          └────┬────┘    └──────┬───────┘
    │                               │               │
    │          ┌────────────────────┴───────────┐   │
    │          │                                │   │
    │          ▼                                ▼   ▼
    │      ┌──────────────┐          ┌──────────────────┐
    │      │   PAYMENT    │          │  KAFKA CLUSTER   │
    │      │   SERVICE    │          │ (Saga Events)    │
    │      │  (8086)      │          │  Port 9092       │
    │      │  +Saga+Kafka │          │  Kafka UI: 8085  │
    │      └──────────────┘          └──────────────────┘
    │
    └──────────┬─────────────────────────────────────────┐
               │                                          │
    ┌──────────▼──────┐  ┌──────────────────┐  ┌─────────▼────────┐
    │   DATABASES     │  │  INFRASTRUCTURE  │  │  DISCOVERY &     │
    │                 │  │                  │  │  CONFIG          │
    │ Auth_db (3307)  │  │ Zookeeper (2181) │  │ Eureka (8761)    │
    │ User_db (3308)  │  │ Kafka Broker     │  │ Config Server    │
    │ Order_db (3309) │  │ Kafka UI (8085)  │  │ (8888)           │
    │ Inventory (3310)│  │ Zipkin (9411)    │  │ Zipkin (9411)    │
    │ Payment_db(3311)│  │ (Tracing)        │  │                  │
    └─────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Step 1: Start Infrastructure (Docker Compose)

### 1a. Navigate to workspace root

```powershell
cd e:\microsevices\microservices-platform
```

### 1b. Start all infrastructure services

```powershell
docker-compose up -d
```

This starts:

- ✅ **MySQL** (5 databases): auth_db, user_db, order_db, inventory_db, payment_db
- ✅ **Zookeeper** (2181) — Kafka coordinator
- ✅ **Kafka** (9092) — Message broker
- ✅ **Kafka UI** (8085) — Kafka monitoring dashboard
- ✅ **Zipkin** (9411) — Distributed tracing

### 1c. Verify infrastructure is running

```powershell
# List running containers
docker-compose ps

# Expected output:
# NAME                COMMAND                  SERVICE           STATUS         PORTS
# auth-db             "docker-entrypoint..."   auth-db           Up 2 minutes   0.0.0.0:3307...
# user-db             "docker-entrypoint..."   user-db           Up 2 minutes   0.0.0.0:3308...
# order-db            "docker-entrypoint..."   order-db          Up 2 minutes   0.0.0.0:3309...
# inventory-db        "docker-entrypoint..."   inventory-db      Up 2 minutes   0.0.0.0:3310...
# payment-db          "docker-entrypoint..."   payment-db        Up 2 minutes   0.0.0.0:3311...
# zookeeper           "docker-entrypoint..."   zookeeper         Up 2 minutes   0.0.0.0:2181...
# kafka               "docker-entrypoint..."   kafka             Up 2 minutes   0.0.0.0:9092...
# kafka-ui            "docker-entrypoint..."   kafka-ui          Up 2 minutes   0.0.0.0:8085...
# zipkin              "docker-entrypoint..."   zipkin            Up 2 minutes   0.0.0.0:9411...
```

### 1d. Health checks (optional)

```powershell
# Test Kafka connectivity
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Test MySQL — auth-db
docker exec auth-db mysql -u root -proot -e "SELECT 1;"
```

---

## Step 2: Build All Microservices

### 2a. Full build (recommended — first time)

```powershell
# From workspace root: e:\microsevices\microservices-platform
mvn clean install -DskipTests -T 1C
```

**What this does:**

- Cleans previous builds
- Compiles all 9 modules
- Runs unit tests (skipped with -DskipTests)
- Creates executable JARs in `target/` directories
- Downloads dependencies

**Expected output (end):**

```
[INFO] Reactor Build Order:
[INFO] Microservices Platform 1.0.0 [1/9]
[INFO] Eureka Server 1.0.0 [2/9]
[INFO] Config Server 1.0.0 [3/9]
[INFO] API Gateway 1.0.0 [4/9]
[INFO] Auth Service 1.0.0 [5/9]
[INFO] User Service 1.0.0 [6/9]
[INFO] Order Service 1.0.0 [7/9]
[INFO] Inventory Service 1.0.0 [8/9]
[INFO] Payment Service 1.0.0 [9/9]
[INFO] BUILD SUCCESS
```

### 2b. Fast rebuild (after code changes)

```powershell
# Only build specific service + dependencies
mvn install -DskipTests -am -pl order-service

# Or rebuild multiple modules
mvn install -DskipTests -pl auth-service,user-service,order-service -am

# -am = also make (build dependencies)
# -pl = projects list
```

---

## Step 3: Start Services in Startup Order

**⚠️ IMPORTANT: Start services in this exact order for proper initialization**

### 3a. Terminal 1 — Eureka Server (Service Discovery)

```powershell
cd e:\microsevices\microservices-platform\eureka-server
mvn spring-boot:run
```

**Wait for startup message:**

```
[INFO] Tomcat started on port(s): 8761 (http)
[INFO] Started EurekaServerApplication in ... seconds
```

**Dashboard:** http://localhost:8761

---

### 3b. Terminal 2 — Config Server (Centralized Configuration)

```powershell
cd e:\microsevices\microservices-platform\config-server
mvn spring-boot:run
```

**Wait for startup message:**

```
[INFO] Tomcat started on port(s): 8888 (http)
[INFO] Started ConfigServerApplication in ... seconds
```

**Health check:**

```powershell
curl http://localhost:8888/actuator/health
```

---

### 3c. Terminal 3 — Auth Service

```powershell
cd e:\microsevices\microservices-platform\auth-service
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8081 (http)
[INFO] Started AuthServiceApplication in ... seconds
[INFO] ... Registered with Eureka as AUTHSERVICE
```

**Available at:** http://localhost:8081

---

### 3d. Terminal 4 — User Service

```powershell
cd e:\microsevices\microservices-platform\user-service
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8082 (http)
[INFO] Started UserServiceApplication in ... seconds
[INFO] ... Registered with Eureka as USERSERVICE
```

**Available at:** http://localhost:8082

---

### 3e. Terminal 5 — Order Service

```powershell
cd e:\microsevices\microservices-platform\order-service
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8083 (http)
[INFO] Started OrderServiceApplication in ... seconds
[INFO] ... Registered with Eureka as ORDERSERVICE
[INFO] Subscribed to channel 'order-created-out-0' (channels: [order-created-out-0])
```

**Available at:** http://localhost:8083

---

### 3f. Terminal 6 — Inventory Service

```powershell
cd e:\microsevices\microservices-platform\inventory-service
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8084 (http)
[INFO] Started InventoryServiceApplication in ... seconds
[INFO] ... Registered with Eureka as INVENTORYSERVICE
[INFO] Subscribed to channel 'orderCreatedConsumer-in-0' (channels: [orderCreatedConsumer-in-0])
```

**Available at:** http://localhost:8084

---

### 3g. Terminal 7 — Payment Service

```powershell
cd e:\microsevices\microservices-platform\payment-service
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8086 (http)
[INFO] Started PaymentServiceApplication in ... seconds
[INFO] ... Registered with Eureka as PAYMENTSERVICE
[INFO] Subscribed to channel 'inventoryReservedConsumer-in-0' (channels: [inventoryReservedConsumer-in-0])
```

**Available at:** http://localhost:8086

---

### 3h. Terminal 8 — API Gateway (Main Entry Point)

```powershell
cd e:\microsevices\microservices-platform\api-gateway
mvn spring-boot:run
```

**Wait for:**

```
[INFO] Tomcat started on port(s): 8080 (http)
[INFO] Started ApiGatewayApplication in ... seconds
[INFO] ... Registered with Eureka as APIGATEWAY
```

**Available at:** http://localhost:8080

---

## Step 4: Verify All Services Are Running

### 4a. Eureka Dashboard — Verify Service Registration

**Eureka UI:** http://localhost:8761

You should see **8 registered instances**:

- `EUREKA-SERVER` (8761)
- `CONFIG-SERVER` (8888)
- `AUTHSERVICE` (8081)
- `USERSERVICE` (8082)
- `APIGATEWAY` (8080)
- `ORDERSERVICE` (8083)
- `INVENTORYSERVICE` (8084)
- `PAYMENTSERVICE` (8086)

### 4b. Quick Health Checks

```powershell
# Check all service health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8086/actuator/health
```

Expected response: `{"status":"UP"}`

---

## Step 5: Access API Documentation (OpenAPI/Swagger)

Each service now includes **interactive Swagger UI** for exploring APIs:

### All Services — Swagger UI Locations

| Service               | Port | Swagger UI                            | OpenAPI JSON                      |
| --------------------- | ---- | ------------------------------------- | --------------------------------- |
| **API Gateway**       | 8080 | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| **Auth Service**      | 8081 | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| **User Service**      | 8082 | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| **Order Service**     | 8083 | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| **Inventory Service** | 8084 | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| **Payment Service**   | 8086 | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |

### 5a. Example: Test Auth Service via Swagger

1. **Open browser:** http://localhost:8081/swagger-ui.html
2. **Find** `/auth/register` endpoint
3. **Click** "Try it out"
4. **Enter example body:**
   ```json
   {
     "firstName": "John",
     "lastName": "Doe",
     "email": "john@example.com",
     "password": "password123"
   }
   ```
5. **Click** "Execute"
6. **View response** with HTTP status code

---

## Step 6: Test the Complete Saga Flow

### 6a. Create a User (Auth Service)

**Request:**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Smith",
    "email": "alice@example.com",
    "password": "Secure123!"
  }'
```

**Response:**

```json
{
  "id": 1,
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 6b. Get the JWT Token

**Request:**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Secure123!"
  }'
```

**Response:** _(copy the token)_

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 6c. Create an Order (with JWT Bearer Token)

**Request:**

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 1,
    "price": 999.99,
    "totalAmount": 999.99
  }'
```

**Response:**

```json
{
  "id": 100,
  "userId": 1,
  "productName": "Laptop",
  "quantity": 1,
  "status": "PENDING",
  "totalAmount": 999.99
}
```

### 6d. Monitor Saga Events via Kafka UI

**Kafka UI:** http://localhost:8085

Watch topics:

- `order-created-topic` — initial order
- `inventory-reserved-topic` — inventory step success
- `inventory-reservation-failed-topic` — inventory step failure
- `payment-completed-topic` — payment success
- `payment-failed-topic` — payment failure

### 6e. Check Order Status

**Request:**

```bash
curl -X GET http://localhost:8080/orders/100 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Response (after saga completes in ~2-3 seconds):**

```json
{
  "id": 100,
  "userId": 1,
  "productName": "Laptop",
  "quantity": 1,
  "status": "CONFIRMED",  ← Updated by saga!
  "totalAmount": 999.99
}
```

---

## Step 7: Monitor Distributed Tracing (Zipkin)

### 7a. Access Zipkin Dashboard

**Zipkin UI:** http://localhost:9411

### 7b. Trace Your Order Request

1. **Go to search tab**
2. **Select service:** `order-service`
3. **Search**
4. **View trace showing:**
   - Order creation → Kafka publish
   - Inventory service processing
   - Payment service processing
   - All timestamps and latencies

---

## Step 8: Monitor Kafka

### 8a. Kafka UI Dashboard

**Kafka UI:** http://localhost:8085

**Features:**

- View all Kafka topics
- Monitor message flow in real-time
- Inspect message contents
- View consumer groups
- Monitor broker health

### Popular topics to watch:

- `order-created-topic`
- `inventory-reserved-topic`
- `inventory-reservation-failed-topic`
- `payment-completed-topic`
- `payment-failed-topic`

---

## Common Tasks

### Rebuild Single Service After Code Change

```powershell
# Rebuild order-service including dependencies
mvn clean install -DskipTests -am -pl order-service

# Then restart order-service terminal (Ctrl+C, re-run)
```

### View Service Logs in Real Time

```powershell
# Terminal showing order-service logs
mavenlogs: tail E:\microsevices\microservices-platform\order-service\logs\*.log

# Or from service terminal — logs stream to console
```

### Stop All Services

```powershell
# From each service terminal: Ctrl+C
```

### Stop All Infrastructure

```powershell
docker-compose down

# Preserve volumes:
docker-compose down -v
```

### Reset Databases

```powershell
# Clear all data and start fresh
docker-compose down -v
docker-compose up -d

# Wait ~30 second for databases to initialize
```

---

## Troubleshooting

### Service fails to start — "Eureka: Unable to register"

**Solution:**

- ✅ Ensure Eureka is started first
- ✅ Check `localhost:8761` is accessible
- ✅ Wait 5 seconds after Eureka startup before starting other services

### Kafka broker not responding

**Solution:**

```powershell
# Verify Kafka is running
docker-compose ps | grep kafka

# Check logs
docker logs kafka

# Restart if needed
docker-compose restart kafka
```

### Database connection refused

**Solution:**

```powershell
# Verify MySQL containers running
docker-compose ps | grep db

# Check MySQL is fully initialized
docker logs auth-db

# Wait 10+ seconds after docker-compose up
```

### Order saga doesn't complete

**Solution:**

1. **Check Kafka topics exist:**
   - Go to http://localhost:8085
   - Verify all 5 topics are listed
2. **Monitor logs:**
   - Check order-service logs for `Saga step` messages
   - Check inventory-service for `Saga step 2`
   - Check payment-service for `Saga step 3`
3. **Inspect Kafka messages:**
   - Use Kafka UI to view topic messages
   - Verify events are being published

---

## Performance Tips

| Tip                       | Command                                     |
| ------------------------- | ------------------------------------------- |
| **Parallel build**        | `mvn install -T 1.5C` (use 1.5× CPU cores)  |
| **Skip tests**            | `mvn install -DskipTests`                   |
| **Skip javadoc**          | `mvn install -Dskip.javadoc`                |
| **Build specific module** | `mvn install -pl module-name`               |
| **Offline mode**          | `mvn install -o` (after first online build) |

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    Microservices Platform                        │
│                                                                   │
│  ┌──────────────────────→ Eureka (8761)                         │
│  │                       Service Registry                        │
│  │                                                               │
│  ├─→ Config Server (8888)                                       │
│  │   Centralized Config via Git                                 │
│  │                                                               │
│  ├─→ API Gateway (8080) ──→ JWT Validation                      │
│  │         ↓                                                      │
│  │    ┌────┴─────────────┬─────────────┬──────────────┐        │
│  │    ↓                  ↓             ↓              ↓          │
│  │ Auth-Svc          User-Svc      Order-Svc    Inventory-Svc   │
│  │ (8081)            (8082)        (8083)        (8084)         │
│  │ JWT Tokens        User CRUD     Saga Pattern   Stock Mgmt    │
│  │                                 Feign Calls   Kafka Consume   │
│  │                                 ↓                             │
│  │                            Payment-Svc                        │
│  │                            (8086)                             │
│  │                            Saga Step 3                        │
│  │                                                               │
│  │  ┌─────────────────────────────────────────────────────┐    │
│  │  │           Kafka (9092) — Event Broker              │    │
│  │  │  Topics: order-created, inventory-reserved, etc.   │    │
│  │  │  Kafka UI: 8085                                     │    │
│  │  └─────────────────────────────────────────────────────┘    │
│  │                                                               │
│  │  ┌─────────────────────────────────────────────────────┐    │
│  │  │    MySQL × 5 (auth, user, order, inventory, payment)   │    │
│  │  │    Ports: 3307, 3308, 3309, 3310, 3311             │    │
│  │  └─────────────────────────────────────────────────────┘    │
│  │                                                               │
│  │  ┌─────────────────────────────────────────────────────┐    │
│  │  │              Zipkin (9411) — Tracing               │    │
│  │  │     Span tracking + distributed trace visualization  │    │
│  │  └─────────────────────────────────────────────────────┘    │
│  │                                                               │
└──┴───────────────────────────────────────────────────────────────┘
```

---

## Next Steps

1. ✅ **Run through saga flow** (Section 6)
2. ✅ **Monitor with Zipkin & Kafka UI** (Sections 7-8)
3. ✅ **Use Swagger UIs** to explore all endpoints (Section 5)
4. 📝 Extend payment gateway integration in `PaymentService.charge()`
5. 📝 Add integration tests for saga scenarios
6. 📝 Implement notification service for order events

---

**Happy microservicing! 🚀**
