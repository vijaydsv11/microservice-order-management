# Microservices Common Configuration

## Environment Variables

All services support the following environment variables:

### JWT Configuration

```bash
JWT_SECRET=your-production-secret-key
JWT_EXPIRATION=86400000  # 24 hours in milliseconds
```

### OAuth2 / Google Configuration

```bash
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### Database Configuration

```bash
DB_USERNAME=root
DB_PASSWORD=root
DB_URL_AUTH=jdbc:mysql://localhost:3306/auth_db
DB_URL_USER=jdbc:mysql://localhost:3306/user_db
DB_URL_ORDER=jdbc:mysql://localhost:3306/order_db
```

### Kafka Configuration

```bash
KAFKA_BROKERS=localhost:9092
KAFKA_ZOOKEEPER=localhost:2181
```

### Eureka Configuration

```bash
EUREKA_URL=http://localhost:8761/eureka
```

## Building Docker Images with Jib

### Build locally without Docker daemon

```bash
mvn clean compile jib:build -pl order-service \
  -Djib.to.image=ghcr.io/yourusername/order-service:1.0.0 \
  -Djib.to.auth.username=YOUR_GITHUB_USERNAME \
  -Djib.to.auth.password=YOUR_GITHUB_TOKEN
```

### Build to Docker daemon

```bash
mvn clean compile jib:dockerBuild -pl order-service
```

### Build all services

```bash
# Build all and push to registry
mvn clean compile jib:build

# Build all to local Docker daemon
mvn clean compile jib:dockerBuild
```

## Java 17 Records Usage

DTOs are now implemented using Java 17 records for immutability and cleaner code:

```java
public record OrderDTO(
    Long id,
    Long userId,
    String productName,
    Integer quantity,
    BigDecimal price,
    BigDecimal totalAmount,
    String status,
    String userName,
    String userEmail
) {}
```

### Creating Record Instances

```java
OrderDTO order = new OrderDTO(
    1L,           // id
    5L,           // userId
    "Laptop",     // productName
    1,            // quantity
    999.99,       // price
    999.99,       // totalAmount
    "PENDING",    // status
    "John Doe",   // userName
    "john@example.com"  // userEmail
);
```

### Accessing Record Fields

```java
Long id = order.id();  // Accessor method (not getId())
String name = order.productName();
```

## Stream API Usage

Services extensively use Java streams for functional programming:

```java
// Get all orders and filter
orderRepository.findAll()
    .parallelStream()
    .filter(order -> order.getStatus() == OrderStatus.PENDING)
    .map(this::convertToDTO)
    .collect(Collectors.toList());

// Check valid status
boolean isValid = Arrays.stream(Order.OrderStatus.values())
    .anyMatch(s -> s.toString().equals(requestStatus));
```

## Spring Cloud Stream for Kafka

### Publishing Events

```java
@Service
public class OrderCreatedEventProducer {
    private final StreamBridge streamBridge;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        streamBridge.send("order-created-out-0", event);
    }
}
```

### Consuming Events

```java
@Bean
public Function<OrderCreatedEvent, Void> processOrderEvent() {
    return event -> {
        // Process event
        inventory.decreaseStock(event.productId());
        notification.sendEmail(event.userEmail());
        return null;
    };
}
```

### Configuration

```yaml
spring:
  cloud:
    stream:
      bindings:
        order-created-out-0:
          destination: order-created-topic
          contentType: application/json
      kafka:
        binder:
          brokers: localhost:9092
```

## JWT Authorization

### Public Endpoints (No JWT Required)

- `/auth/login`
- `/auth/register`
- `/actuator/health`

### Protected Endpoints (JWT Required)

- All `/users/**` endpoints
- All `/orders/**` endpoints

### Adding JWT Token to Requests

```bash
curl -H "Authorization: Bearer <returned-jwt-token>" \
  http://localhost:8080/users
```

## Google OAuth2 Integration

### Prerequisites

1. Create Google OAuth2 credentials at https://console.cloud.google.com/
2. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://your-domain.com/login/oauth2/code/google`

### Environment Setup

```bash
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret"
```

### Using OAuth2

1. Redirect users to: `http://localhost:8080/oauth2/authorization/google`
2. After authentication, access `/users/me` endpoint with OAuth2 token

## Jib Plugin Features

- **No Docker daemon required**: Build images without Docker installed
- **Lightweight layers**: Only application code changes trigger image rebuild
- **Alpine base image**: Uses Java 17 on Alpine for smaller image size
- **Production ready**: G1GC garbage collector and timezone configuration

### Configuration in pom.xml

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.0</version>
    <configuration>
        <from>
            <image>eclipse-temurin:17-jre-alpine</image>
        </from>
        <to>
            <image>ghcr.io/microservices/${project.artifactId}:${project.version}</image>
        </to>
        <container>
            <jvmFlags>-XX:+UseG1GC</jvmFlags>
        </container>
    </configuration>
</plugin>
```

## Production Checklist

- [ ] Change JWT_SECRET in production
- [ ] Configure Google OAuth2 credentials
- [ ] Update database passwords
- [ ] Enable HTTPS/TLS
- [ ] Configure proper logging
- [ ] Set up monitoring and alerts
- [ ] Configure backup strategies
- [ ] Use secrets management (Vault, HashiCorp, etc.)
- [ ] Enable rate limiting
- [ ] Implement API gateway authentication

## Migration Path from Lombok to Records

For existing entities that don't use Lombok:

1. Convert DTO classes to records
2. Update service layer to use accessor methods (e.g., `dto.id()` instead of `dto.getId()`)
3. Use record constructors for instantiation: `new OrderDTO(...)`
4. Entities remain with Lombok for JPA @Entity compatibility

## Troubleshooting Jib Builds

### Issue: "Repository does not exist"

```bash
# Solution: Configure container registry credentials
mvn jib:build \
  -Djib.to.auth.username=<username> \
  -Djib.to.auth.password=<token>
```

### Issue: "Image too large"

```bash
# Solution: Use Docker build cache
mvn jib:build -Djib.cache.layers=true
```

### Issue: "Port 8080 already in use"

```bash
# Solution: Change port in application.yml or use environment variable
export SERVER_PORT=9080
mvn spring-boot:run
```

## Kafka Event-Driven Architecture

### Order Service (Producer)

The order-service publishes OrderCreatedEvent when a new order is created:

```java
// OrderCreatedEvent record
public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    String productName,
    Integer quantity,
    BigDecimal totalAmount,
    String userEmail,
    long timestamp
) {}

// Publishing via StreamBridge
@Service
@AllArgsConstructor
public class OrderCreatedEventProducer {
    private final StreamBridge streamBridge;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        streamBridge.send("order-created-out-0", event);
    }
}
```

### Inventory Service (Consumer)

The inventory-service consumes OrderCreatedEvent and allocates inventory:

```yaml
# application.yml
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

```java
// Functional bean consumer
@Configuration
public class StreamConfig {
    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedConsumer() {
        return inventoryService::handleOrderCreatedEvent;
    }
}

// Event handler
@Service
public class InventoryService {
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        Inventory inventory = inventoryRepository
            .findByProductName(event.productName())
            .orElseThrow(...);

        inventory.allocateQuantity(event.quantity());
        inventoryRepository.save(inventory);
    }
}
```

### Event Flow

1. User creates order via POST `/orders` in order-service
2. OrderService saves order and publishes OrderCreatedEvent to Kafka topic "order-created-topic"
3. Kafka persists event with topic partitions
4. InventoryService consumer group "inventory-service-group" receives event
5. StreamConfig's orderCreatedConsumer bean processes event
6. InventoryService.handleOrderCreatedEvent() allocates quantity
7. Inventory is updated in inventory_db (port 3310)

### Testing the Kafka Flow

**Step 1: Create order**

```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 999.99
  }'
```

**Step 2: Verify inventory was allocated**

```bash
curl http://localhost:8084/inventory/product/Laptop
```

Expected response:

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

### Consumer Configuration Details

**Function Definition**

- `function.definition: orderCreatedConsumer` tells Spring Cloud Stream which beans are consumers/producers
- Bean name must end in `Consumer` for input bindings (automatic suffix), `Supplier` for output

**Binding Configuration**

- `orderCreatedConsumer-in-0`: Input binding (function name + `-in-0`)
- `destination: order-created-topic`: Kafka topic name
- `group: inventory-service-group`: Consumer group for scalability
- `concurrency: 2`: Process 2 events concurrently

**Message Content Type**

- `content-type: application/json`: Records are serialized as JSON by default
- Spring Cloud Stream automatically deserializes JSON to OrderCreatedEvent record

### Scaling Consumers

To scale inventory-service horizontally:

```bash
# Terminal 1
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"

# Terminal 2 (different process)
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"
```

Both instances join the same consumer group "inventory-service-group" and Kafka load-balances partitions between them.

### Services Architecture

| Service           | Port | Database            | Role                                 |
| ----------------- | ---- | ------------------- | ------------------------------------ |
| Eureka Server     | 8761 | -                   | Service Registry                     |
| API Gateway       | 8080 | -                   | Request Routing, JWT Validation      |
| Auth Service      | 8081 | auth_db (3307)      | JWT Token Generation, OAuth2         |
| User Service      | 8082 | user_db (3308)      | User Management                      |
| Order Service     | 8083 | order_db (3309)     | Order Management, Event Publisher    |
| Inventory Service | 8084 | inventory_db (3310) | Inventory Management, Event Consumer |

### Kafka Infrastructure

- **Kafka Broker**: localhost:9092
- **Zookeeper**: localhost:2181
- **Default Topic**: order-created-topic (auto-created)
