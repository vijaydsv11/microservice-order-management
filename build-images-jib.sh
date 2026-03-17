#!/bin/bash

# Jib Docker Image Builder Script
# Builds all microservices using Google Jib Maven plugin

set -e

echo "============================================"
echo "Building Docker Images with Jib"
echo "============================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[i]${NC} $1"
}

# Configuration
REGISTRY=${1:-"localhost:5000"}  # Default to local registry
IMAGE_TAG=${2:-"latest"}

print_info "Using registry: $REGISTRY"
print_info "Using tag: $IMAGE_TAG"
echo ""

# Services to build
SERVICES=("eureka-server" "config-server" "api-gateway" "auth-service" "user-service" "order-service" "inventory-service" "payment-service")

# Build all services
for SERVICE in "${SERVICES[@]}"; do
    print_info "Building $SERVICE..."
    
    if [ "$REGISTRY" == "localhost:5000" ]; then
        # Build to local Docker daemon
        mvn clean compile jib:dockerBuild \
            -pl "$SERVICE" \
            -DskipTests \
            -Djib.to.image="$REGISTRY/$SERVICE:$IMAGE_TAG" 2>&1 | grep -E "(BUILD|ERROR|\[INFO\])"
    else
        # Build and push to remote registry
        mvn clean compile jib:build \
            -pl "$SERVICE" \
            -DskipTests \
            -Djib.to.image="$REGISTRY/$SERVICE:$IMAGE_TAG" 2>&1 | grep -E "(BUILD|ERROR|\[INFO\])"
    fi
    
    if [ $? -eq 0 ]; then
        print_status "$SERVICE built successfully"
    else
        echo "ERROR: Failed to build $SERVICE"
        exit 1
    fi
done

echo ""
echo "============================================"
echo "All Docker images built successfully!"
echo "============================================"
echo ""

if [ "$REGISTRY" == "localhost:5000" ]; then
    echo "Images available locally in Docker:"
    docker images | grep -E "(REPOSITORY|$REGISTRY)"
else
    echo "Images pushed to: $REGISTRY"
    echo ""
    print_info "To run images locally, pull first:"
    for SERVICE in "${SERVICES[@]}"; do
        echo "docker run -d $REGISTRY/$SERVICE:$IMAGE_TAG"
    done
fi
