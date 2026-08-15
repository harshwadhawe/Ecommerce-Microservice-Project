# E-commerce Microservices Application

A comprehensive e-commerce web application built with Spring Boot microservices architecture, React.js frontend, and containerized with Docker.

## Architecture Overview

This application follows a microservices architecture with the following components:

### Backend Services (Java Spring Boot)
- **User Service** (Port 8081) - User management and JWT authentication
- **Product Service** (Port 8082) - Product catalog with MongoDB
- **Cart Service** (Port 8083) - Shopping cart with Redis
- **Order Service** (Port 8084) - Order management with MySQL
- **Payment Service** (Port 8085) - Payment gateway simulator

### Frontend
- **React.js Application** (Port 3000) - Responsive user interface

### Databases
- **MySQL** - User and Order data
- **MongoDB** - Product catalog
- **Redis** - Shopping cart and session management

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17 (for local development)
- Node.js 18+ (for frontend development)
- Maven 3.8+ (for building services)

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd SpringBoot
   ```

2. **Build all services**
   ```bash
   # Build backend services
   cd backend/user-service && mvn clean package -DskipTests
   cd ../product-service && mvn clean package -DskipTests
   cd ../cart-service && mvn clean package -DskipTests
   cd ../order-service && mvn clean package -DskipTests
   cd ../payment-service && mvn clean package -DskipTests
   cd ../../
   
   # Build frontend
   cd frontend && npm install && npm run build
   cd ../
   ```

3. **Start all services**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - User Service: http://localhost:8081
   - Product Service: http://localhost:8082
   - Cart Service: http://localhost:8083
   - Order Service: http://localhost:8084
   - Payment Service: http://localhost:8085

## Local Development

### Backend Services

Each service can be run independently:

```bash
# User Service
cd backend/user-service
mvn spring-boot:run

# Product Service
cd backend/product-service
mvn spring-boot:run

# Cart Service
cd backend/cart-service
mvn spring-boot:run

# Order Service
cd backend/order-service
mvn spring-boot:run

# Payment Service
cd backend/payment-service
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

## Database Setup

### MySQL (User and Order Services)
- Host: localhost:3306
- Database: ecommerce
- Username: ecommerce
- Password: password

### MongoDB (Product Service)
- Host: localhost:27017
- Database: productdb
- Username: admin
- Password: password

### Redis (Cart Service)
- Host: localhost:6379
- No authentication required

## API Documentation

### User Service (8081)
- `POST /api/users/register` - User registration
- `POST /api/users/login` - User login
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile
- `POST /api/users/validate` - Validate JWT token

### Product Service (8082)
- `GET /api/products` - Get all products (paginated)
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product
- `GET /api/products/search?q={query}` - Search products
- `GET /api/products/category/{category}` - Get products by category

### Cart Service (8083)
- `GET /api/cart/{userId}` - Get user's cart
- `POST /api/cart/{userId}/items` - Add item to cart
- `PUT /api/cart/{userId}/items/{productId}` - Update cart item
- `DELETE /api/cart/{userId}/items/{productId}` - Remove item from cart
- `DELETE /api/cart/{userId}` - Clear cart

### Order Service (8084)
- `POST /api/orders` - Create new order
- `GET /api/orders/{userId}` - Get user's orders
- `GET /api/orders/{orderId}` - Get order by ID
- `PUT /api/orders/{orderId}/status` - Update order status

### Payment Service (8085)
- `POST /api/payment/process` - Process payment

## Configuration

### Environment Variables

#### User Service
- `MYSQL_HOST` - MySQL host (default: localhost)
- `MYSQL_PORT` - MySQL port (default: 3306)
- `MYSQL_DATABASE` - Database name (default: ecommerce)
- `MYSQL_USERNAME` - Database username (default: ecommerce)
- `MYSQL_PASSWORD` - Database password (default: password)
- `JWT_SECRET` - JWT secret key

#### Product Service
- `MONGODB_HOST` - MongoDB host (default: localhost)
- `MONGODB_PORT` - MongoDB port (default: 27017)
- `MONGODB_DATABASE` - Database name (default: productdb)
- `MONGODB_USERNAME` - Database username (default: admin)
- `MONGODB_PASSWORD` - Database password (default: password)

#### Cart Service
- `REDIS_HOST` - Redis host (default: localhost)
- `REDIS_PORT` - Redis port (default: 6379)
- `PRODUCT_SERVICE_URL` - Product service URL

#### Order Service
- `MYSQL_HOST` - MySQL host
- `USER_SERVICE_URL` - User service URL
- `PRODUCT_SERVICE_URL` - Product service URL
- `CART_SERVICE_URL` - Cart service URL
- `PAYMENT_SERVICE_URL` - Payment service URL

## Testing

### Unit Tests
```bash
# Run tests for specific service
cd backend/user-service
mvn test

# Run tests for all services
./run-tests.sh
```

### Integration Tests
```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run integration tests
mvn verify -Pintegration-test
```

## AWS Deployment

### Prerequisites
- AWS CLI configured
- Docker images pushed to ECR
- RDS instances for MySQL
- DocumentDB for MongoDB
- ElastiCache for Redis

### Deployment Steps

1. **Create ECR repositories**
   ```bash
   aws ecr create-repository --repository-name ecommerce/user-service
   aws ecr create-repository --repository-name ecommerce/product-service
   aws ecr create-repository --repository-name ecommerce/cart-service
   aws ecr create-repository --repository-name ecommerce/order-service
   aws ecr create-repository --repository-name ecommerce/payment-service
   aws ecr create-repository --repository-name ecommerce/frontend
   ```

2. **Build and push images**
   ```bash
   # Build and tag images
   docker build -t ecommerce/user-service:latest ./backend/user-service
   docker tag ecommerce/user-service:latest {account-id}.dkr.ecr.{region}.amazonaws.com/ecommerce/user-service:latest
   
   # Push to ECR
   aws ecr get-login-password --region {region} | docker login --username AWS --password-stdin {account-id}.dkr.ecr.{region}.amazonaws.com
   docker push {account-id}.dkr.ecr.{region}.amazonaws.com/ecommerce/user-service:latest
   ```

3. **Deploy using ECS or Elastic Beanstalk**
   - Use the provided `docker-compose.yml` as a reference
   - Configure environment variables for AWS resources
   - Set up load balancers and auto-scaling groups

## Security Features

- JWT-based authentication
- Password encryption using BCrypt
- CORS configuration
- Input validation and sanitization
- SQL injection prevention
- XSS protection

## Performance Optimization

- Redis caching for cart data
- Database connection pooling
- Pagination for large datasets
- Lazy loading in React components
- Docker multi-stage builds

## Monitoring and Logging

- Spring Boot Actuator endpoints
- Structured logging with Logback
- Health check endpoints
- Metrics collection ready for Prometheus

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 3000, 3306, 6379, 8081-8085, 27017 are available
2. **Database connection**: Check database services are running and accessible
3. **Memory issues**: Increase Docker memory allocation if services fail to start
4. **Network issues**: Ensure Docker networks are properly configured

### Logs
```bash
# View logs for specific service
docker-compose logs user-service

# View logs for all services
docker-compose logs -f
```

## Support

For questions or issues, please create an issue in the GitHub repository or contact the development team.