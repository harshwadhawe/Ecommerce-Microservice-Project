# E-commerce Microservices Application

[![CI](https://github.com/harshwadhawe/Ecommerce-Microservice-Project/actions/workflows/ci.yml/badge.svg)](https://github.com/harshwadhawe/Ecommerce-Microservice-Project/actions/workflows/ci.yml)

An e-commerce web application built as Spring Boot microservices with a React frontend, running on
Docker Compose or Kubernetes.

The working flow is: **register → log in → browse the catalog → add to cart → check out → order
history**. All five services are implemented.

## Architecture Overview

Each service is an independent Maven project with its own datastore. There is no API gateway and no
service registry: the frontend calls service ports directly, and services address each other by
hostname from environment variables.

### Backend Services (Java Spring Boot 3.1.5, Java 17)
| Service | Port | Store | State |
|---------|------|-------|-------|
| User Service | 8081 | MySQL | Registration, login, JWT issuing, profile |
| Product Service | 8082 | MongoDB | Catalog CRUD, search, pagination, soft delete |
| Cart Service | 8083 | Redis | Cart with stock checks; verifies JWTs and cart ownership |
| Order Service | 8084 | MySQL | Checkout orchestration, order history, status changes |
| Payment Service | 8085 | none | Simulator: ~90% success, random decline reasons |

### Frontend
- **React 18 + React Router** (Port 3000) — calls the real services; JWT kept in localStorage

### Databases
- **MySQL** — users (and orders, once order-service exists)
- **MongoDB** — product catalog
- **Redis** — carts, keyed `cart:{userId}` with a 24h TTL

## Status

| Area | State |
|------|-------|
| Register / login / JWT auth | Working, 27 tests |
| Product catalog + search | Working, 20 tests |
| Cart with stock limits + ownership | Working, 41 tests |
| Payment simulation | Working, 7 tests |
| Orders + checkout + history | Working, 31 tests |
| Frontend shopping flow | Working, 6 tests |
| End-to-end API suite | 59 checks, green on compose and Kubernetes |
| CI on push | Unit, frontend, and e2e jobs |

## Quick Start

### Prerequisites

- **Docker** — enough on its own; every image compiles itself in a multi-stage build
- **jq** — for `test-e2e.sh` and `seed.sh`
- Java 17 and Maven 3.8+ — only to run services directly on the host
- Node.js 18+ — only for the frontend dev server

### Using Docker Compose (Recommended)

```bash
git clone https://github.com/harshwadhawe/Ecommerce-Microservice-Project.git
cd Ecommerce-Microservice-Project

docker-compose up -d --build   # images build from source; no prior mvn package needed
./seed.sh                      # demo catalog, users, a filled cart and an order
```

Then open **http://localhost:3000** and log in as `alice@example.com` / `password123`.

Services listen on 8081 (user), 8082 (product), 8083 (cart), 8084 (order, empty) and 8085 (payment).

First build compiles five Spring Boot apps and takes a few minutes; later builds are cached.

## Local Development

### Backend Services

Start the databases in Docker, then run whichever services you are working on directly on the host.

```bash
docker-compose up -d mysql mongodb redis

export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS; Maven needs 17, not the system default

# -f avoids relative `cd` chains; each line is its own terminal
mvn -f backend/user-service/pom.xml    spring-boot:run   # :8081  needs mysql
mvn -f backend/product-service/pom.xml spring-boot:run   # :8082  needs mongodb
mvn -f backend/cart-service/pom.xml    spring-boot:run   # :8083  needs redis + product-service
mvn -f backend/payment-service/pom.xml spring-boot:run   # :8085  no dependencies
mvn -f backend/order-service/pom.xml   spring-boot:run   # :8084  needs mysql + cart + payment
```

Check they are up with `curl localhost:8081/actuator/health`.

### Frontend
```bash
cd frontend
npm install
npm start        # dev server on :3000, talks to the services on 8081-8085
npm test         # jest/RTL
```

The pages call the real services. Service URLs come from `REACT_APP_*_SERVICE_URL`, defaulting to
`http://localhost:<port>`, and are baked in at build time — rebuild the image to change them.

The shopping flow: register → login (JWT stored in localStorage) → browse the live catalog →
add to cart (cart-service checks stock) → checkout (payment-service charges, cart is emptied on
success). **There is no order-service**, so nothing persists an order: the confirmation screen shows
the payment transaction id, and there is no order history.

## Database Setup

These are the docker-compose credentials; the Kubernetes deployment uses the same values, supplied
from the `ecommerce-secrets` Secret.

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

## Seed Data

`./seed.sh` fills an empty environment with a demo catalog, users, one populated cart and one order.
Re-running is safe: products already present by name are skipped, existing emails are left alone, and
a user who already has a cart or an order keeps it. `./cluster.sh up` runs it for you.

```bash
./seed.sh                # localhost (docker-compose or the k8s cluster)
HOST=1.2.3.4 ./seed.sh
```

**13 products** across Electronics, Audio, Home and Furniture, with stock levels chosen to exercise
the UI: `Webcam 1080p` is out of stock (Add to Cart disabled) and `Desk Lamp` has 2 left, so asking
for more triggers cart-service's stock rejection.

**4 users**, all with password `password123`:

| Email | Name |
|-------|------|
| alice@example.com | Alice Nguyen |
| bob@example.com | Bob Martins |
| carol@example.com | Carol Silva |
| dave@example.com | Dave Okafor |

Alice starts with a Laptop and a Wireless Mouse in her cart, so the header badge and cart page have
something to show on first login. Bob starts with one completed order, so the Orders page is not
empty either — the payment simulator declines roughly one attempt in ten, so the script retries a
couple of times to make that order `PAID`.

Requires `jq`, and the user, product and cart services to be running.

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

All cart endpoints require `Authorization: Bearer <token>` from user-service login, and the
`{userId}` must match the token's owner — otherwise 401 (no/invalid token) or 403 (someone else's
cart).

- `GET /api/cart/{userId}` - Get user's cart
- `POST /api/cart/{userId}/items` - Add item to cart
- `PUT /api/cart/{userId}/items/{productId}` - Update cart item
- `DELETE /api/cart/{userId}/items/{productId}` - Remove item from cart
- `DELETE /api/cart/{userId}` - Clear cart

### Order Service (8084)

All endpoints require `Authorization: Bearer <token>`. The user is taken from the token, never from
the request — there is no `{userId}` to tamper with.

- `POST /api/orders` - Check out: reads the cart from cart-service, charges payment-service, stores
  the order, empties the cart. `201` on success, `402` if declined, `400` on an empty cart
- `GET /api/orders` - The authenticated user's orders, newest first
- `GET /api/orders/{orderId}` - One order (`403` if it belongs to someone else)
- `PUT /api/orders/{orderId}/status` - Change status (`PENDING`, `PAID`, `PAYMENT_FAILED`,
  `CANCELLED`, `SHIPPED`, `DELIVERED`)

The browser never sends the amount: order-service reads the basket from cart-service itself and
charges what it computed. A declined payment still records the order as `PAYMENT_FAILED` and leaves
the cart intact so the shopper can retry.

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
- `JWT_SECRET` - Must be identical to user-service's, or every cart request returns 401

#### Order Service
- `MYSQL_HOST` - MySQL host (default: localhost)
- `MYSQL_USERNAME` / `MYSQL_PASSWORD` - Database credentials
- `CART_SERVICE_URL` - Cart service URL (the basket is read from here at checkout)
- `PAYMENT_SERVICE_URL` - Payment service URL
- `JWT_SECRET` - Must match user-service's, or every order request returns 401

## Testing

Requires Java 17. If `java -version` reports anything older:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
```

### Unit Tests

126 tests across all five services. Only order-service's persistence test touches a database, and
that one uses in-memory H2 — nothing external is needed.

```bash
# One service
mvn -f backend/cart-service/pom.xml test

# All services
for s in user product cart order payment; do mvn -f backend/$s-service/pom.xml test; done

# A single class, or a single method
mvn -f backend/cart-service/pom.xml test -Dtest=CartServiceTest
mvn -f backend/cart-service/pom.xml test -Dtest=CartTest#addingSameProductTwiceMergesQuantities
```

What they cover:

| Service | Tests | Focus |
|---------|-------|-------|
| user-service | 27 | password hashing, duplicate registration, patch semantics, JWT expiry/forgery/tampering, 401 vs 500 on bad credentials |
| product-service | 20 | soft delete, stock floor at zero, active-product filtering, pagination and sort defaults, validation |
| cart-service | 41 | line-item merging, BigDecimal totals, stock rejection, Redis TTL refresh, JSON round-trip, cart ownership, downstream outage handling |
| order-service | 31 | checkout ordering, cart snapshotting, decline handling, order ownership, and an H2-backed test that a declined order really is persisted |
| payment-service | 7 | success/failure branches with a stubbed `Random`, transaction ids, request validation |

### Coverage

JaCoCo runs on `verify` and writes an HTML report. It is report-only — no minimum threshold is
enforced, so coverage never fails a build.

```bash
mvn -f backend/cart-service/pom.xml verify
open backend/cart-service/target/site/jacoco/index.html
```

### End-to-End Tests

`test-e2e.sh` runs 59 checks against the real HTTP APIs of all five services (51 on the runs where the payment simulator declines, since the failure branch asserts less): registration, login,
JWT protection, cart ownership (401 without a token, 403 for someone else's cart), product CRUD,
search, cart merging and stock limits, payment processing, and the full checkout — order created,
cart emptied, order retrievable, history listed, status advanced. It creates a uniquely-named user
and product, then deletes both.

Checkout asserts against both payment outcomes: a `201` order must be `PAID` with the cart emptied,
a `402` must leave a `PAYMENT_FAILED` order and the cart untouched.

```bash
# 1. Databases
docker-compose up -d mysql mongodb redis

# 2. Services (each in its own terminal)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn -f backend/user-service/pom.xml    spring-boot:run
mvn -f backend/product-service/pom.xml spring-boot:run
mvn -f backend/cart-service/pom.xml    spring-boot:run
mvn -f backend/order-service/pom.xml   spring-boot:run
mvn -f backend/payment-service/pom.xml spring-boot:run

# 3. Run
./test-e2e.sh                # or: HOST=<host> ./test-e2e.sh
```

Requires `jq`. Exits non-zero on the first failing expectation, so it works as a CI gate. The
checkout step is skipped and reported as such — order-service has no implementation yet.

### Continuous Integration

`.github/workflows/ci.yml` runs on every push:

| Job | What it does |
|-----|--------------|
| `unit` | `mvn verify` per service in parallel (matrix of 5), uploads each JaCoCo report as a build artifact |
| `frontend` | `npm ci && npm run build` |
| `e2e` | Boots MySQL, MongoDB and Redis as service containers, builds and launches the four service jars, then runs `test-e2e.sh` |

The e2e job needs no extra configuration — the service-container credentials match the defaults
already in each `application.yml`. It sets `WAIT_SECONDS=180` so the script waits for JVM startup
instead of failing fast, and dumps the last 100 lines of every service log when a run fails.

### Not Yet Covered

- **Repository queries.** `ProductRepository`'s regex `@Query` methods are only exercised through
  the e2e script. Testcontainers-backed `@DataMongoTest` would verify them properly.
- **Frontend component tests.** `src/api.test.js` covers error extraction and session handling, but
  no test renders a page. Worth adding once the UI stops changing shape.

## Kubernetes (local)

Runs the frontend, all five services and their databases on Docker Desktop's built-in cluster. No
Helm, no kind, no extra tooling — plain manifests in `k8s/`, driven by one script.

Enable the cluster once: **Docker Desktop → Settings → Kubernetes → Enable Kubernetes → Apply &
Restart**. After that:

```bash
./cluster.sh up        # build images, deploy, wait for ready, seed demo data
./cluster.sh status    # what is running, and on which ports
./cluster.sh down      # remove the app, keep the database contents
./cluster.sh destroy   # remove everything, including the data volumes
```

`up` prints the URLs and login when it finishes, and works from a completely empty cluster. Two
escape hatches for the slow part:

```bash
SKIP_BUILD=1 ./cluster.sh up    # redeploy without rebuilding the six images
SKIP_SEED=1  ./cluster.sh up    # deploy without demo data
```

**`down` keeps your data** — it removes deployments, services and the secret but leaves the
PersistentVolumeClaims, so a later `up` finds the same users, orders and products. Verified across a
full cycle: 4 users, 1 order and 13 products all survived `down` → `up`. `destroy` is the one that
erases them.

Treat that as convenience, not durability. This is a laptop cluster on ephemeral local-path volumes,
and the contents can disappear for reasons outside the script — a Docker Desktop restart, a node
rebuild, or resource pressure. `./cluster.sh up` reseeds from scratch, so nothing here is worth
mourning.

Because the ports match docker-compose, both scripts work against the cluster unchanged:

```bash
./test-e2e.sh
./seed.sh
```

Doing it by hand instead of using the script:

```bash
for s in user-service product-service cart-service order-service payment-service; do
  docker build -t "ecommerce/${s}:local" "backend/${s}"   # quote it: zsh eats an unquoted :local
done
docker build -t "ecommerce/frontend:local" frontend
kubectl apply -f k8s/
kubectl get pods -w
```

Services are `type: LoadBalancer`, which Docker Desktop maps onto localhost — so 3000 and
8081-8085 behave exactly as they do under compose. Stop compose first or the ports collide.

Verified on Docker Desktop's cluster (kind mode, Kubernetes 1.36.1): all 8 pods Ready, `test-e2e.sh`
46/46 green against the cluster, and a `kubectl rollout restart` served 60/60 requests with no
failures.

Useful while poking at it:

```bash
kubectl logs -f deploy/cart-service
kubectl describe pod -l app=cart-service     # why a pod is not ready
kubectl rollout restart deploy/cart-service  # rolling restart, zero dropped requests
kubectl delete pod -l app=cart-service       # watch it come back on its own
./cluster.sh down                            # tear down, keep data
kubectl delete -f k8s/                       # same, but this DOES delete the PVCs and your data
```

Notes:

- **Rebuilding an image does not redeploy it.** The cluster node caches by tag, so rebuilding
  `:local` and running `kubectl rollout restart` silently keeps serving the old code — a stale pod
  looks exactly like a bug in your change. `./cluster.sh up` avoids this by tagging every build
  `build-<timestamp>` and pointing the deployments at it. By hand:

  ```bash
  TAG=$(date +%s)
  docker build -t "ecommerce/order-service:$TAG" backend/order-service
  kubectl set image deploy/order-service "order-service=ecommerce/order-service:$TAG"
  ```

- **Stopping pods does not free the ports.** `LoadBalancer` services hold 3000 and 8081-8085 even at
  zero replicas, so services started on the host afterwards collide with a listener that has no pods
  behind it — which shows up as connection-closed errors rather than "port in use". `./cluster.sh
  down` deletes the services for this reason.

- **`enableServiceLinks: false` is required, not cosmetic.** Kubernetes injects Docker-link-era
  variables for every Service — `REDIS_PORT` becomes `tcp://10.96.x.x:6379`, which collides with
  this app's own `${REDIS_PORT}` / `${MYSQL_PORT}` / `${MONGODB_PORT}` placeholders and kills
  startup with a `NumberFormatException`. Removing that line breaks cart, user and product services.
- **The frontend is deployed** (`k8s/frontend.yaml`) on port 3000 and talks to the real services.
- **Secrets are dev values** committed in `k8s/services.yaml`, matching docker-compose. `jwt-secret`
  must stay identical between user-service and cart-service.
- **Databases run in-cluster with PVCs** for MySQL and MongoDB. Redis has no volume on purpose —
  carts are a 24h-TTL cache. For anything beyond local use, point at managed databases and delete
  `k8s/databases.yaml`.

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

3. **Deploy to EKS or ECS**
   - The manifests in `k8s/` are the closest starting point: swap `image:` for ECR references,
     delete `k8s/databases.yaml` in favour of RDS/DocumentDB/ElastiCache, and replace the
     `LoadBalancer` services with an Ingress.
   - Move `ecommerce-secrets` out of the manifest into Secrets Manager or SSM.

None of this AWS section has been executed — unlike the local Kubernetes path, it is untested.

## Security

What is actually enforced:

- **BCrypt password hashing**; responses never include the hash.
- **JWT authentication.** user-service signs tokens (HS256, 24h) carrying the numeric user id.
- **Local token verification in cart-service** using the shared `JWT_SECRET` — no callback to
  user-service, so cart auth survives a user-service outage.
- **Cart ownership.** `@PreAuthorize("#userId == authentication.name")` on every cart endpoint:
  no token → 401, another user's cart → 403.
- **Bean Validation** on every request body, returning field-level 400s.
- **Failed logins return 401** with a message that does not reveal whether the account exists.
- **Parameterized queries** through Spring Data (JPA and MongoDB), so no hand-built SQL.

Known limitations, deliberately not hidden:

- product-service and payment-service are **unauthenticated** — anyone can create or delete products.
- CORS allows any origin (`allowedOriginPatterns("*")`), appropriate for local development only.
- Secrets are committed as dev defaults in `docker-compose.yml` and `k8s/services.yaml`.
- Card details are posted to the payment simulator in plaintext JSON; it is not a real gateway.

## Performance

- Redis-backed carts with a 24h TTL, refreshed on read
- HikariCP connection pooling (Spring Boot default) for MySQL
- Server-side pagination on every catalog listing
- Timeouts on cart → product calls (2s connect, 3s response) so one slow service cannot pin threads
- Multi-stage Docker builds producing JRE-only images that run as a non-root user

## Monitoring and Logging

- **Spring Boot Actuator** on all five services: `/actuator/health` and `/actuator/info`
- Kubernetes startup, readiness and liveness probes read those health endpoints
- Logback at the Spring Boot defaults (plain text, not structured JSON)
- No metrics registry is configured — Prometheus scraping would need `micrometer-registry-prometheus`
  plus exposing the `prometheus` endpoint

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

No license file has been added yet, so default copyright applies.

## Troubleshooting

### Common Issues

1. **Port conflicts** — ports 3000, 3306, 6379, 8081-8085 and 27017 must be free. Compose and the
   Kubernetes cluster both bind the same ports, so run one at a time.
2. **Every cart request returns 401** — `JWT_SECRET` differs between user-service and cart-service.
   They must match exactly; cart-service verifies the token locally.
3. **Cart appears to reset on every request** — the Redis `ObjectMapper` must keep
   `FAIL_ON_UNKNOWN_PROPERTIES` disabled. `Cart` serializes computed getters it cannot read back,
   and the failure is swallowed as "empty cart". `CartSerializationTest` guards this.
4. **Pods crash with `NumberFormatException: For input string: "tcp://10.96..."`** — a pod is missing
   `enableServiceLinks: false`. Kubernetes injects `REDIS_PORT`/`MYSQL_PORT` variables that collide
   with the app's own placeholders.
5. **Images tagged `<service>ocal:latest`** — zsh expanded an unquoted `$s:local` (`:l` is its
   lowercase modifier). Quote the tag: `docker build -t "ecommerce/${s}:local"`.
6. **`mvn` fails on Java version** — `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.
7. **Memory** — five JVMs plus three databases need roughly 4GB of Docker memory.

### Logs

Docker Compose:

```bash
docker-compose logs -f user-service
docker-compose logs -f
```

Kubernetes:

```bash
kubectl logs -f deploy/order-service
kubectl describe pod -l app=order-service    # why a pod is not ready
./cluster.sh status
```

## Support

Open an issue at
[github.com/harshwadhawe/Ecommerce-Microservice-Project/issues](https://github.com/harshwadhawe/Ecommerce-Microservice-Project/issues).