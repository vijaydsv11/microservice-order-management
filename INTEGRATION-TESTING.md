# Integration Testing Guide

## End-to-End Kafka Event Flow Test

This guide demonstrates the complete event-driven architecture with Order Service as producer and Inventory Service as consumer.

## Prerequisites

- All services running (Eureka, Auth, User, Order, Inventory, API Gateway)
- MySQL databases created
- Kafka and Zookeeper running
- Inventory data initialized

## Step 1: Verify All Services are Running

```bash
# Check Eureka Dashboard
curl http://localhost:8761/actuator/health

# Check Auth Service
curl http://localhost:8081/actuator/health

# Check User Service
curl http://localhost:8082/actuator/health

# Check Order Service
curl http://localhost:8083/actuator/health

# Check Inventory Service
curl http://localhost:8084/actuator/health

# Check API Gateway
curl http://localhost:8080/actuator/health
```

All should return: `{"status":"UP"}`

## Step 2: Check Initial Inventory

```bash
curl -X GET http://localhost:8084/inventory
```

Expected response (populated by InitialDataLoader):

```json
[
  {
    "id": 1,
    "productName": "Laptop",
    "quantityAvailable": 100,
    "quantityReserved": 0,
    "quantityAllocated": 0,
    "quantityFree": 100
  },
  {
    "id": 2,
    "productName": "Mouse",
    "quantityAvailable": 500,
    "quantityReserved": 0,
    "quantityAllocated": 0,
    "quantityFree": 500
  },
  ...
]
```

## Step 3: Register and Authenticate User

### Register

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@1234",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "testuser",
  "email": "test@example.com",
  "message": "User registered successfully"
}
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@1234"
  }'
```

Copy the JWT token from the response for subsequent requests.

```bash
export JWT_TOKEN="<token-from-response>"
```

## Step 4: Create User Profile

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "testuser@gmail.com",
    "phone": "555-1234",
    "address": "123 Test St",
    "city": "Test City",
    "state": "TC",
    "zipCode": "12345"
  }'
```

Response:

```json
{
  "id": 1,
  "firstName": "Test",
  "lastName": "User",
  "email": "testuser@gmail.com",
  "phone": "555-1234",
  "address": "123 Test St",
  "city": "Test City",
  "state": "TC",
  "zipCode": "12345"
}
```

```bash
export USER_ID=1
```

## Step 5: Create Order (Triggers Kafka Event)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": '$USER_ID',
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99,
    "totalAmount": 1999.98
  }'
```

Response:

```json
{
  "id": 1,
  "userId": 1,
  "productName": "Laptop",
  "quantity": 2,
  "price": 999.99,
  "totalAmount": 1999.98,
  "status": "PENDING",
  "userName": "Test User",
  "userEmail": "test@example.com"
}
```

```bash
export ORDER_ID=1
```

**Note:** At this point, the Order Service has published `OrderCreatedEvent` to Kafka topic "order-created-topic".

## Step 6: Observe Inventory Service Processing Event

Monitor Inventory Service logs:

```bash
# In the terminal running inventory-service, you should see:
# INFO 1234 --- [io-8084-exec-1] c.m.i.s.InventoryService :
# Processing order created event: OrderId=1, Product=Laptop, Quantity=2
# INFO 1234 --- [io-8084-exec-1] c.m.i.s.InventoryService :
# Successfully allocated inventory: Product=Laptop, Quantity=2, Remaining=98
```

## Step 7: Verify Inventory Was Updated

```bash
curl -X GET http://localhost:8084/inventory/product/Laptop
```

Expected response (inventory was decreased by 2):

```json
{
  "id": 1,
  "productName": "Laptop",
  "quantityAvailable": 100,
  "quantityReserved": 0,
  "quantityAllocated": 2,
  "quantityFree": 98
}
```

**Key observation:** `quantityAllocated` increased from 0 to 2, `quantityFree` decreased from 100 to 98.

## Step 8: Create Multiple Orders to Test Consumer Scaling

```bash
# Order 2: 3 units of Mouse
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": 1,
    "productName": "Mouse",
    "quantity": 3,
    "price": 25.00,
    "totalAmount": 75.00
  }'

# Order 3: 1 unit of Keyboard
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": 1,
    "productName": "Keyboard",
    "quantity": 1,
    "price": 99.99,
    "totalAmount": 99.99
  }'

# Order 4: 5 units of Monitor
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": 1,
    "productName": "Monitor",
    "quantity": 5,
    "price": 299.99,
    "totalAmount": 1499.95
  }'
```

## Step 9: Verify All Inventory Updates

```bash
curl -X GET http://localhost:8084/inventory
```

Expected response:

```json
[
  {
    "id": 1,
    "productName": "Laptop",
    "quantityAvailable": 100,
    "quantityReserved": 0,
    "quantityAllocated": 2,
    "quantityFree": 98
  },
  {
    "id": 2,
    "productName": "Mouse",
    "quantityAvailable": 500,
    "quantityReserved": 0,
    "quantityAllocated": 3,
    "quantityFree": 497
  },
  {
    "id": 3,
    "productName": "Keyboard",
    "quantityAvailable": 250,
    "quantityReserved": 0,
    "quantityAllocated": 1,
    "quantityFree": 249
  },
  {
    "id": 4,
    "productName": "Monitor",
    "quantityAvailable": 50,
    "quantityReserved": 0,
    "quantityAllocated": 5,
    "quantityFree": 45
  },
  ...
]
```

## Step 10: Get Low Stock Items

```bash
curl -X GET "http://localhost:8084/inventory/low-stock/10"
```

This returns all items where quantityFree < 10.

## Testing Kafka Consumer Scaling

### Terminal 1: Start second Inventory Service instance

```bash
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"
```

### Terminal 2: Create more orders

```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": 1,
      "productName": "Headphones",
      "quantity": '$i',
      "price": 49.99,
      "totalAmount": '$((i * 49))'.99
    }'
  sleep 0.5
done
```

### Observation

Both Inventory Service instances (port 8084 and 8085) will process events in parallel. Kafka automatically distributes partitions between instances in the same consumer group.

## End-to-End Flow Summary

```
┌─────────────────────────────────────────────────────────────┐
│                   HTTP Request Flow                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User → API Gateway → Order Service → MySQL (order_db)      │
│         :8080              :8083                             │
│                                                              │
│  User Service (for order enrichment via Feign):             │
│  Order Service → User Service → MySQL (user_db)             │
│                    :8082                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│               Kafka Event-Driven Flow                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Order Service → Kafka Topic "order-created-topic"          │
│  (Publisher)            :9092                               │
│                            ↓                                │
│              [Persistent Event Store]                       │
│                            ↓                                │
│  Inventory Service Consumer Group (8084, 8085, ...)         │
│  (Consumer 1, Consumer 2, ...)                              │
│                            ↓                                │
│              MySQL (inventory_db) [Update]                  │
│                        :3310                                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Troubleshooting

### Event Not Processed

1. Check Kafka is running: `docker-compose ps | grep kafka`
2. Check Inventory Service logs for errors
3. Verify Spring Cloud Stream bindings in application.yml
4. Check consumer group: `kafka-consumer-groups --list --bootstrap-server localhost:9092`

### Slow Event Processing

1. Check concurrent threads: `concurrency: 2` in application.yml
2. Scale Inventory Service horizontally (multiple instances)
3. Check database performance: `SHOW PROCESSLIST;` in MySQL

### Duplicate Processing

1. Check to ensure exactly one consumer group instance processes a partition
2. Verify partition count does not exceed consumer count

## Monitoring

### View Consumer Group Status

```bash
docker exec kafka kafka-consumer-groups \
  --list \
  --bootstrap-server localhost:9092
```

### View Topic Partitions and Consumer Lag

```bash
docker exec kafka kafka-consumer-groups \
  --describe \
  --group inventory-service-group \
  --bootstrap-server localhost:9092
```

### View Kafka Topic Messages

```bash
docker exec kafka kafka-console-consumer \
  --topic order-created-topic \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --max-messages 10
```
