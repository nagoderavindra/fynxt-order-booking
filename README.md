# FYNXT Order Booking & Portfolio API

A backend REST API for a concurrent stock trading desk that supports order booking, order lifecycle management, portfolio management, and sector overlap analysis.

The application is built using **Java 17, Spring Boot 3.5.4, Spring Data JPA, and MySQL**.

---

## 1. Project Overview

The application provides APIs to:

* Place BUY and SELL orders
* Enforce a maximum of 3 pending orders per trader
* Validate SELL orders against available portfolio holdings
* Fill pending orders
* Cancel pending orders
* Maintain trader portfolio holdings
* Calculate sector-wise portfolio breakdown
* Calculate portfolio overlap against predefined stock baskets
* Handle concurrent order requests safely
* Provide validation and centralized error handling

The application is designed with a focus on **clean architecture, concurrency safety, maintainability, and production-oriented practices**.

---

## 2. Technology Stack

| Technology      | Version / Usage    |
| --------------- | ------------------ |
| Java            | 17                 |
| Spring Boot     | 3.5.4              |
| Spring Data JPA | Database access    |
| Hibernate       | ORM                |
| MySQL           | 8.x                |
| Maven           | Build tool         |
| JUnit 5         | Unit testing       |
| Mockito         | Mock-based testing |
| REST API        | API communication  |
| Git / GitHub    | Version control    |

---

## 3. Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.fynxt
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       └── service
│   │           ├── impl
│   │           └── SectorOverlapCalculator.java
│   │
│   └── resources
│       ├── application.properties
│       └── schema.sql
│
└── test
    └── java
        └── com.fynxt
            ├── OrderServiceImplTest
            ├── PortfolioServiceImplTest
            ├── SectorOverlapCalculatorTest
            ├── SectorOverlapServiceImplTest
            └── OrderConcurrencyTest
```

---

## 4. Core Features

### Order Booking

A trader can place a BUY or SELL order.

Example:

```json
{
  "traderId": "T001",
  "stock": "AAPL",
  "sector": "TECH",
  "quantity": 50,
  "side": "BUY"
}
```

---

### Order Lifecycle

Orders follow the lifecycle:

```text
PENDING
   ├──> FILLED
   └──> CANCELLED
```

Only pending orders can be filled or cancelled.

---

### Pending Order Limit

Each trader can have a maximum of **3 pending orders**.

If the trader already has 3 pending orders, a new order is rejected.

---

### SELL Validation

A SELL order is accepted only when the trader has sufficient available holdings.

Available quantity is calculated as:

```text
Available Quantity =
Portfolio Quantity - Reserved Quantity
```

This prevents multiple pending SELL orders from overselling the same stock.

---

## 5. API Endpoints

### 1. Place Order

```http
POST /api/orders
```

Request:

```json
{
  "traderId": "T001",
  "stock": "AAPL",
  "sector": "TECH",
  "quantity": 50,
  "side": "BUY"
}
```

---

### 2. Fill Order

```http
POST /api/orders/{orderId}/fill
```

Only `PENDING` orders can be filled.

For BUY:

```text
Holding = Holding + Order Quantity
```

For SELL:

```text
Holding = Holding - Order Quantity
```

---

### 3. Cancel Order

```http
POST /api/orders/{orderId}/cancel
```

Only `PENDING` orders can be cancelled.

For pending SELL orders, the reserved quantity is released when the order is cancelled.

---

### 4. Get Portfolio

```http
GET /api/traders/{traderId}/portfolio
```

Example response:

```json
{
  "traderId": "T001",
  "positions": {
    "AAPL": 90,
    "MSFT": 50
  },
  "sectorBreakdown": {
    "TECH": 140
  }
}
```

---

### 5. Sector Overlap

```http
GET /api/traders/{traderId}/sector-overlap
```

The API calculates portfolio overlap against predefined baskets.

### Baskets

#### TECH_HEAVY

```text
AAPL, MSFT, GOOGL, TSLA, NVDA
```

#### FINANCE_HEAVY

```text
JPM, GS, BAC, MS, WFC
```

#### BALANCED

```text
AAPL, JPM, XOM, JNJ, TSLA
```

Overlap formula:

```text
Overlap % =
(2 × Number of Common Stocks)
/
(Number of Portfolio Stocks + Number of Basket Stocks)
× 100
```

Risk levels:

```text
Any overlap >= 60%  → HIGH
Any overlap >= 40%  → MEDIUM
All overlaps < 40%  → LOW
```

The basket with the highest overlap is returned as the dominant basket.

---

### 6. Add Holdings

```http
POST /api/traders/{traderId}/portfolio/holdings
```

Request:

```json
{
  "stock": "AAPL",
  "sector": "TECH",
  "quantity": 100
}
```

This endpoint is useful for initializing or updating portfolio holdings.

---

## 6. Database Design

The application uses three primary tables.

### Traders

```text
traders
---------
id
name
created_at
```

### Portfolio Holdings

```text
portfolio_holdings
------------------
id
trader_id
stock
sector
quantity
reserved_quantity
created_at
updated_at
```

A unique constraint is applied to:

```text
(trader_id, stock)
```

This ensures that a trader has only one holding record per stock.

### Orders

```text
orders
------
id
trader_id
stock
sector
quantity
side
status
created_at
updated_at
```

Database constraints are used for:

* Positive order quantity
* Valid BUY / SELL side
* Valid order status
* Non-negative portfolio quantity
* Non-negative reserved quantity
* Reserved quantity cannot exceed portfolio quantity

---

## 7. Concurrency Handling

Concurrency is an important part of the assignment.

For example, if multiple requests try to place orders for the same trader at the same time, simply checking the pending order count is not sufficient.

The application uses **pessimistic database locking**.

The trader row is locked before checking the pending order count:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

This serializes concurrent modifications for the same trader.

The same approach is used when updating orders and portfolio holdings.

### SELL Reservation

For SELL orders, available holdings are protected using:

```text
Available =
Quantity - Reserved Quantity
```

When a SELL order is placed:

```text
Reserved Quantity += Order Quantity
```

When the order is filled:

```text
Quantity -= Order Quantity
Reserved Quantity -= Order Quantity
```

When the order is cancelled:

```text
Reserved Quantity -= Order Quantity
```

This prevents concurrent SELL orders from selling more shares than are available.

---

## 8. Transaction Management

Business operations are executed inside database transactions using Spring's `@Transactional`.

This ensures that related operations such as:

```text
Validate order
      ↓
Reserve holdings
      ↓
Create order
```

are handled atomically.

Similarly, filling a SELL order updates both the portfolio quantity and reserved quantity within the same transaction.

---

## 9. Error Handling

The application uses centralized exception handling with `@RestControllerAdvice`.

Handled scenarios include:

* Trader not found
* Order not found
* Order is not pending
* Maximum pending order limit reached
* Insufficient holdings for SELL
* Invalid portfolio sector
* Invalid request fields
* Unexpected server errors

Example error response:

```json
{
  "timestamp": "2026-10-07T10:30:00",
  "status": 400,
  "error": "Business Error",
  "message": "Insufficient available holdings for SELL order",
  "path": "/api/orders"
}
```

---

## 10. Input Validation

Bean Validation is used for API request validation.

Examples:

```text
@NotBlank
@NotNull
@Positive
```

This prevents invalid requests such as:

* Empty trader ID
* Empty stock
* Missing quantity
* Zero quantity
* Negative quantity
* Missing BUY / SELL side

---

## 11. Testing

The project includes unit and integration-style tests covering the main business scenarios.

### Order Service Tests

Test cases include:

* Successful BUY order
* Successful SELL order
* Insufficient holdings
* Maximum pending order limit
* Trader not found
* Successful order fill
* Fill non-pending order
* Successful cancellation
* Cancel non-pending order

### Portfolio Tests

Test cases include:

* Adding new holding
* Increasing existing holding
* Portfolio retrieval
* Sector breakdown
* Trader validation
* Sector mismatch validation
* Invalid quantity scenarios

### Sector Overlap Tests

Tests cover:

* TECH_HEAVY overlap
* FINANCE_HEAVY overlap
* BALANCED overlap
* HIGH risk
* MEDIUM risk
* LOW risk

### Concurrency Test

A concurrency integration test sends multiple simultaneous order requests for the same trader.

The test verifies that even with concurrent requests, the application does not allow more than **3 pending orders**.

Database pessimistic locking is used to protect this business rule.

---

## 12. How to Run the Application

### Prerequisites

Install:

* Java 17
* Maven
* MySQL 8.x

### Create Database

Create the database:

```sql
CREATE DATABASE fynxt_order_booking;
```

The application automatically executes `schema.sql` during startup.

### Configure Database

Configure the following environment variables:

```text
DB_USERNAME
DB_PASSWORD
```

Example:

```text
DB_USERNAME=root
DB_PASSWORD=your_password
```

### Run Using Maven

```bash
mvn spring-boot:run
```

Or run:

```text
FynxtOrderBookingApplication
```

from IntelliJ IDEA.

Application runs on:

```text
http://localhost:8080
```

---

## 13. Example Testing Flow

A simple testing flow is:

```text
1. Add initial AAPL holding
        ↓
2. Place BUY order
        ↓
3. Place SELL order
        ↓
4. Verify pending order limit
        ↓
5. Fill BUY order
        ↓
6. Fill SELL order
        ↓
7. Check portfolio
        ↓
8. Check sector overlap
        ↓
9. Test cancellation
        ↓
10. Test insufficient SELL
        ↓
11. Test concurrent requests
```

---

## 14. Design Decisions & Trade-offs

### Pessimistic Locking

Pessimistic locking was selected because the assignment has strict concurrent business rules around:

* Pending order limits
* Portfolio holdings
* SELL reservations

The trade-off is that database locks can reduce concurrency for operations involving the same trader, but they provide stronger consistency for these business rules.

### Direct traderId Instead of Entity Relationship

The order and portfolio entities use `traderId` directly rather than maintaining a full JPA relationship.

This keeps the model simple and avoids unnecessary entity loading for this assignment.

### Pure Java Sector Calculator

Sector overlap calculation is implemented as a framework-independent Java component.

This keeps the business logic:

* Easy to test
* Independent of database access
* Easy to maintain
* Reusable

### MySQL

MySQL was selected as the relational database because the assignment requires relational persistence and MySQL provides the required transactional and locking capabilities.

---

## 15. SOLID / Clean Code Principles

The project follows practical clean-code principles:

* Controllers handle HTTP requests.
* Services contain business logic.
* Repositories handle database access.
* DTOs separate API contracts from persistence entities.
* Exceptions represent business and resource errors.
* Sector overlap calculation is isolated from framework and database code.
* Common error handling is centralized
16. Future Improvements

For a production-scale system, possible improvements include:

Authentication and authorization
API versioning
OpenAPI / Swagger documentation
Distributed tracing
Metrics and monitoring
Redis caching where appropriate
Kafka-based asynchronous processing
Idempotency keys
Optimistic locking where suitable
CI/CD pipeline
Docker containerization
Kubernetes deployment
Rate limiting

These were kept outside the core assignment scope to maintain a focused implementation.

17. Conclusion

This project demonstrates a production-oriented approach to building a concurrent order booking and portfolio management API using Spring Boot.

The implementation focuses on:

REST API Design
      +
Business Rules
      +
Database Constraints
      +
Transactions
      +
Concurrency Control
      +
Validation
      +
Error Handling
      +
Testing

The main design goal is to maintain correctness under concurrent requests while keeping the code simple, testable, and maintainable.
