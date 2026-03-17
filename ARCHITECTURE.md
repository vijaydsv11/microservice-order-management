# Microservices Architecture Documentation

## System Architecture

### High-Level Architecture

```plaintext
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ HTTP/REST
                     ▼
        ┌────────────────────────────┐
        │     API GATEWAY (8080)     │
        │  (Spring Cloud Gateway)    │
        │   - Route Management       │
        │   - JWT Validation         │
        └────────────┬───────────────┘
                     │
         ┌───────────┴───────────┬──────────────┐
         │                       │              │
         ▼                       ▼              ▼
    ┌─────────────┐      ┌──────────────┐  ┌──────────────┐
    │ AUTH SERVICE│      │ USER SERVICE │  │ ORDER SERVICE│
    │  (8081)     │      │   (8082)     │  │   (8083)     │
    │ JWT Token   │      │ User CRUD    │  │ Orders CRUD  │
    └────┬────────┘      └──────┬───────┘  └───────┬──────┘
         │                      │                   │
    ┌────▼─────┐           ┌────▼────┐        ┌────▼────┐
    │ AUTH_DB   │           │ USER_DB  │        │ ORDER_DB │
    │MySQL 3307 │           │MySQL3308 │        │MySQL3309 │
    └───────────┘           └──────────┘        └─────┬────┘
                                                       │
                                          Feign Client │
                                                       │
                                          ┌────────────▼────┐
                                          │  USER SERVICE   │
                                          │   (Reference)   │
                                          └─────────────────┘
                                                    │
                                                    │
                    ┌───────────────────────────────1
                    │
                    ▼
          ┌─────────────────────┐
          │  KAFKA BROKER(9092) │
          │ order-created-topic │
          └─────────────────────┘
                    │
        ┌───────────┴──────────────┐
        │                          │
        ▼                          ▼
   ┌──────────────┐        ┌─────────────────┐
   │ INVENTORY    │        │  NOTIFICATION   │
   │  SERVICE     │        │   SERVICE       │
   │(Future)      │        │  (Future)       │
   └──────────────┘        └─────────────────┘

SERVICE DISCOVERY:
        all services
            ▲
            │
            ▼
  ┌──────────────────────┐
  │ EUREKA SERVER (8761) │
  │ (Spring Netflix)     │
  │ Service Registry     │
  │ Load Balancing       │
  └──────────────────────┘
```

## Communication Patterns

### 1. **Synchronous Communication (REST + Feign)**

```
Order Service → [Feign Client] → User Service
                                      ▼
                                  HTTP GET /users/{id}
                                      ▼
                                  Return User Details
                                      ▼
                                Order Service [uses data]
```

**Example**: Order Service validates user before creating order

- Uses Feign client to call User Service
- Waits for response
- Processes based on response
- Real-time validation

### 2. **Asynchronous Communication (Kafka Events)**

```
Order Service → [Publish Event] → Kafka Topic → Inventory Service
                                             → Notification Service
                                             → Other Consumers
```

**Example**: Order Created Event

- Order Service creates order and publishes event
- Event contains: orderId, userId, product, amount, email
- Multiple services can consume independently
- No direct wait for consumer responses
- Better scalability and decoupling

## Service Responsibilities

### **API Gateway (Port 8080)**

**Role**: Entry point for all client requests

- **Routing**: Directs requests to appropriate services
- **Authentication**: Validates JWT tokens
- **Load Balancing**: Distributes load across service instances
- **Cross-cutting Concerns**: Handles common patterns

**Routes**:

```
/auth/** → Auth Service (8081)
/users/** → User Service (8082)
/orders/** → Order Service (8083)
```

### **Auth Service (Port 8081)**

**Role**: Authentication and token management

- **Register**: Create new user accounts
- **Login**: Authenticate users and issue JWT tokens
- **Password Security**: BCrypt encryption
- **Database**: Stores user credentials

**Key Classes**:

- `AuthController`: REST endpoints
- `AuthService`: Business logic
- `JwtProvider`: Token generation/validation
- `UserRepository`: Database access
- `SecurityConfig`: Password encoding

### **User Service (Port 8082)**

**Role**: User profile management

- **CRUD Operations**: Create, read, update, delete users
- **Profile Data**: Address, contact info, preferences
- **Email Validation**: Ensure unique emails
- **Feign Client**: Called by Order Service

**Key Classes**:

- `UserController`: REST endpoints
- `UserService`: Business logic
- `UserRepository`: Database access
- `UserDTO`: Data transfer object

### **Order Service (Port 8083)**

**Role**: Order management and event publishing

- **Order Management**: Create, read, update orders
- **User Validation**: Calls User Service via Feign
- **Event Publishing**: Publishes order events to Kafka
- **Status Tracking**: Manages order lifecycle

**Key Classes**:

- `OrderController`: REST endpoints
- `OrderService`: Business logic
- `UserServiceClient`: Feign client for User Service
- `OrderCreatedEventProducer`: Kafka event publishing
- `OrderRepository`: Database access

### **Eureka Server (Port 8761)**

**Role**: Service discovery and registration

- **Service Registry**: Maintains list of available services
- **Health Checking**: Monitors service health
- **Load Balancing**: Routes traffic to healthy instances
- **Dashboard**: Web UI for service visualization

## Data Flow Scenarios

### Scenario 1: User Registration and Login

```
1. Client POST /auth/register
                    ▼
              API Gateway (Route)
                    ▼
              Auth Service
                    ▼
              Check username/email uniqueness
                    ▼
              Hash password (BCrypt)
                    ▼
              Save to Auth_DB
                    ▼
              Generate JWT token
                    ▼
              Return token + user info
```

### Scenario 2: Create Order with User Validation

```
1. Client POST /orders (with JWT token)
                    ▼
              API Gateway (Validate JWT)
                    ▼
              Order Service (Controller)
                    ▼
              OrderService.createOrder()
                    ▼
         Feign Client: GET /users/{userId}
                    ▼
         User Service: Returns user data
                    ▼
         Validate user exists
                    ▼
         Create Order entity
                    ▼
         Save to Order_DB
                    ▼
         Publish OrderCreatedEvent
                    ▼
         Kafka Producer: Send event
                    ▼
         Kafka Topic: order-created-topic
                    ▼
         Return Order DTO to client
```

### Scenario 3: Event-Driven Inventory Update

```
Order Service (publishes event)
                    ▼
         OrderCreatedEvent
         {
           orderId: 1,
           userId: 5,
           productName: "Laptop",
           quantity: 1,
           totalAmount: 999.99,
           userEmail: "user@example.com"
         }
                    ▼
         Kafka Topic: order-created-topic
                    ▼
         Inventory Service (listens)
                    ▼
         Decrease stock
                    ▼
         Update Inventory_DB
                    ▼
         Notification Service (listens)
                    ▼
         Send order confirmation email
                    ▼
         Update Notification_DB
```

## Database Schema

### Auth_DB (auth_db)

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  enabled BOOLEAN DEFAULT true
);
```

### User_DB (user_db)

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20),
  address VARCHAR(255),
  city VARCHAR(100),
  state VARCHAR(50),
  zip_code VARCHAR(10)
);
```

### Order_DB (order_db)

```sql
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_name VARCHAR(255),
  quantity INT,
  price DECIMAL(10,2),
  total_amount DECIMAL(10,2),
  status ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED'),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Technology Stack Details

### Framework & Libraries

| Technology             | Version  | Purpose            |
| ---------------------- | -------- | ------------------ |
| Spring Boot            | 3.2.0    | Framework          |
| Spring Cloud           | 2023.0.0 | Microservices      |
| Spring Cloud Gateway   | Latest   | API Gateway        |
| Spring Cloud Eureka    | Latest   | Service Discovery  |
| Spring Cloud OpenFeign | Latest   | HTTP Client        |
| Spring Kafka           | Latest   | Message Publishing |
| Spring Data JPA        | Latest   | ORM                |
| MySQL Connector        | 8.0.33   | Database Driver    |
| JJWT                   | 0.12.3   | JWT Tokens         |
| BCrypt                 | Latest   | Password Hashing   |
| Lombok                 | Latest   | Code Generation    |
| Java                   | 17       | Language           |
| Maven                  | 3.6+     | Build Tool         |
| Docker                 | Latest   | Containerization   |
| Kafka                  | 7.5.0    | Message Broker     |
| MySQL                  | 8.0      | Database           |
| Zookeeper              | 7.5.0    | Kafka Coordination |

## Deployment Considerations

### Development

- Run all services locally with Docker for infrastructure
- Use IDE for each service
- Hot reload for quick development

### Production

- Containerize each service (Docker)
- Deploy to Kubernetes or VM
- Use managed services (RDS, MSK, etc.)
- Implement proper security (SSL/TLS, secrets management)
- Add monitoring and logging
- Implement backup strategies
- Use load balancers
- Configure auto-scaling

## Performance Optimization

### Database

- Connection pooling (HikariCP)
- Query optimization
- Indexing on frequently searched columns
- Database replication

### Caching

- Redis for session/token caching
- Spring Cache abstractions
- Distributed caching

### Load Balancing

- API Gateway load balancing
- Eureka client-side load balancing
- Kubernetes ingress

### Monitoring

- Spring Boot Actuator
- Prometheus metrics
- Grafana dashboards
- ELK stack for logs

## Security Measures

### Current Implementation

- JWT tokens with expiration
- Password hashing with BCrypt
- API Gateway validation

### Recommended Enhancements

- SSL/TLS for all communications
- Spring Security for fine-grained access control
- OAuth2 for third-party integrations
- Rate limiting and throttling
- API key management
- Secrets management (HashiCorp Vault)
- CORS configuration
- SQL injection prevention
- CSRF protection
- Input validation

## Scalability Strategy

### Horizontal Scaling

- Run multiple instances of each service
- Load balancer distributes requests
- Eureka handles service discovery

### Vertical Scaling

- Increase resources for high-load services
- Database optimization
- Caching layers

### Database Scaling

- Read replicas
- Sharding strategy
- Database replication

## Monitoring & Observability

### Logs

- Centralized logging (ELK stack)
- Structured logging
- Log aggregation

### Metrics

- Time-series metrics (Prometheus)
- Application metrics via Actuator
- Custom business metrics

### Tracing

- Distributed tracing (Sleuth + Zipkin)
- Request tracking across services
- Performance bottleneck identification

### Alerts

- Threshold-based alerts
- Error rate monitoring
- SLA monitoring
