# Complete API Reference

## 📌 Overview

This document provides a complete reference of all available endpoints across the microservices platform.

**To test these APIs:**

1. Start services: Follow [QUICK-START.md](QUICK-START.md)
2. Use Swagger UI: http://localhost:8080/swagger-ui.html (or service-specific UI)
3. Or use curl commands below

---

## 🔐 Authentication

### Get JWT Token

**Endpoint:** `POST /auth/login`  
**Service:** Auth Service (via Gateway at 8080)  
**Headers:** `Content-Type: application/json`

**Request:**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "email": "user@example.com"
}
```

**Usage:** Add to all subsequent requests:

```bash
-H "Authorization: Bearer <TOKEN>"
```

---

## 👤 Auth Service (Port 8081)

### Register User

**Endpoint:** `POST /auth/register`  
**Authentication:** None  
**Headers:** `Content-Type: application/json`

**Request:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Secure123!"
}
```

**Response:**

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**cURL Example:**

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

---

## 👥 User Service (Port 8082)

### Get All Users

**Endpoint:** `GET /users`  
**Authentication:** Optional  
**Response:** Array of user objects

```bash
curl -X GET http://localhost:8080/users
```

**Response:**

```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  },
  {
    "id": 2,
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com"
  }
]
```

### Get User by ID

**Endpoint:** `GET /users/{id}`  
**Parameters:**

- `id` (path) — User ID (required)

```bash
curl -X GET http://localhost:8080/users/1
```

**Response:**

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

### Get User by Email

**Endpoint:** `GET /users/email/{email}`  
**Parameters:**

- `email` (path) — User email (required)

```bash
curl -X GET http://localhost:8080/users/email/john@example.com
```

### Create User

**Endpoint:** `POST /users`  
**Authentication:** Required (Bearer token)

**Request:**

```json
{
  "firstName": "Alice",
  "lastName": "Brown",
  "email": "alice@example.com"
}
```

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "firstName": "Alice",
    "lastName": "Brown",
    "email": "alice@example.com"
  }'
```

### Update User

**Endpoint:** `PUT /users/{id}`  
**Parameters:**

- `id` (path) — User ID (required)

```bash
curl -X PUT http://localhost:8080/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.updated@example.com"
  }'
```

### Delete User

**Endpoint:** `DELETE /users/{id}`  
**Parameters:**

- `id` (path) — User ID (required)

```bash
curl -X DELETE http://localhost:8080/users/1 \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📦 Order Service (Port 8083)

### Get All Orders

**Endpoint:** `GET /orders`  
**Response:** Array of order objects

```bash
curl -X GET http://localhost:8080/orders
```

**Response:**

```json
[
  {
    "id": 100,
    "userId": 1,
    "productName": "Laptop",
    "quantity": 1,
    "price": 999.99,
    "totalAmount": 999.99,
    "status": "CONFIRMED",
    "userName": "John Doe",
    "userEmail": "john@example.com"
  }
]
```

### Get Order by ID

**Endpoint:** `GET /orders/{id}`  
**Parameters:**

- `id` (path) — Order ID (required)

```bash
curl -X GET http://localhost:8080/orders/100 \
  -H "Authorization: Bearer <TOKEN>"
```

### Get Orders by User

**Endpoint:** `GET /orders/user/{userId}`  
**Parameters:**

- `userId` (path) — User ID (required)

```bash
curl -X GET http://localhost:8080/orders/user/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### Create Order (Triggers Saga)

**Endpoint:** `POST /orders`  
**Authentication:** Required  
**⚠️ IMPORTANT:** This endpoint triggers the distributed saga pattern!

**Request:**

```json
{
  "userId": 1,
  "productName": "Laptop",
  "quantity": 2,
  "price": 999.99,
  "totalAmount": 1999.98
}
```

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99,
    "totalAmount": 1999.98
  }'
```

**Response:**

```json
{
  "id": 100,
  "userId": 1,
  "productName": "Laptop",
  "quantity": 2,
  "price": 999.99,
  "totalAmount": 1999.98,
  "status": "PENDING",
  "userName": "John Doe",
  "userEmail": "john@example.com"
}
```

**🔄 Saga Flow After Order Creation:**

1. Order status: **PENDING**
2. (2-3 seconds) InventoryService reserves stock
3. (2-3 seconds) PaymentService processes payment
4. Final status: **CONFIRMED** (success) or **CANCELLED** (failure)

### Update Order Status

**Endpoint:** `PUT /orders/{id}/status`  
**Parameters:**

- `id` (path) — Order ID (required)
- `status` (query) — New status (required)

```bash
curl -X PUT "http://localhost:8080/orders/100/status?status=CONFIRMED" \
  -H "Authorization: Bearer <TOKEN>"
```

### Delete Order

**Endpoint:** `DELETE /orders/{id}`  
**Parameters:**

- `id` (path) — Order ID (required)

```bash
curl -X DELETE http://localhost:8080/orders/100 \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📦 Inventory Service (Port 8084)

### Get All Inventory

**Endpoint:** `GET /inventory`  
**Response:** Array of inventory items

```bash
curl -X GET http://localhost:8080/inventory
```

**Response:**

```json
[
  {
    "id": 1,
    "productName": "Laptop",
    "quantityAvailable": 100,
    "quantityReserved": 2,
    "quantityAllocated": 0,
    "quantityFree": 98
  }
]
```

### Get Inventory by ID

**Endpoint:** `GET /inventory/{id}`  
**Parameters:**

- `id` (path) — Inventory ID (required)

```bash
curl -X GET http://localhost:8080/inventory/1
```

### Get Inventory by Product Name

**Endpoint:** `GET /inventory/product/{productName}`  
**Parameters:**

- `productName` (path) — Product name (required)

```bash
curl -X GET http://localhost:8080/inventory/product/Laptop
```

**Response:**

```json
{
  "id": 1,
  "productName": "Laptop",
  "quantityAvailable": 100,
  "quantityReserved": 2,
  "quantityAllocated": 0,
  "quantityFree": 98
}
```

### Get Low Stock Items

**Endpoint:** `GET /inventory/low-stock/{threshold}`  
**Parameters:**

- `threshold` (path) — Stock threshold (required, e.g., 20)

```bash
curl -X GET http://localhost:8080/inventory/low-stock/20
```

**Response:** Array of inventory items with quantity < threshold

### Add Inventory

**Endpoint:** `POST /inventory/add`  
**Parameters:**

- `productName` (query) — Product name (required)
- `quantity` (query) — Quantity to add (required)

```bash
curl -X POST "http://localhost:8080/inventory/add?productName=Laptop&quantity=50"
```

### Reserve Inventory

**Endpoint:** `POST /inventory/reserve`  
**Parameters:**

- `productName` (query) — Product name (required)
- `quantity` (query) — Quantity to reserve (required)

```bash
curl -X POST "http://localhost:8080/inventory/reserve?productName=Laptop&quantity=2"
```

### Release Inventory

**Endpoint:** `POST /inventory/release`  
**Parameters:**

- `productName` (query) — Product name (required)
- `quantity` (query) — Quantity to release (required)

```bash
curl -X POST "http://localhost:8080/inventory/release?productName=Laptop&quantity=2"
```

---

## 💳 Payment Service (Port 8086)

### Get All Payments

**Endpoint:** `GET /payments`  
**Response:** Array of payment objects

```bash
curl -X GET http://localhost:8086/payments
```

**Response:**

```json
[
  {
    "id": 1,
    "orderId": 100,
    "amount": 1999.98,
    "status": "COMPLETED",
    "createdAt": "2026-03-16T22:30:45.123"
  }
]
```

### Get Payment by ID

**Endpoint:** `GET /payments/{id}`  
**Parameters:**

- `id` (path) — Payment ID (required)

```bash
curl -X GET http://localhost:8086/payments/1
```

### Get Payment by Order ID

**Endpoint:** `GET /payments/order/{orderId}`  
**Parameters:**

- `orderId` (path) — Order ID (required)

```bash
curl -X GET http://localhost:8086/payments/order/100
```

**Response:**

```json
{
  "id": 1,
  "orderId": 100,
  "amount": 1999.98,
  "status": "COMPLETED",
  "createdAt": "2026-03-16T22:30:45.123",
  "updatedAt": "2026-03-16T22:30:47.456"
}
```

---

## 🏥 Health & Monitoring Endpoints

All services expose health and metrics endpoints:

### Health Check

**Endpoint:** `GET /actuator/health`  
**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8086/actuator/health
```

### Metrics

**Endpoint:** `GET /actuator/metrics`

```bash
curl http://localhost:8080/actuator/metrics
```

### Service Discovery

**Endpoint:** `GET /eureka/apps`  
**Service:** Eureka Server (8761)

```bash
curl http://localhost:8761/eureka/apps | grep serviceName
```

---

## 🔄 Complete Order Saga Example

### Step 1: Register User

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

**Response includes JWT token** — save it

### Step 2: Get JWT Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "password": "Secure123!"}' | jq -r '.token')

echo $TOKEN
```

### Step 3: Create Order (Starts Saga)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99,
    "totalAmount": 1999.98
  }'
```

**Note:** Status will be **PENDING**

### Step 4: Wait 2-3 Seconds (Saga Processing)

```bash
sleep 3
```

### Step 5: Check Order Status

```bash
curl -X GET http://localhost:8080/orders/100 \
  -H "Authorization: Bearer $TOKEN"
```

**Status should now be: CONFIRMED** (saga succeeded)

### Step 6: Monitor via Kafka UI

**URL:** http://localhost:8085

Watch topics:

- `order-created-topic`
- `inventory-reserved-topic`
- `payment-completed-topic`

---

## 📊 API Status Codes

| Code | Meaning                            |
| ---- | ---------------------------------- |
| 200  | OK — Request succeeded             |
| 201  | Created — Resource created         |
| 400  | Bad Request — Invalid parameters   |
| 401  | Unauthorized — Missing/invalid JWT |
| 403  | Forbidden — Access denied          |
| 404  | Not Found — Resource doesn't exist |
| 500  | Server Error — Service error       |

---

## 🔗 Interactive Testing

**Use Swagger UI for interactive testing:**

1. Open: http://localhost:8080/swagger-ui.html (or service-specific)
2. Click endpoint
3. Click "Try it out"
4. Enter parameters
5. Click "Execute"
6. View response

---

## Required Fields Summary

| Endpoint                  | Required Fields                                   |
| ------------------------- | ------------------------------------------------- |
| `POST /auth/register`     | firstName, lastName, email, password              |
| `POST /auth/login`        | email, password                                   |
| `POST /users`             | firstName, lastName, email                        |
| `POST /orders`            | userId, productName, quantity, price, totalAmount |
| `POST /inventory/add`     | productName, quantity                             |
| `POST /inventory/reserve` | productName, quantity                             |

---

**For more details, see comprehensive guides:**

- [QUICK-START.md](QUICK-START.md)
- [STARTUP-GUIDE.md](STARTUP-GUIDE.md)
