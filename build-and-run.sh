#!/bin/bash

# Microservices Platform Build & Run Script

set -e

echo "============================================"
echo "Microservices Platform - Build & Run"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[i]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Check prerequisites
print_info "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    print_error "Java is not installed"
    exit 1
fi
print_status "Java found: $(java -version 2>&1 | head -n 1)"

if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed"
    exit 1
fi
print_status "Maven found: $(mvn -version | head -n 1)"

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    exit 1
fi
print_status "Docker found: $(docker --version)"

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed"
    exit 1
fi
print_status "Docker Compose found: $(docker-compose --version)"

echo ""

# Step 1: Start Docker infrastructure
print_info "Starting Docker infrastructure..."
docker-compose up -d

print_status "Docker services started"
print_info "Waiting 30 seconds for services to be ready..."
sleep 30

echo ""

# Step 2: Build all services
print_info "Building all services with Maven..."
mvn clean install -DskipTests -q

print_status "All services built successfully"

echo ""
echo "============================================"
echo "Setup Complete!"
echo "============================================"
echo ""
print_info "Services to start (in separate terminals):"
echo ""
echo "1. Eureka Server (Service Discovery)"
echo "   cd eureka-server && mvn spring-boot:run"
echo ""
echo "2. Auth Service"
echo "   cd auth-service && mvn spring-boot:run"
echo ""
echo "3. User Service"
echo "   cd user-service && mvn spring-boot:run"
echo ""
echo "4. Order Service"
echo "   cd order-service && mvn spring-boot:run"
echo ""
echo "5. API Gateway (last)"
echo "   cd api-gateway && mvn spring-boot:run"
echo ""
echo "============================================"
echo "Service URLs:"
echo "============================================"
echo "API Gateway: http://localhost:8080"
echo "Auth Service: http://localhost:8081"
echo "User Service: http://localhost:8082"
echo "Order Service: http://localhost:8083"
echo "Eureka Dashboard: http://localhost:8761"
echo ""
echo "============================================"
echo "Database Connections:"
echo "============================================"
echo "Auth DB: localhost:3307 (auth_db)"
echo "User DB: localhost:3308 (user_db)"
echo "Order DB: localhost:3309 (order_db)"
echo "Credentials: root / root"
echo ""
echo "Kafka: localhost:9092"
echo "Zookeeper: localhost:2181"
echo ""
echo "============================================"
echo "View Docker Logs:"
echo "============================================"
echo "docker-compose logs -f"
echo ""
print_status "Setup complete! Start services in separate terminals."
