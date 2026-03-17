# Quick Start Reference

## ⚡ 30-Second Summary

```powershell
# Terminal 1: Start infrastructure
docker-compose up -d

# Terminal 2: Build everything
mvn clean install -DskipTests -T 1C

# Then in 8 separate terminals, start services in order:
cd eureka-server          && mvn spring-boot:run
cd config-server          && mvn spring-boot:run
cd auth-service           && mvn spring-boot:run
cd user-service           && mvn spring-boot:run
cd order-service          && mvn spring-boot:run
cd inventory-service      && mvn spring-boot:run
cd payment-service        && mvn spring-boot:run
cd api-gateway            && mvn spring-boot:run
```

---

## 🎯 Service Ports & URLs

| Service               | Port | Swagger UI                            | Endpoint              |
| --------------------- | ---- | ------------------------------------- | --------------------- |
| **API Gateway**       | 8080 | http://localhost:8080/swagger-ui.html | http://localhost:8080 |
| **Auth Service**      | 8081 | http://localhost:8081/swagger-ui.html | http://localhost:8081 |
| **User Service**      | 8082 | http://localhost:8082/swagger-ui.html | http://localhost:8082 |
| **Order Service**     | 8083 | http://localhost:8083/swagger-ui.html | http://localhost:8083 |
| **Inventory Service** | 8084 | http://localhost:8084/swagger-ui.html | http://localhost:8084 |
| **Payment Service**   | 8086 | http://localhost:8086/swagger-ui.html | http://localhost:8086 |
| **Eureka Dashboard**  | 8761 | —                                     | http://localhost:8761 |
| **Config Server**     | 8888 | —                                     | http://localhost:8888 |
| **Kafka UI**          | 8085 | —                                     | http://localhost:8085 |
| **Zipkin Tracing**    | 9411 | —                                     | http://localhost:9411 |

---

## 🚀 First Time Setup

### Step 1: Start Docker Infrastructure

```powershell
docker-compose up -d
# Databases: 3307, 3308, 3309, 3310, 3311
# Kafka: 9092 / Zookeeper: 2181
```

### Step 2: Build All Modules

```powershell
mvn clean install -DskipTests -T 1C
# ~3-5 minutes depending on machine
```

### Step 3: Start Services (8 terminals in this order)

1. Eureka Server (8761) — **MUST START FIRST**
2. Config Server (8888)
3. Auth Service (8081)
4. User Service (8082)
5. Order Service (8083)
6. Inventory Service (8084)
7. Payment Service (8086)
8. API Gateway (8080) — **START LAST**

### Step 4: Verify Services Running

- Check Eureka: http://localhost:8761
- Should see 8 services registered

---

## 📚 API Examples

### 1. Register User

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

### 2. Login & Get JWT Token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Secure123!"
  }'
```

**Copy the token from response**

### 3. Create Order (requires JWT)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99,
    "totalAmount": 1999.98
  }'
```

### 4. Get Order Status

```bash
curl -X GET http://localhost:8080/orders/100 \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### 5. Get All Users

```bash
curl -X GET http://localhost:8080/users
```

### 6. Get All Inventory

```bash
curl -X GET http://localhost:8080/inventory
```

---

## 🔍 Monitoring & Debugging

### Swagger UI (Interactive API Testing)

- **Auth Service:** http://localhost:8081/swagger-ui.html
- **User Service:** http://localhost:8082/swagger-ui.html
- **Order Service:** http://localhost:8083/swagger-ui.html
- **Inventory Service:** http://localhost:8084/swagger-ui.html
- **Payment Service:** http://localhost:8086/swagger-ui.html

### Eureka Service Registry

- **URL:** http://localhost:8761
- **Shows:** All registered services with healthy/down status

### Kafka Topic Monitoring

- **URL:** http://localhost:8085
- **Watch topics:**
  - `order-created-topic`
  - `inventory-reserved-topic`
  - `inventory-reservation-failed-topic`
  - `payment-completed-topic`
  - `payment-failed-topic`

### Distributed Tracing

- **URL:** http://localhost:9411/zipkin
- **Search by:** Service name, span name, tags
- **View:** Full request flow across services

---

## 🐛 Troubleshooting

| Issue                             | Solution                                                   |
| --------------------------------- | ---------------------------------------------------------- |
| **"Connection refused"**          | Ensure `docker-compose up -d` succeeded                    |
| **"Eureka: Unable to register"**  | Start Eureka first; wait 5s before starting others         |
| **"Kafka broker not responding"** | Check `docker-compose ps \| grep kafka`                    |
| **"Port already in use"**         | Kill process: `netstat -ano`, then `taskkill /PID <id> /F` |
| **"Order saga doesn't complete"** | Check Kafka UI — verify topics exist                       |
| **Build fails**                   | Clear cache: `mvn clean` then retry                        |

---

## 📦 Database Schemas (Auto-created)

Each service has its own database with auto-generated schema:

- **auth_db**: users table (register/login)
- **user_db**: users table (user profiles)
- **order_db**: orders table (order records)
- **inventory_db**: inventory table (stock records)
- **payment_db**: payments table (transaction records)

Access: `mysql -h localhost -P 330X -u root -proot` (3307-3311)

---

## 🔄 Distributed Saga Flow

```
User creates order → OrderService (PENDING)
         ↓
OrderCreatedEvent published → Kafka
         ↓
InventoryService reserves stock
  ✅ Success → InventoryReservedEvent
  ❌ Failure → InventoryReservationFailedEvent → Order CANCELLED
         ↓
PaymentService processes payment
  ✅ Success → PaymentCompletedEvent → Order CONFIRMED
  ❌ Failure → PaymentFailedEvent → InventoryService releases stock → Order CANCELLED
```

Total flow time: ~2-3 seconds (end-to-end)

---

## 🛠️ Common Commands

```powershell
# Rebuild specific service
mvn install -DskipTests -am -pl order-service

# Rebuild multiple services
mvn install -DskipTests -pl auth-service,user-service -am

# Start single service in debug mode
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics

# View Eureka registered instances
curl http://localhost:8761/eureka/apps | grep serviceName

# Stop all Docker containers
docker-compose down

# Reset databases (WARNING: deletes all data)
docker-compose down -v && docker-compose up -d
```

---

## 📖 Full Documentation

- **Detailed startup:** See [STARTUP-GUIDE.md](STARTUP-GUIDE.md)
- **Architecture:** See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Implementation notes:** See [MODERNIZATION-SUMMARY.md](MODERNIZATION-SUMMARY.md)

---

## ✅ Verification Checklist

- [ ] Docker containers running: `docker-compose ps`
- [ ] All 8 services in Eureka: http://localhost:8761
- [ ] Auth Service responds: `curl http://localhost:8081/actuator/health`
- [ ] User API works: `curl http://localhost:8082/users`
- [ ] Swagger UI accessible: http://localhost:8083/swagger-ui.html
- [ ] Kafka topics visible: http://localhost:8085
- [ ] Zipkin accessible: http://localhost:9411

---

**Happy coding! 🚀**
