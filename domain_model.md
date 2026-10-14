# Book Worm Platform — Domain Model Specification

## 1. Overview & DDD Strategic Design

This document specifies the Domain Model for the **Book Worm** E-Commerce platform using Domain-Driven Design (DDD) principles. It bridges the architectural wireframes and functional requirements into domain aggregates, entities, value objects, domain events, and state machines ahead of implementation.

---

## 2. Bounded Contexts & Aggregate Boundaries

```mermaid
flowchart TD
    subgraph MemberContext ["1. Member & Identity Context"]
        UserAgg["User Aggregate (AR: User)"]
        AddressVO["Address Entity/VO"]
        WishlistAgg["Wishlist Aggregate (AR: Wishlist)"]
    end

    subgraph StoreContext ["2. Store Governance Context"]
        StoreAgg["Store Aggregate (AR: Store)"]
        StorePolicyVO["StorePolicy Entity/VO"]
    end

    subgraph CatalogContext ["3. Catalog & Discovery Context"]
        BookAgg["Book Aggregate (AR: Book)"]
        AuthorAgg["Author Aggregate (AR: Author)"]
        CategoryAgg["Category Aggregate (AR: Category)"]
        ReviewAgg["Review Aggregate (AR: Review)"]
    end

    subgraph CartContext ["4. Cart Context"]
        CartAgg["Cart Aggregate (AR: Cart)"]
        CartItemVO["CartItem Entity"]
    end

    subgraph OrderContext ["5. Order Context"]
        OrderAgg["Order Aggregate (AR: Order)"]
        OrderItemVO["OrderItem Value Object"]
        StatusHistoryVO["OrderStatusHistory Entity"]
    end

    subgraph PaymentContext ["6. Payment & Wallet Context"]
        PaymentTxAgg["PaymentTransaction Aggregate"]
        WalletAgg["Wallet Aggregate (AR: WalletAccount)"]
        RefundAgg["RefundTransaction Aggregate"]
    end

    subgraph ShippingContext ["7. Shipping & Fulfillment Context"]
        ShipmentAgg["Shipment Aggregate (AR: Shipment)"]
        TrackingEventVO["TrackingEvent Entity"]
    end

    UserAgg -->|Places| OrderAgg
    UserAgg -->|Owns| CartAgg
    UserAgg -->|Manages| WishlistAgg
    StoreAgg -->|Configures Rules| OrderAgg
    BookAgg -->|Referenced by SKU/Price| OrderItemVO
    BookAgg -->|Added to| CartItemVO
    OrderAgg -->|Triggers Settlement| PaymentTxAgg
    OrderAgg -->|Dispatches| ShipmentAgg
```

---

## 3. Aggregate Definitions & Invariants

### 3.1 `MemberContext`
#### Aggregate Root: `User`
- **Entities / VOs**: `Address`, `Role` (ENUM), `UserCredentials`
- **Invariants**:
  - `email` must be unique and valid.
  - Passwords must be hashed; raw text is never persisted.
  - A user can have multiple addresses, but at most one `is_default = true`.
  - Roles: `ROLE_GUEST`, `ROLE_MEMBER`, `ROLE_STORE_ADMIN`, `ROLE_CATALOG_ADMIN`, `ROLE_CSR`, `ROLE_FULFILLMENT`.

#### Aggregate Root: `Wishlist`
- **Entities / VOs**: `WishlistItem` (`user_id`, `book_id`, `added_at`)
- **Invariants**:
  - A user cannot add duplicate book entries to their wishlist (unique composite constraint on `(user_id, book_id)`).

---

### 3.2 `StoreContext`
#### Aggregate Root: `Store`
- **Entities / VOs**: `StorePolicy`
- **Invariants**:
  - Store must specify base currency (default: `INR`).
  - Store policies encapsulate provisional/configurable parameters:
    - `cancellation_window_hours`: Default 48 hours (Configurable).
    - `tax_rate_percent`: Provisional default ~12.00% (Configurable).
    - `free_delivery_threshold`: Provisional default ₹500.00 (Configurable).
    - `standard_delivery_fee`: Provisional default ₹50.00 (Configurable).
    - `loyalty_redemption_rate`: Provisional default 1 point = ₹1.00 (Configurable).
    - `return_window_days`: Provisional default 7 days (Configurable).

---

### 3.3 `CatalogContext`
#### Aggregate Root: `Book`
- **Entities / VOs**: `Author`, `Publisher`, `Category`, `BookFormat` (ENUM: `PAPERBACK`, `HARDCOVER`, `EBOOK`), `Money` (Price, Currency)
- **Invariants**:
  - `price` must be $\ge 0$.
  - `stock_quantity` must be $\ge 0$.
  - Category must belong to one of the 20 taxonomy categories.
  - `average_rating` is computed from approved reviews ($1.0 \le \text{avg} \le 5.0$).

#### Aggregate Root: `Review`
- **Entities / VOs**: `Rating` (1 to 5), `ReviewText`
- **Invariants**:
  - `rating` must be an integer between 1 and 5.
  - `review_text` must not exceed **100 characters** (`@Size(max=100)`).
  - Unauthenticated users cannot submit reviews.

---

### 3.4 `CartContext`
#### Aggregate Root: `Cart`
- **Entities / VOs**: `CartItem` (`book_id`, `quantity`, `unit_price`, `tentative_delivery_date`)
- **Invariants**:
  - `quantity` for any line item must be $\ge 1$.
  - Guest carts are tracked via `session_token`.
  - When a guest logs in, their cart is merged into their member account cart.
  - If a duplicate book is added during cart merge, quantities are summed up (capped at available inventory).

---

### 3.5 `OrderContext`
#### Aggregate Root: `Order`
- **Entities / VOs**: `OrderItem`, `OrderStatusHistory`, `OrderPricing` (Subtotal, Tax, Shipping, Discount, Total), `DeliveryAddress` (immutable snapshot)
- **Invariants**:
  - An order must contain at least one `OrderItem`.
  - Prices in `OrderItem` are immutable snapshots at the moment of checkout.
  - `total_amount = subtotal + tax_amount + shipping_charge - discount_amount`.
  - **Cancellation Rule**: Order can be cancelled if `elapsed_time <= cancellation_window_hours` AND `status IN ('CREATED', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING')`.
  - Transition to `CONFIRMED` requires verified successful payment.

---

### 3.6 `PaymentContext`
#### Aggregate Root: `PaymentTransaction`
- **Entities / VOs**: `PaymentMethod` (ENUM: `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `WALLET`), `TransactionStatus` (ENUM: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`), `GatewayReference`
- **Invariants**:
  - Card details are tokenized; raw card number and CVV are never stored.
  - `amount` must equal the payable amount of the associated order.
  - Every transaction has an immutable audit log.

#### Aggregate Root: `WalletAccount`
- **Entities / VOs**: `WalletTransaction` (Credit/Debit entries)
- **Invariants**:
  - Wallet balance cannot drop below zero.
  - Points/Wallet deductions are logged with debit transactions linking to the respective `order_id`.

---

### 3.7 `ShippingContext`
#### Aggregate Root: `Shipment`
- **Entities / VOs**: `TrackingEvent` (`status`, `location`, `timestamp`), `EstimatedDeliveryDate`
- **Invariants**:
  - An order shipment is created upon order confirmation.
  - `tentative_delivery_date` is computed using destination PIN code and warehouse dispatch SLA.

---

## 4. Entity-Relationship (ER) Schema Design

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : "has"
    USERS ||--o{ WISHLISTS : "owns"
    USERS ||--o{ REVIEWS : "writes"
    USERS ||--o{ ORDERS : "places"
    USERS ||--o| WALLET_ACCOUNTS : "owns"

    STORES ||--o{ STORE_POLICIES : "defines"
    
    CATEGORIES ||--o{ BOOKS : "classifies"
    AUTHORS ||--o{ BOOKS : "writes"
    PUBLISHERS ||--o{ BOOKS : "publishes"
    BOOKS ||--o{ REVIEWS : "receives"
    BOOKS ||--o{ WISHLISTS : "saved_in"
    BOOKS ||--o{ CART_ITEMS : "added_to"
    BOOKS ||--o{ ORDER_ITEMS : "included_in"

    CARTS ||--o{ CART_ITEMS : "contains"
    
    ORDERS ||--o{ ORDER_ITEMS : "contains"
    ORDERS ||--o{ ORDER_STATUS_HISTORIES : "tracks"
    ORDERS ||--o{ PAYMENT_TRANSACTIONS : "settled_by"
    ORDERS ||--o| SHIPMENTS : "dispatched_via"
    
    WALLET_ACCOUNTS ||--o{ WALLET_TRANSACTIONS : "logs"
    SHIPMENTS ||--o{ TRACKING_EVENTS : "records"

    USERS {
        UUID user_id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR phone
        VARCHAR role
        TIMESTAMP created_at
    }

    ADDRESSES {
        UUID address_id PK
        UUID user_id FK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR address_line
        VARCHAR city
        VARCHAR state
        VARCHAR country
        VARCHAR pin_code
        VARCHAR phone_number
        BOOLEAN is_default
    }

    CATEGORIES {
        BIGSERIAL category_id PK
        VARCHAR name UK
        VARCHAR slug UK
        INT display_order
    }

    AUTHORS {
        UUID author_id PK
        VARCHAR name
        TEXT bio
        VARCHAR avatar_url
    }

    PUBLISHERS {
        UUID publisher_id PK
        VARCHAR name
    }

    BOOKS {
        UUID book_id PK
        VARCHAR title
        VARCHAR isbn UK
        UUID author_id FK
        UUID publisher_id FK
        BIGINT category_id FK
        VARCHAR format
        NUMERIC price
        VARCHAR language
        VARCHAR cover_image_url
        TEXT description
        INT sales_count
        NUMERIC average_rating
        INT rating_count
        INT stock_quantity
        TIMESTAMP created_at
    }

    REVIEWS {
        UUID review_id PK
        UUID book_id FK
        UUID user_id FK
        VARCHAR user_name
        INT rating
        VARCHAR review_text
        TIMESTAMP created_at
    }

    CARTS {
        UUID cart_id PK
        UUID user_id FK "nullable"
        VARCHAR session_token "nullable"
        TIMESTAMP updated_at
    }

    CART_ITEMS {
        UUID cart_item_id PK
        UUID cart_id FK
        UUID book_id FK
        INT quantity
        NUMERIC unit_price
    }

    ORDERS {
        UUID order_id PK
        VARCHAR order_number UK
        UUID user_id FK
        VARCHAR status
        NUMERIC subtotal
        NUMERIC tax_amount
        NUMERIC shipping_charge
        NUMERIC discount_amount
        NUMERIC total_amount
        UUID shipping_address_id FK
        DATE tentative_delivery_date
        TIMESTAMP created_at
        TIMESTAMP cancelled_at
    }

    ORDER_ITEMS {
        UUID order_item_id PK
        UUID order_id FK
        UUID book_id FK
        VARCHAR title
        VARCHAR format
        INT quantity
        NUMERIC unit_price
        NUMERIC total_price
    }

    ORDER_STATUS_HISTORIES {
        UUID history_id PK
        UUID order_id FK
        VARCHAR from_status
        VARCHAR to_status
        TEXT remarks
        TIMESTAMP changed_at
    }

    PAYMENT_TRANSACTIONS {
        UUID transaction_id PK
        UUID order_id FK
        VARCHAR payment_method
        NUMERIC amount
        VARCHAR status
        VARCHAR gateway_ref_id
        TIMESTAMP created_at
    }

    SHIPMENTS {
        UUID shipment_id PK
        UUID order_id FK
        VARCHAR carrier_name
        VARCHAR tracking_number
        VARCHAR status
        DATE estimated_delivery_date
        TIMESTAMP dispatched_at
        TIMESTAMP delivered_at
    }

    STORE_POLICIES {
        UUID policy_id PK
        UUID store_id FK
        INT cancellation_window_hours
        NUMERIC tax_rate_percent
        NUMERIC free_delivery_threshold
        NUMERIC standard_delivery_fee
        NUMERIC loyalty_redemption_rate
        INT return_window_days
    }
```

---

## 5. Domain State Machines & Lifecycles

### 5.1 Order Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> CREATED: Checkout Initiated
    CREATED --> PENDING_PAYMENT: Payment Session Triggered
    PENDING_PAYMENT --> CONFIRMED: Payment Verified (Success)
    PENDING_PAYMENT --> FAILED: Payment Aborted / Rejected
    FAILED --> PENDING_PAYMENT: Retry Payment
    
    CONFIRMED --> PROCESSING: Warehouse Pick & Pack
    PROCESSING --> SHIPPED: Carrier Dispatched
    SHIPPED --> DELIVERED: Out for Delivery & Delivered
    
    CREATED --> CANCELLED: User Cancels (<= 48h)
    PENDING_PAYMENT --> CANCELLED: User Cancels (<= 48h)
    CONFIRMED --> CANCELLED: User Cancels (<= 48h & Pre-Dispatch)
    PROCESSING --> CANCELLED: User Cancels (<= 48h & Pre-Dispatch)
    
    DELIVERED --> RETURN_REQUESTED: Return Initiated (<= Return Window)
    RETURN_REQUESTED --> RETURNED: Return Received & Inspected
    
    CANCELLED --> [*]
    RETURNED --> [*]
    DELIVERED --> [*]
```

### 5.2 Payment Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> INITIATED: Customer Selects Payment Method
    INITIATED --> AUTHORIZING: Gateway Token / Form Submitted
    AUTHORIZING --> SUCCESS: Gateway Callback / Webhook Verified
    AUTHORIZING --> FAILED: Gateway Rejection / Timeout
    SUCCESS --> REFUND_REQUESTED: Order Cancelled / Returned
    REFUND_REQUESTED --> REFUNDED: Refund Settled
    FAILED --> [*]
    REFUNDED --> [*]
```

---

## 6. Provisional vs. Confirmed Business Parameters

| Parameter | Type / Target | Default Provisional Value | Scope & Configuration Strategy |
| :--- | :--- | :--- | :--- |
| **Tax Rate** | `NUMERIC(5,2)` | `12.00%` | Configured in `STORE_POLICIES.tax_rate_percent` / `application.properties`. Pending final tax law confirmation. |
| **Free Delivery Threshold** | `NUMERIC(10,2)` | `₹500.00` | Configured in `STORE_POLICIES.free_delivery_threshold`. Orders $\ge ₹500$ get `delivery_charges = 0`. |
| **Standard Delivery Charge** | `NUMERIC(10,2)` | `₹50.00` | Configured in `STORE_POLICIES.standard_delivery_fee`. Applied when total $< free\_delivery\_threshold$. |
| **Cancellation Window** | `INT` (Hours) | `48` | Configured in `STORE_POLICIES.cancellation_window_hours`. Customer journey Slide 3 specifies 48 hrs. |
| **Return Window** | `INT` (Days) | `7` | Configured in `STORE_POLICIES.return_window_days`. Allowed post-delivery. Pending confirmation. |
| **Loyalty Points Rate** | `NUMERIC(5,2)` | `1.00` | Configured in `STORE_POLICIES.loyalty_redemption_rate` (1 point = ₹1). Pending confirmation. |
| **Review Max Length** | `INT` (Chars) | `100` | Confirmed by Slide 5 UI wireframe (`0/100` counter). Hard constraint `@Size(max = 100)`. |
| **20-Category Taxonomy** | `List<String>` | Fixed 20 Categories | Confirmed by Slide 5/6 UI wireframe sidebar. Seeded in DB master data. |

---

## 7. Additional Features Requiring Confirmation

The following features were identified during wireframe and flow analysis as helpful additions; they are cleanly separated and marked pending stakeholder confirmation:

1. **Social Login / OAuth2 (Google/GitHub)**: UI shows standard login; OAuth2 can be added as an extension.
2. **Dynamic Tier-Based Loyalty Points (Silver/Gold/Platinum)**: Basic 1:1 points redemption is modeled; tiered accrual requires confirmation.
3. **Multi-Currency Dynamic Conversion**: Backend defaults to INR (`₹`); multi-currency rates (USD, EUR) require conversion service confirmation.
4. **SMS / WhatsApp Gateway Notifications**: Order confirmation is currently modeled via REST receipt and DB record; external SMS integration pending confirmation.
