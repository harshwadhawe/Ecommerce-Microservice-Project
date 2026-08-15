#!/bin/bash

# E-commerce Application Build Script
set -e

echo "Building E-commerce Application..."

# Function to print colored output
print_status() {
    echo "$1"
}

print_error() {
    echo "$1"
}

print_warning() {
    echo "$1"
}

# Build backend services
print_status "Building backend services..."

services=("user-service" "product-service" "cart-service" "order-service" "payment-service")

for service in "${services[@]}"; do
    print_status "Building $service..."
    if [ -d "backend/$service" ]; then
        cd "backend/$service"
        if mvn clean package -DskipTests; then
            print_status "$service built successfully"
        else
            print_error "Failed to build $service"
            exit 1
        fi
        cd "../../"
    else
        print_error "Directory backend/$service not found"
        exit 1
    fi
done

# Build frontend
print_status "Building frontend..."
if [ -d "frontend" ]; then
    cd frontend
    if npm install && npm run build; then
        print_status "Frontend built successfully"
    else
        print_error "Failed to build frontend"
        exit 1
    fi
    cd ../
else
    print_error "Frontend directory not found"
    exit 1
fi

print_status "All services built successfully!"
print_status "Ready to deploy with: docker-compose up -d"

# Check if Docker is available
if command -v docker &> /dev/null; then
    print_status "Docker is available"
    print_status "Run the following command to start all services:"
    echo "   docker-compose up -d"
else
    print_warning "Docker not found. Install Docker to run the application."
fi