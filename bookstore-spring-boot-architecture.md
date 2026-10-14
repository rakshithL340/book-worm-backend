# Book Worm Platform — Spring Boot 3 Architecture Specification

This document defines the production-grade **Java 21 & Spring Boot 3.x** backend architecture for the **Book Worm** E-Commerce platform based on the approved OpenAPI 3.1 specification, domain model, and PostgreSQL database schema.

---

## 1. Technology Stack & Framework Matrix

| Component | Technology | Version | Purpose & Rationale |
| :--- | :--- | :--- | :--- |
| **Language Runtime** | Java OpenJDK | `21 (LTS)` | Modern Java features (Virtual Threads, Pattern Matching, Records, Sequenced Collections). |
| **Framework** | Spring Boot | `3.3.x` | Enterprise application framework with Spring Framework 6 and native Jakarta EE 10. |
| **Build Tool** | Apache Maven | `3.9+` | Dependency management, build lifecycle, and code generation plugin execution. |
| **Database** | PostgreSQL | `15+ / 16` | Relational storage with JSONB, UUID, full-text search, and ACID transactional safety. |
| **ORM / Data Access** | Spring Data JPA / Hibernate | `6.5+` | Object-relational mapping, `@SQLRestriction` for soft deletes, `@Version` for optimistic locks. |
| **Database Migrations**| Flyway | `10.x` | Version-controlled, idempotent DDL and DML migrations (`V1__...` through `V7__...`). |
| **Security & Auth** | Spring Security / JJWT | `6.3.x / 0.12.x` | Stateless JWT authentication, RBAC, password hashing with BCrypt. |
| **Code Generation** | OpenAPI Generator Maven Plugin | `7.5.x` | Contract-first API stub generation from `openapi.yaml`. |
| **Object Mapping** | MapStruct | `1.5.5.Final` | Compile-time, type-safe entity-to-DTO and DTO-to-entity mappers (zero runtime overhead). |
| **Boilerplate Reducer**| Project Lombok | `1.18.32` | Getters, setters, builders, and loggers. |
| **Testing & Isolation**| Testcontainers / JUnit 5 | `1.19.x / 5.10.x`| Real PostgreSQL containerized integration testing with MockMvc. |

---

## 2. Layered Architecture & Modular Design

The platform follows a **Modular Monolith** architecture with strict **Domain-Driven Design (DDD)** layered boundaries:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION / WEB LAYER                        │
│   • Spring REST Controllers (@RestController)                          │
│   • OpenAPI Generated Delegate Interfaces                              │
│   • Request Validation (@Valid, JSR-380)                               │
│   • Global Exception Handler (@RestControllerAdvice / ProblemDetail)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Calls
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      APPLICATION / SERVICE LAYER                       │
│   • Transactional Application Services (@Service, @Transactional)      │
│   • Orchestration & Workflow Coordinators                              │
│   • MapStruct DTO Mappers                                              │
│   • Domain Event Publishers                                            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Coordinates
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER                              │
│   • Domain Entities (@Entity) & Aggregate Roots                        │
│   • Value Objects, Enums & Domain Invariants                           │
│   • Domain Business Rules (48h Cancellation, 100-Char Review Limit)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Persists via
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                           │
│   • Spring Data JPA Repositories (@Repository)                         │
│   • Spring Security Filter Chain & JWT Token Providers                 │
│   • Flyway Database Migration Scripts                                  │
│   • Third-Party Gateway Adapters (Payment / Logistics)                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Package Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── bookworm/
│   │           ├── BookWormApplication.java
│   │           │
│   │           ├── common/                         # Cross-cutting concerns & shared infrastructure
│   │           │   ├── config/                     # SecurityConfig, WebConfig, OpenApiConfig, JpaConfig
│   │           │   ├── exception/                  # GlobalExceptionHandler, CustomExceptions, ErrorCodes
│   │           │   ├── model/                      # BaseEntity, PageResponseDto, ProblemDetailFactory
│   │           │   ├── security/                   # JwtTokenProvider, JwtAuthFilter, UserPrincipal
│   │           │   └── util/                       # DateTimeUtils, Constants, HeaderUtils
│   │           │
│   │           ├── auth/                           # Domain: Authentication & Sessions
│   │           │   ├── controller/                 # AuthController
│   │           │   ├── service/                    # AuthService, TokenBlacklistService
│   │           │   ├── dto/                        # RegisterRequest, LoginRequest, AuthResponse
│   │           │   └── mapper/                     # AuthMapper
│   │           │
│   │           ├── user/                           # Domain: Users & Address Book
│   │           │   ├── controller/                 # UserController, AddressController
│   │           │   ├── service/                    # UserService, AddressService
│   │           │   ├── entity/                     # User, Address, UserRole
│   │           │   ├── repository/                 # UserRepository, AddressRepository
│   │           │   ├── dto/                        # UserProfileResponse, AddressDto
│   │           │   └── mapper/                     # UserMapper, AddressMapper
│   │           │
│   │           ├── store/                          # Domain: Store Governance & Policies
│   │           │   ├── controller/                 # StoreAdminController
│   │           │   ├── service/                    # StorePolicyService
│   │           │   ├── entity/                     # Store, StorePolicy
│   │           │   ├── repository/                 # StoreRepository, StorePolicyRepository
│   │           │   └── dto/                        # StorePolicyDto
│   │           │
│   │           ├── catalog/                        # Domain: Categories, Authors, Publishers, Books
│   │           │   ├── controller/                 # CatalogController, BookController, AuthorController, PublisherController
│   │           │   ├── service/                    # BookService, CategoryService, AuthorService, PublisherService
│   │           │   ├── entity/                     # Book, Category, Author, Publisher, BookFormat
│   │           │   ├── repository/                 # BookRepository, CategoryRepository, AuthorRepository, PublisherRepository
│   │           │   ├── dto/                        # BookCardDto, BookDetailDto, CategoryDto, AuthorDto
│   │           │   └── mapper/                     # BookMapper, CategoryMapper, AuthorMapper
│   │           │
│   │           ├── search/                         # Domain: Full-Text Search
│   │           │   ├── controller/                 # SearchController
│   │           │   └── service/                    # BookSearchService, BookSpecification
│   │           │
│   │           ├── recommendation/                 # Domain: Merchandising & Recommendations
│   │           │   ├── controller/                 # RecommendationController
│   │           │   └── service/                    # RecommendationService, RecommendationStrategy
│   │           │
│   │           ├── review/                         # Domain: Reviews & Ratings (100-Char Limit)
│   │           │   ├── controller/                 # ReviewController
│   │           │   ├── service/                    # ReviewService
│   │           │   ├── entity/                     # Review
│   │           │   ├── repository/                 # ReviewRepository
│   │           │   ├── dto/                        # ReviewCreateRequest, ReviewResponse
│   │           │   └── mapper/                     # ReviewMapper
│   │           │
│   │           ├── wishlist/                       # Domain: Customer Wishlists
│   │           │   ├── controller/                 # WishlistController
│   │           │   ├── service/                    # WishlistService
│   │           │   ├── entity/                     # Wishlist, WishlistItem
│   │           │   ├── repository/                 # WishlistRepository, WishlistItemRepository
│   │           │   └── dto/                        # WishlistResponse, WishlistItemDto
│   │           │
│   │           ├── cart/                           # Domain: Shopping Cart (Quantity Steppers)
│   │           │   ├── controller/                 # CartController
│   │           │   ├── service/                    # CartService, CartMergeService
│   │           │   ├── entity/                     # Cart, CartItem
│   │           │   ├── repository/                 # CartRepository, CartItemRepository
│   │           │   ├── dto/                        # CartResponse, CartItemDto, AddToCartDto
│   │           │   └── mapper/                     # CartMapper
│   │           │
│   │           ├── order/                          # Domain: Order Lifecycle & 48h Cancellation
│   │           │   ├── controller/                 # OrderController
│   │           │   ├── service/                    # OrderOrchestratorService, OrderCalculationService, OrderCancellationService
│   │           │   ├── entity/                     # Order, OrderItem, OrderStatus, OrderStatusHistory
│   │           │   ├── repository/                 # OrderRepository, OrderItemRepository, OrderStatusHistoryRepository
│   │           │   ├── dto/                        # OrderCalculateDto, OrderCheckoutDto, OrderDetailDto
│   │           │   └── mapper/                     # OrderMapper
│   │           │
│   │           ├── payment/                        # Domain: Payments & Wallet
│   │           │   ├── controller/                 # PaymentController
│   │           │   ├── service/                    # PaymentService, WalletService
│   │           │   ├── entity/                     # PaymentTransaction, WalletAccount, WalletTransaction, RefundTransaction
│   │           │   ├── repository/                 # PaymentTransactionRepository, WalletAccountRepository, RefundRepository
│   │           │   ├── dto/                        # PaymentInitiateDto, WalletBalanceDto
│   │           │   └── mapper/                     # PaymentMapper
│   │           │
│   │           ├── shipping/                       # Domain: Shipping & Delivery (EDD & Tracking)
│   │           │   ├── controller/                 # ShippingController
│   │           │   ├── service/                    # ShippingService, EddCalculationService
│   │           │   ├── entity/                     # Shipment, TrackingEvent, ShipmentStatus
│   │           │   ├── repository/                 # ShipmentRepository, TrackingEventRepository
│   │           │   ├── dto/                        # EddResponseDto, ShipmentTrackingDto
│   │           │   └── mapper/                     # ShippingMapper
│   │           │
│   │           └── promotion/                      # Domain: Coupons & Loyalty Points
│   │               ├── controller/                 # CouponController, LoyaltyController
│   │               ├── service/                    # CouponService, LoyaltyService
│   │               ├── entity/                     # Coupon, CouponRedemption, LoyaltyAccount, LoyaltyTransaction
│   │               ├── repository/                 # CouponRepository, LoyaltyAccountRepository
│   │               ├── dto/                        # CouponValidationDto, LoyaltyPointsDto
│   │               └── mapper/                     # PromotionMapper
│   │
│   └── resources/
│       ├── application.yml                         # Main Spring Boot configuration
│       ├── application-dev.yml                     # Local development profile
│       ├── application-test.yml                    # Testcontainers profile
│       ├── db/
│       │   └── migration/                          # Flyway SQL migration scripts
│       │       ├── V1__create_users_and_addresses.sql
│       │       ├── V2__create_catalog_and_carts.sql
│       │       ├── V3__create_orders.sql
│       │       ├── V4__create_payments_and_wallets.sql
│       │       ├── V5__create_shipping.sql
│       │       ├── V6__create_reviews.sql
│       │       └── V7__create_coupons_and_loyalty.sql
│       └── openapi/
│           └── openapi.yaml                        # Canonical API Contract
│
└── test/
    └── java/
        └── com/
            └── bookworm/
                ├── BookWormApplicationTests.java
                ├── common/
                │   └── BaseIntegrationTest.java    # Testcontainers PostgreSQL setup
                ├── auth/
                ├── catalog/
                ├── cart/
                ├── order/
                └── payment/
```

---

## 4. Maven Build Configuration (`pom.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.2</version>
        <relativePath/>
    </parent>

    <groupId>com.bookworm</groupId>
    <artifactId>book-worm-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>book-worm-backend</name>
    <description>Book Worm E-Commerce Bookstore Backend Service</description>

    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok.version>1.18.32</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <jjwt.version>0.12.5</jjwt.version>
        <testcontainers.version>1.19.8</testcontainers.version>
        <openapi-generator.version>7.5.0</openapi-generator.version>
        <springdoc.version>2.5.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- ============================================================= -->
        <!-- Spring Boot Starters -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ============================================================= -->
        <!-- Database & Migrations -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- ============================================================= -->
        <!-- Security & JWT -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- ============================================================= -->
        <!-- OpenAPI UI & Docs -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openapitools</groupId>
            <artifactId>jackson-databind-nullable</artifactId>
            <version>0.2.6</version>
        </dependency>

        <!-- ============================================================= -->
        <!-- Code Generation & Utilities -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- ============================================================= -->
        <!-- Testing Dependencies -->
        <!-- ============================================================= -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler Plugin with Lombok & MapStruct Integration -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Spring Boot Packaging Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- OpenAPI Generator Maven Plugin -->
            <plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
                <version>${openapi-generator.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <inputSpec>${project.basedir}/openapi.yaml</inputSpec>
                            <generatorName>spring</generatorName>
                            <apiPackage>com.bookworm.generated.api</apiPackage>
                            <modelPackage>com.bookworm.generated.dto</modelPackage>
                            <configOptions>
                                <interfaceOnly>true</interfaceOnly>
                                <useSpringBoot3>true</useSpringBoot3>
                                <useJakartaEe>true</useJakartaEe>
                                <openApiNullable>false</openApiNullable>
                                <skipDefaultInterface>true</skipDefaultInterface>
                                <generateConstructorWithAllArgs>true</generateConstructorWithAllArgs>
                            </configOptions>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 5. Architectural Decision Records (ADRs)

### ADR-01: Contract-First API Development
- **Context**: The REST API contract is formally defined in `openapi.yaml`.
- **Decision**: Use `openapi-generator-maven-plugin` configured with `interfaceOnly=true` and `useSpringBoot3=true`.
- **Rationale**: Generates Jakarta-compliant API interfaces and DTOs during build time, ensuring compile-time safety and preventing controller implementation drift.

### ADR-02: Spring Boot 3 RFC 7807 Exception Architecture
- **Context**: APIs must return standard error envelopes (`application/problem+json`).
- **Decision**: Implement a centralized `@RestControllerAdvice` leveraging Spring Boot 3's native `org.springframework.http.ProblemDetail`.
- **Rationale**: Replaces custom legacy error wrappers with standards-compliant problem details that include custom error codes and validation field details.

### ADR-03: Soft Delete Pattern via Hibernate 6
- **Context**: Domain entities must support soft deletion (`is_deleted`, `deleted_at`) without corrupting unique constraints or requiring manual query filtering.
- **Decision**: Use `@SQLDelete(sql = "UPDATE table_name SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")` and `@SQLRestriction("is_deleted = false")`.
- **Rationale**: Hibernate automatically applies soft delete filters to all JPA repository queries (`findById`, `findAll`) transparently.

### ADR-04: Optimistic Locking with `@Version`
- **Context**: Concurrent inventory adjustments, cart modifications, and order status updates must prevent lost updates.
- **Decision**: Include a `@Version private Long version;` field on all mutable root entities.
- **Rationale**: Catches concurrent collisions and throws `OptimisticLockingFailureException`, translated to `409 Conflict`.

### ADR-05: Database Versioning with Flyway
- **Context**: Database schema must be repeatable, idempotent, and tracked in version control.
- **Decision**: Map the 7 generated SQL migration scripts to standard Flyway naming in `src/main/resources/db/migration/`:
  - `V1__create_users_and_addresses.sql`
  - `V2__create_catalog_and_carts.sql`
  - `V3__create_orders.sql`
  - `V4__create_payments_and_wallets.sql`
  - `V5__create_shipping.sql`
  - `V6__create_reviews.sql`
  - `V7__create_coupons_and_loyalty.sql`
- **Rationale**: Automatic database initialization on application startup across local dev, CI/CD, and production container environments.

### ADR-06: Stateless JWT Authentication & Guest Session Handling
- **Context**: Platform serves both unauthenticated Guest users and authenticated Members.
- **Decision**: Use Spring Security with a custom `JwtAuthenticationFilter` verifying Bearer tokens. For guest users, extract `X-Session-Token` headers and associate with transient carts.
- **Rationale**: Complete statelessness enables seamless horizontal scaling in Kubernetes/OpenShift (ROKS/ROSA).

### ADR-07: Integration Testing with Testcontainers
- **Context**: Integration tests must execute against real PostgreSQL rather than in-memory H2 to test database-specific features (UUID generation, partial unique indexes, trigram search).
- **Decision**: Base integration test class spins up a dynamic PostgreSQL Testcontainer (`@Container static PostgreSQLContainer`).
- **Rationale**: Eliminates environment parity issues between development and production.
