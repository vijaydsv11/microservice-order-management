# Microservices Platform Setup Guide

## Quick Start Commands

### 1. Prerequisites Check

```bash
java -version          # Should be Java 17+
mvn -version          # Should be Maven 3.6+
docker -version       # For containerization
docker-compose -version
```

### 2. Initial Setup

```bash
# Navigate to project directory
cd microservices-platform

# Start infrastructure (MySQL, Kafka, Zookeeper)
docker-compose up -d

# Wait for services to be ready (about 30 seconds)
sleep 30

# Build all services
mvn clean install -DskipTests
```

### 3. Start Services (Run each in separate terminal)

**Terminal 1 - Eureka Server:**

```bash
cd eureka-server && mvn spring-boot:run
```

**Terminal 2 - Auth Service:**

```bash
cd auth-service && mvn spring-boot:run
```

**Terminal 3 - User Service:**

```bash
cd user-service && mvn spring-boot:run
```

**Terminal 4 - Order Service:**

```bash
cd order-service && mvn spring-boot:run
```

**Terminal 5 - API Gateway:**

```bash
cd api-gateway && mvn spring-boot:run
```

### 4. Verify Services

All services should be registered in Eureka:

- Open browser: http://localhost:8761
- You should see all 4 services listed

## Port Mappings

| Service        | Port | URL                   |
| -------------- | ---- | --------------------- |
| API Gateway    | 8080 | http://localhost:8080 |
| Auth Service   | 8081 | http://localhost:8081 |
| User Service   | 8082 | http://localhost:8082 |
| Order Service  | 8083 | http://localhost:8083 |
| Eureka Server  | 8761 | http://localhost:8761 |
| MySQL Auth DB  | 3307 | localhost:3307        |
| MySQL User DB  | 3308 | localhost:3308        |
| MySQL Order DB | 3309 | localhost:3309        |
| Kafka          | 9092 | localhost:9092        |
| Zookeeper      | 2181 | localhost:2181        |

## Database Credentials

All databases use:

- **Username**: root
- **Password**: root

## Creating Test Data

### 1. Register User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }'
```

Copy the returned JWT token.

### 2. Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 3. Create User Profile

```bash
TOKEN="your-jwt-token-here"

curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "profile@example.com",
    "phone": "555-1234",
    "address": "123 Test St",
    "city": "Test City",
    "state": "TC",
    "zipCode": "12345"
  }'
```

Note the user ID from the response.

### 4. Create Order

```bash
TOKEN="your-jwt-token-here"
USER_ID=1

curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": '$USER_ID',
    "productName": "Test Product",
    "quantity": 2,
    "price": 99.99,
    "totalAmount": 199.98
  }'
```

## Development Workflow

### Code Changes

1. Make changes to any service
2. Rebuild with: `mvn clean install -DskipTests`
3. Restart only the affected service

### Database Changes

1. Modify entity classes
2. Hibernate DDL-auto is set to `update`, so changes are applied on restart

### Adding New Endpoints

1. Create controller method
2. Restart the service
3. Test via API Gateway

## Debugging

### Check Service Health

```bash
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # User
curl http://localhost:8083/actuator/health  # Order
curl http://localhost:8080/actuator/health  # Gateway
```

### View Logs

```bash
# Recent logs
docker-compose logs

# Follow logs
docker-compose logs -f

# Specific service
docker-compose logs -f kafka
docker-compose logs -f zookeeper
```

### Access MySQL

```bash
mysql -h localhost -P 3307 -u root -p
# Inside MySQL
use auth_db;
show tables;
select * from users;
```

### Enable Debug Logging

In each service's `application.yml`, add:

```yaml
logging:
  level:
    root: DEBUG
    org.springframework.web: DEBUG
    com.microservices: DEBUG
```

## Stopping Services

### Stop All Docker Containers

```bash
docker-compose down
```

### Remove Volumes (Reset Databases)

```bash
docker-compose down -v
```

## Troubleshooting

### Issue: "Connection refused" errors

**Solution**:

- Wait a bit longer for Docker services to start
- Check: `docker-compose ps`

### Issue: "Feign client not found"

**Solution**:

- Verify User Service is registered in Eureka
- Check http://localhost:8761
- Restart Order Service

### Issue: Kafka connection errors

**Solution**:

- Check Kafka is running: `docker-compose logs kafka`
- Restart with: `docker-compose restart kafka`

### Issue: Port already in use

**Solution**:

```bash
# Find process using port (Linux/Mac)
lsof -i :8080

# Kill process
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## Project Structure

```
microservices-platform/
├── eureka-server/          # Service discovery server
├── api-gateway/            # API gateway (routes & JWT validation)
├── auth-service/           # JWT authentication
├── user-service/           # User CRUD operations
├── order-service/          # Order management + Feign + Kafka
├── docker-compose.yml      # Infrastructure setup
├── pom.xml                 # Parent Maven configuration
├── README.md               # Main documentation
└── SETUP.md               # This file
```

## Next Steps

1. **Explore Eureka Dashboard**: http://localhost:8761
2. **Test API Gateway**: Route requests through http://localhost:8080
3. **Monitor Kafka Events**: Check order-created-topic
4. **Review Code**: Understand Feign client and Kafka producer
5. **Add New Services**: Follow the same pattern for inventory/notification services
6. **Deploy**: Create Dockerfiles and push to registry

## Performance Tuning

### Database Connection Pool

Edit `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Kafka Batch Processing

Edit Order Service `application.yml`:

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      linger-ms: 10
```

## Security Notes

⚠️ **FOR DEVELOPMENT ONLY** - Not suitable for production

- Change JWT_SECRET in production
- Use environment variables for credentials
- Enable SSL/TLS
- Implement rate limiting
- Add request validation
- Use secrets management (HashiCorp Vault, etc.)
- Implement proper CORS policies
- Add API authentication/authorization

## Support & Questions

- Check the main README.md for architectural details
- Review service-specific application.yml files
- Check logs for detailed error information
- Verify all ports are accessible: `netstat -an`
