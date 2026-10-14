# Book Worm Platform — REST API Architecture & Interface Specification

This document defines the comprehensive REST API design for the **Book Worm** E-Commerce platform. It specifies endpoint URI contracts, request/response Data Transfer Objects (DTOs), validation constraints, and error codes across all 15 functional domains.

---

## 1. Global API Standards & Conventions

### 1.1 Protocol & Base Paths
- **Protocol**: HTTPS (TLS 1.3 enforced)
- **Base URI Path**: `/api/v1`
- **Content-Type**: `application/json; charset=utf-8`

### 1.2 Authentication & Headers
- **Bearer Token**: `Authorization: Bearer <JWT_ACCESS_TOKEN>` for authenticated member/admin endpoints.
- **Guest Session Header**: `X-Session-Token: <UUID_STRING>` for unauthenticated guest cart and browsing operations.
- **Idempotency Key**: `Idempotency-Key: <UUID_STRING>` required on sensitive write operations (`/orders/checkout`, `/payments/initiate`).

### 1.3 Standard Error Envelope
All error responses adhere to RFC 7807 (Problem Details for HTTP APIs):
```json
{
  "timestamp": "2025-07-21T10:30:00Z",
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Invalid request payload",
  "path": "/api/v1/books/reviews",
  "errors": [
    {
      "field": "reviewText",
      "rejectedValue": "This review text is longer than the allowed maximum character limit...",
      "message": "Review text must not exceed 100 characters"
    }
  ]
}
```

---

## 2. API Domain Specifications

---

### Domain 1: Authentication (`/api/v1/auth`)

#### 1.1 Register User
- **Resource Name**: User Registration
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/register`
- **Request DTO (`RegisterRequest`)**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+919876543210"
  }
  ```
- **Response DTO (`AuthResponse` - `201 Created`)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Smith",
      "role": "ROLE_MEMBER"
    }
  }
  ```
- **Validation Rules**:
  - `email`: `@NotBlank`, `@Email`, Max 255 chars
  - `password`: `@NotBlank`, Min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
  - `firstName`, `lastName`: `@NotBlank`, Max 100 chars
  - `phone`: `@Pattern(regexp = "^\\+?[0-9]{10,15}$")`
- **Error Codes**:
  - `400 Bad Request`: `VALIDATION_FAILED`
  - `409 Conflict`: `AUTH_EMAIL_ALREADY_EXISTS`

#### 1.2 User Login
- **Resource Name**: User Authentication
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/login`
- **Request DTO (`LoginRequest`)**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "guestSessionToken": "d4e2a1b0-8c9f-4e3a-9f1a-5b2c3d4e5f6a"
  }
  ```
- **Response DTO (`AuthResponse` - `200 OK`)**: Same as `AuthResponse` with active JWT access token.
- **Validation Rules**: `email`: `@NotBlank`, `@Email`; `password`: `@NotBlank`
- **Error Codes**:
  - `401 Unauthorized`: `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_DISABLED`

#### 1.3 User Logout
- **Resource Name**: Invalidate Session
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/logout`
- **Request DTO**: Empty body (Authorized Bearer Token)
- **Response DTO (`200 OK`)**: `{"message": "Logged out successfully"}`
- **Error Codes**: `401 Unauthorized`: `AUTH_UNAUTHORIZED`

---

### Domain 2: Users & Addresses (`/api/v1/users`)

#### 2.1 Get Current User Profile
- **Resource Name**: Customer Profile
- **HTTP Method**: `GET`
- **URI**: `/api/v1/users/profile`
- **Response DTO (`UserProfileResponse` - `200 OK`)**:
  ```json
  {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+919876543210",
    "role": "ROLE_MEMBER"
  }
  ```
- **Error Codes**: `401 Unauthorized`: `AUTH_UNAUTHORIZED`, `404 Not Found`: `USER_NOT_FOUND`

#### 2.2 List User Addresses
- **Resource Name**: Address Book
- **HTTP Method**: `GET`
- **URI**: `/api/v1/users/addresses`
- **Response DTO (`List<AddressResponse>` - `200 OK`)**:
  ```json
  [
    {
      "addressId": "c71a3b52-19e4-42b7-a392-f01e23a45c67",
      "firstName": "John",
      "lastName": "Smith",
      "addressLine1": "Flat 402, Sunshine Heights",
      "addressLine2": "MG Road",
      "city": "Bengaluru",
      "state": "Karnataka",
      "country": "India",
      "pinCode": "560001",
      "phoneNumber": "+919876543210",
      "isDefault": true
    }
  ]
  ```

#### 2.3 Add New Address
- **Resource Name**: Create Address
- **HTTP Method**: `POST`
- **URI**: `/api/v1/users/addresses`
- **Request DTO (`AddressCreateRequest`)**:
  ```json
  {
    "firstName": "John",
    "lastName": "Smith",
    "addressLine1": "Flat 402, Sunshine Heights",
    "addressLine2": "MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "country": "India",
    "pinCode": "560001",
    "phoneNumber": "+919876543210",
    "isDefault": true
  }
  ```
- **Response DTO (`AddressResponse` - `201 Created`)**
- **Validation Rules**:
  - `firstName`, `lastName`, `addressLine1`, `city`, `state`: `@NotBlank`, Max 100 chars
  - `pinCode`: `@NotBlank`, `@Pattern(regexp = "^[1-9][0-9]{5}$")` (6-digit Indian PIN)
  - `phoneNumber`: `@NotBlank`, `@Pattern(regexp = "^\\+?[0-9]{10,15}$")`
- **Error Codes**: `400 Bad Request`: `VALIDATION_FAILED`

#### 2.4 Delete Address
- **Resource Name**: Delete Address
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/users/addresses/{addressId}`
- **Response DTO (`204 No Content`)**
- **Error Codes**: `404 Not Found`: `ADDRESS_NOT_FOUND`

---

### Domain 3: Authors (`/api/v1/authors`)

#### 3.1 List Authors
- **Resource Name**: Authors Directory
- **HTTP Method**: `GET`
- **URI**: `/api/v1/authors?page=0&size=20`
- **Response DTO (`PagedResponse<AuthorSummaryResponse>` - `200 OK`)**:
  ```json
  {
    "content": [
      {
        "authorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "name": "Daniel Reed",
        "avatarUrl": "https://cdn.bookworm.com/authors/daniel-reed.jpg"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

#### 3.2 Get Author Details
- **Resource Name**: Author Profile
- **HTTP Method**: `GET`
- **URI**: `/api/v1/authors/{authorId}`
- **Response DTO (`AuthorDetailResponse` - `200 OK`)**:
  ```json
  {
    "authorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "name": "Daniel Reed",
    "bio": "Daniel Reed is a writer, minimalist, and productivity coach based in San Francisco...",
    "avatarUrl": "https://cdn.bookworm.com/authors/daniel-reed.jpg",
    "books": [
      {
        "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "title": "Joy of Minimalism",
        "price": 149.00,
        "format": "PAPERBACK",
        "coverImageUrl": "https://cdn.bookworm.com/covers/minimalism.jpg"
      }
    ]
  }
  ```
- **Error Codes**: `404 Not Found`: `AUTHOR_NOT_FOUND`

---

### Domain 4: Publishers (`/api/v1/publishers`)

#### 4.1 List Publishers
- **Resource Name**: Publisher Catalog
- **HTTP Method**: `GET`
- **URI**: `/api/v1/publishers`
- **Response DTO (`List<PublisherResponse>` - `200 OK`)**:
  ```json
  [
    {
      "publisherId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
      "name": "ABC Publishers"
    }
  ]
  ```

---

### Domain 5: Categories (`/api/v1/categories`)

#### 5.1 List All Categories (20 Taxonomy Categories)
- **Resource Name**: Categories Navigation
- **HTTP Method**: `GET`
- **URI**: `/api/v1/categories`
- **Response DTO (`List<CategoryResponse>` - `200 OK`)**:
  ```json
  [
    {
      "categoryId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "name": "Self-help",
      "slug": "self-help",
      "displayOrder": 7
    },
    {
      "categoryId": "c2a3b4c5-d6e7-8f9a-0b1c-2d3e4f5a6b7c",
      "name": "Science Fiction",
      "slug": "science-fiction",
      "displayOrder": 3
    }
  ]
  ```

---

### Domain 6: Books (`/api/v1/books`)

#### 6.1 Browse / Filter Books
- **Resource Name**: Book Catalog Listing
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books?category=self-help&language=English&format=PAPERBACK&minPrice=100&maxPrice=500&sortBy=relevance&page=0&size=12`
- **Query Parameters**:
  - `category`: String (Slug or Name)
  - `language`: String (e.g. `English`, `Hindi`)
  - `format`: String (`PAPERBACK`, `HARDCOVER`, `EBOOK`)
  - `minPrice`: Decimal ($\ge 0$)
  - `maxPrice`: Decimal ($\ge 0$)
  - `sortBy`: String (`relevance`, `price_asc`, `price_desc`, `rating`, `newest`)
  - `page`: Integer ($\ge 0$, default 0)
  - `size`: Integer ($\ge 1$, default 12)
- **Response DTO (`PagedResponse<BookCardResponse>` - `200 OK`)**:
  ```json
  {
    "content": [
      {
        "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "title": "Joy of Minimalism",
        "authorName": "Daniel Reed",
        "authorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "price": 149.00,
        "format": "PAPERBACK",
        "categoryName": "Self-help",
        "coverImageUrl": "https://cdn.bookworm.com/covers/minimalism.jpg",
        "averageRating": 4.8,
        "tentativeDeliveryDate": "2025-07-21"
      }
    ],
    "page": 0,
    "size": 12,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

#### 6.2 Get Book Details (PDP View)
- **Resource Name**: Product Details Page
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books/{bookId}`
- **Response DTO (`BookDetailResponse` - `200 OK`)**:
  ```json
  {
    "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "title": "Joy of Minimalism",
    "isbn": "978-0143127741",
    "author": {
      "authorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "name": "Daniel Reed",
      "bio": "Daniel Reed is a writer, minimalist, and productivity coach...",
      "avatarUrl": "https://cdn.bookworm.com/authors/daniel-reed.jpg"
    },
    "publisher": {
      "publisherId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
      "name": "ABC Publishers"
    },
    "category": {
      "categoryId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "name": "Self-help",
      "slug": "self-help"
    },
    "format": "PAPERBACK",
    "price": 149.00,
    "language": "English",
    "description": "Discover how less can truly be more. In The Joy of Minimalism...",
    "salesCount": 145,
    "averageRating": 4.8,
    "ratingCount": 24,
    "stockQuantity": 50,
    "coverImageUrl": "https://cdn.bookworm.com/covers/minimalism.jpg",
    "tentativeDeliveryDate": "2025-07-21"
  }
  ```
- **Error Codes**: `404 Not Found`: `BOOK_NOT_FOUND`

---

### Domain 7: Search (`/api/v1/search`)

#### 7.1 Global Keyword Search
- **Resource Name**: Book Search
- **HTTP Method**: `GET`
- **URI**: `/api/v1/search?q=Minimalism&category=self-help&format=PAPERBACK&page=0&size=10`
- **Query Parameters**: `q` (required query string, min 1 char), `category`, `format`, `language`, `minPrice`, `maxPrice`
- **Response DTO (`PagedResponse<BookCardResponse>` - `200 OK`)**
- **Validation Rules**: `q`: `@NotBlank`, Min 1 char, Max 100 chars
- **Error Codes**: `400 Bad Request`: `SEARCH_QUERY_EMPTY`

---

### Domain 8: Wishlist (`/api/v1/wishlist`)

#### 8.1 Get User Wishlist
- **Resource Name**: My Wishlist
- **HTTP Method**: `GET`
- **URI**: `/api/v1/wishlist`
- **Response DTO (`WishlistResponse` - `200 OK`)**:
  ```json
  {
    "wishlistId": "w1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "items": [
      {
        "wishlistItemId": "wi1a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "title": "Joy of Minimalism",
        "authorName": "Daniel Reed",
        "price": 149.00,
        "format": "PAPERBACK",
        "coverImageUrl": "https://cdn.bookworm.com/covers/minimalism.jpg",
        "addedAt": "2025-07-20T12:00:00Z"
      }
    ]
  }
  ```

#### 8.2 Add Book to Wishlist
- **Resource Name**: Save to Wishlist
- **HTTP Method**: `POST`
- **URI**: `/api/v1/wishlist/{bookId}`
- **Response DTO (`201 Created`)**: `{"message": "Book added to wishlist"}`
- **Error Codes**: `404 Not Found`: `BOOK_NOT_FOUND`, `409 Conflict`: `WISHLIST_ITEM_ALREADY_EXISTS`

#### 8.3 Remove Book from Wishlist
- **Resource Name**: Remove from Wishlist
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/wishlist/{bookId}`
- **Response DTO (`204 No Content`)**

---

### Domain 9: Cart (`/api/v1/cart`)

#### 9.1 Get Active Cart
- **Resource Name**: Shopping Cart
- **HTTP Method**: `GET`
- **URI**: `/api/v1/cart`
- **Headers**: Bearer Token (Registered Member) OR `X-Session-Token` (Guest User)
- **Response DTO (`CartResponse` - `200 OK`)**:
  ```json
  {
    "cartId": "ca1a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "totalItems": 2,
    "subtotal": 508.00,
    "items": [
      {
        "cartItemId": "ci1a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "title": "Joy of Minimalism",
        "authorName": "Daniel Reed",
        "format": "PAPERBACK",
        "unitPrice": 149.00,
        "quantity": 1,
        "lineTotal": 149.00,
        "coverImageUrl": "https://cdn.bookworm.com/covers/minimalism.jpg",
        "tentativeDeliveryDate": "2025-07-21"
      },
      {
        "cartItemId": "ci2a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "bookId": "a82bc10b-58cc-4372-a567-0e02b2c3d111",
        "title": "The Path to Success",
        "authorName": "James Wright",
        "format": "PAPERBACK",
        "unitPrice": 359.00,
        "quantity": 1,
        "lineTotal": 359.00,
        "coverImageUrl": "https://cdn.bookworm.com/covers/success.jpg",
        "tentativeDeliveryDate": "2025-07-21"
      }
    ]
  }
  ```

#### 9.2 Add Item to Cart
- **Resource Name**: Add to Basket
- **HTTP Method**: `POST`
- **URI**: `/api/v1/cart/items`
- **Request DTO (`AddToCartRequest`)**:
  ```json
  {
    "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "quantity": 1
  }
  ```
- **Response DTO (`CartResponse` - `200 OK`)**
- **Validation Rules**: `bookId`: `@NotNull`, `quantity`: `@Min(1)`
- **Error Codes**: `404 Not Found`: `BOOK_NOT_FOUND`, `400 Bad Request`: `INSUFFICIENT_STOCK`

#### 9.3 Update Cart Item Quantity (Stepper `- 1 +`)
- **Resource Name**: Modify Quantity
- **HTTP Method**: `PUT`
- **URI**: `/api/v1/cart/items/{cartItemId}`
- **Request DTO (`UpdateQuantityRequest`)**:
  ```json
  {
    "quantity": 2
  }
  ```
- **Response DTO (`CartResponse` - `200 OK`)**
- **Validation Rules**: `quantity`: `@NotNull`, `@Min(1)`
- **Error Codes**: `404 Not Found`: `CART_ITEM_NOT_FOUND`, `400 Bad Request`: `INSUFFICIENT_STOCK`

#### 9.4 Remove Item from Cart
- **Resource Name**: Delete Cart Item
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/cart/items/{cartItemId}`
- **Response DTO (`CartResponse` - `200 OK`)**

#### 9.5 Merge Guest Cart into Member Account
- **Resource Name**: Merge Cart
- **HTTP Method**: `POST`
- **URI**: `/api/v1/cart/merge`
- **Request DTO (`CartMergeRequest`)**:
  ```json
  {
    "guestSessionToken": "d4e2a1b0-8c9f-4e3a-9f1a-5b2c3d4e5f6a"
  }
  ```
- **Response DTO (`CartResponse` - `200 OK`)**

---

### Domain 10: Orders (`/api/v1/orders`)

#### 10.1 Pre-Checkout Calculation (Totals Preview)
- **Resource Name**: Order Total Preview
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/calculate`
- **Request DTO (`OrderCalculateRequest`)**:
  ```json
  {
    "shippingAddressId": "c71a3b52-19e4-42b7-a392-f01e23a45c67",
    "couponCode": "SUMMER100",
    "redeemGiftPoints": 0
  }
  ```
- **Response DTO (`OrderPriceCalculationResponse` - `200 OK`)**:
  ```json
  {
    "itemCount": 2,
    "subtotal": 508.00,
    "taxAmount": 62.00,
    "deliveryCharge": 0.00,
    "isFreeDelivery": true,
    "couponCodeApplied": "SUMMER100",
    "discountAmount": 100.00,
    "pointsDiscount": 0.00,
    "totalPayableAmount": 470.00
  }
  ```

#### 10.2 Create Order (Checkout)
- **Resource Name**: Place Order
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/checkout`
- **Headers**: `Idempotency-Key: <UUID>`
- **Request DTO (`OrderCheckoutRequest`)**:
  ```json
  {
    "shippingAddressId": "c71a3b52-19e4-42b7-a392-f01e23a45c67",
    "couponCode": "SUMMER100",
    "redeemGiftPoints": 0
  }
  ```
- **Response DTO (`OrderCheckoutResponse` - `201 Created`)**:
  ```json
  {
    "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "orderNumber": "BW-2025-00124",
    "status": "PENDING_PAYMENT",
    "subtotal": 508.00,
    "taxAmount": 62.00,
    "deliveryCharge": 0.00,
    "discountAmount": 100.00,
    "totalPayableAmount": 470.00,
    "tentativeDeliveryDate": "2025-07-21"
  }
  ```
- **Validation Rules**: `shippingAddressId`: `@NotNull`
- **Error Codes**:
  - `400 Bad Request`: `EMPTY_CART`, `ADDRESS_INVALID`, `COUPON_INVALID`

#### 10.3 List Order History
- **Resource Name**: My Orders
- **HTTP Method**: `GET`
- **URI**: `/api/v1/orders?page=0&size=10`
- **Response DTO (`PagedResponse<OrderSummaryResponse>` - `200 OK`)**:
  ```json
  {
    "content": [
      {
        "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "orderNumber": "BW-2025-00124",
        "status": "CONFIRMED",
        "totalAmount": 470.00,
        "itemCount": 2,
        "tentativeDeliveryDate": "2025-07-21",
        "createdAt": "2025-07-19T14:30:00Z",
        "isCancellable": true
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

#### 10.4 Get Order Details
- **Resource Name**: Order Details
- **HTTP Method**: `GET`
- **URI**: `/api/v1/orders/{orderId}`
- **Response DTO (`OrderDetailResponse` - `200 OK`)**:
  ```json
  {
    "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "orderNumber": "BW-2025-00124",
    "status": "CONFIRMED",
    "subtotal": 508.00,
    "taxAmount": 62.00,
    "deliveryCharge": 0.00,
    "discountAmount": 100.00,
    "totalAmount": 470.00,
    "tentativeDeliveryDate": "2025-07-21",
    "createdAt": "2025-07-19T14:30:00Z",
    "isCancellable": true,
    "shippingAddress": {
      "firstName": "John",
      "lastName": "Smith",
      "addressLine1": "Flat 402, Sunshine Heights",
      "city": "Bengaluru",
      "state": "Karnataka",
      "pinCode": "560001",
      "phoneNumber": "+919876543210"
    },
    "items": [
      {
        "bookId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "bookTitle": "Joy of Minimalism",
        "bookFormat": "PAPERBACK",
        "quantity": 1,
        "unitPrice": 149.00,
        "totalPrice": 149.00
      }
    ]
  }
  ```
- **Error Codes**: `404 Not Found`: `ORDER_NOT_FOUND`

#### 10.5 Cancel Order (48-Hour Cancellation Rule)
- **Resource Name**: Cancel Order
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/{orderId}/cancel`
- **Request DTO (`CancelOrderRequest`)**:
  ```json
  {
    "reason": "Changed my mind"
  }
  ```
- **Response DTO (`CancelOrderResponse` - `200 OK`)**:
  ```json
  {
    "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "status": "CANCELLED",
    "cancelledAt": "2025-07-20T10:00:00Z",
    "refundInitiated": true,
    "message": "Order successfully cancelled and refund initiated."
  }
  ```
- **Business Rule Check**: `now - createdAt <= 48 hours` AND `status NOT IN ('SHIPPED', 'DELIVERED')`
- **Error Codes**:
  - `400 Bad Request`: `ORDER_CANCELLATION_EXPIRED`, `ORDER_ALREADY_DISPATCHED`

#### 10.6 Buy It Again (Reorder from History)
- **Resource Name**: Reorder Past Purchase
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/{orderId}/buy-again`
- **Response DTO (`CartResponse` - `200 OK`)**: Active cart updated with items from the past order.
- **Error Codes**: `404 Not Found`: `ORDER_NOT_FOUND`, `400 Bad Request`: `ITEMS_OUT_OF_STOCK`

---

### Domain 11: Payments (`/api/v1/payments`)

#### 11.1 Initiate Payment
- **Resource Name**: Complete Payment
- **HTTP Method**: `POST`
- **URI**: `/api/v1/payments/initiate`
- **Headers**: `Idempotency-Key: <UUID>`
- **Request DTO (`PaymentInitiateRequest`)**:
  ```json
  {
    "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "paymentMethod": "CREDIT_CARD",
    "amount": 470.00,
    "cardDetails": {
      "cardNumber": "4111222233334444",
      "nameOnCard": "John Smith",
      "expiryMonth": "12",
      "expiryYear": "2028",
      "cvv": "123"
    }
  }
  ```
- **Response DTO (`PaymentInitiateResponse` - `200 OK`)**:
  ```json
  {
    "transactionId": "tx1a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "orderId": "o1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "status": "SUCCESS",
    "gatewayRefId": "PG_TXN_987654321",
    "message": "Payment completed successfully"
  }
  ```
- **Validation Rules**:
  - `orderId`: `@NotNull`
  - `paymentMethod`: `@NotBlank`, Must be `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, or `WALLET`
  - `amount`: `@DecimalMin("0.01")`
- **Error Codes**:
  - `400 Bad Request`: `PAYMENT_AMOUNT_MISMATCH`, `PAYMENT_DECLINED`
  - `404 Not Found`: `ORDER_NOT_FOUND`

#### 11.2 Get User Wallet Balance
- **Resource Name**: Wallet Balance
- **HTTP Method**: `GET`
- **URI**: `/api/v1/payments/wallet`
- **Response DTO (`WalletBalanceResponse` - `200 OK`)**:
  ```json
  {
    "walletId": "wa1a2b3c-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "balance": 250.00,
    "currencyCode": "INR"
  }
  ```

---

### Domain 12: Shipping & Logistics (`/api/v1/shipping`)

#### 12.1 Calculate Estimated Delivery Date (EDD)
- **Resource Name**: Delivery Date Calculator
- **HTTP Method**: `GET`
- **URI**: `/api/v1/shipping/edd?pinCode=560001`
- **Query Parameters**: `pinCode` (6-digit PIN)
- **Response DTO (`EDDResponse` - `200 OK`)**:
  ```json
  {
    "pinCode": "560001",
    "tentativeDeliveryDate": "2025-07-21",
    "deliveryDays": 2,
    "isServiceable": true
  }
  ```
- **Validation Rules**: `pinCode`: `@Pattern(regexp = "^[1-9][0-9]{5}$")`
- **Error Codes**: `400 Bad Request`: `INVALID_PIN_CODE`

#### 12.2 Track Shipment
- **Resource Name**: Shipment Tracking
- **HTTP Method**: `GET`
- **URI**: `/api/v1/shipping/track/{orderId}`
- **Response DTO (`ShipmentTrackingResponse` - `200 OK`)**:
  ```json
  {
    "shipmentId": "s1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "carrierName": "BlueDart Express",
    "trackingNumber": "BD-9876543210",
    "status": "IN_TRANSIT",
    "estimatedDeliveryDate": "2025-07-21",
    "events": [
      {
        "eventStatus": "DISPATCHED",
        "location": "Bengaluru Central Hub",
        "timestamp": "2025-07-19T18:00:00Z",
        "description": "Package picked up by carrier"
      }
    ]
  }
  ```
- **Error Codes**: `404 Not Found`: `SHIPMENT_NOT_FOUND`

---

### Domain 13: Reviews & Ratings (`/api/v1/books/{bookId}/reviews`)

#### 13.1 Get Book Reviews
- **Resource Name**: Book Reviews List
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books/{bookId}/reviews?page=0&size=10`
- **Response DTO (`PagedResponse<ReviewResponse>` - `200 OK`)**:
  ```json
  {
    "content": [
      {
        "reviewId": "r1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "reviewerName": "John Smith",
        "rating": 5,
        "reviewText": "The accordion component delivers large amounts of content in a small space.",
        "createdAt": "2025-07-18T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

#### 13.2 Submit Book Review
- **Resource Name**: Leave Review
- **HTTP Method**: `POST`
- **URI**: `/api/v1/books/{bookId}/reviews`
- **Request DTO (`ReviewCreateRequest`)**:
  ```json
  {
    "rating": 5,
    "reviewText": "Inspiring read on decluttering life and finding clarity."
  }
  ```
- **Response DTO (`ReviewResponse` - `201 Created`)**
- **Validation Rules**:
  - `rating`: `@NotNull`, `@Min(1)`, `@Max(5)`
  - `reviewText`: `@NotBlank`, `@Size(max = 100)` (Strict 100-character constraint from UI `0/100`)
- **Error Codes**:
  - `400 Bad Request`: `REVIEW_TEXT_EXCEEDS_100_CHARS`, `INVALID_RATING`
  - `401 Unauthorized`: `AUTH_UNAUTHORIZED`

---

### Domain 14: Coupons & Loyalty (`/api/v1/coupons`, `/api/v1/loyalty`)

#### 14.1 Validate Coupon Code
- **Resource Name**: Apply Coupon
- **HTTP Method**: `POST`
- **URI**: `/api/v1/coupons/validate`
- **Request DTO (`CouponValidateRequest`)**:
  ```json
  {
    "code": "SUMMER100",
    "cartSubtotal": 508.00
  }
  ```
- **Response DTO (`CouponValidationResponse` - `200 OK`)**:
  ```json
  {
    "code": "SUMMER100",
    "isValid": true,
    "discountType": "FIXED_AMOUNT",
    "discountValue": 100.00,
    "calculatedDiscount": 100.00,
    "message": "Coupon applied successfully"
  }
  ```
- **Validation Rules**: `code`: `@NotBlank`, `cartSubtotal`: `@NotNull`, `@DecimalMin("0.00")`
- **Error Codes**: `400 Bad Request`: `COUPON_INVALID`, `COUPON_EXPIRED`, `COUPON_MIN_SPEND_NOT_MET`

#### 14.2 Get Loyalty Points Balance
- **Resource Name**: Redeem Gift Points Balance
- **HTTP Method**: `GET`
- **URI**: `/api/v1/loyalty/points`
- **Response DTO (`LoyaltyPointsResponse` - `200 OK`)**:
  ```json
  {
    "pointsBalance": 150,
    "conversionRate": 1.00,
    "monetaryValue": 150.00
  }
  ```

---

### Domain 15: Recommendations (`/api/v1/recommendations`)

#### 15.1 Get "Recommended for You"
- **Resource Name**: Personalized Recommendations
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/for-you?limit=6`
- **Response DTO (`List<BookCardResponse>` - `200 OK`)**

#### 15.2 Get "Bestsellers this Month"
- **Resource Name**: Monthly Bestsellers
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/bestsellers?limit=6`
- **Response DTO (`List<BookCardResponse>` - `200 OK`)**

#### 15.3 Get "New Launches"
- **Resource Name**: New Book Launches
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/new-launches?limit=6`
- **Response DTO (`List<BookCardResponse>` - `200 OK`)**

#### 15.4 Get "Related Reads" (PDP Cross-Sell)
- **Resource Name**: Related Books
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/related/{bookId}?limit=3`
- **Response DTO (`List<BookCardResponse>` - `200 OK`)**
- **Error Codes**: `404 Not Found`: `BOOK_NOT_FOUND`

---

## 3. Summary of API Endpoints

| Domain | Method | URI | Description |
| :--- | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/v1/auth/register` | Register new customer account |
| **Authentication** | `POST` | `/api/v1/auth/login` | Login and receive JWT access token |
| **Authentication** | `POST` | `/api/v1/auth/logout` | Revoke session/token |
| **Users** | `GET` | `/api/v1/users/profile` | Retrieve customer profile |
| **Users** | `GET` | `/api/v1/users/addresses` | List saved delivery addresses |
| **Users** | `POST` | `/api/v1/users/addresses` | Add new delivery address |
| **Users** | `DELETE` | `/api/v1/users/addresses/{id}` | Delete saved delivery address |
| **Authors** | `GET` | `/api/v1/authors` | List authors (paginated) |
| **Authors** | `GET` | `/api/v1/authors/{id}` | Get author profile & book listings |
| **Publishers** | `GET` | `/api/v1/publishers` | List book publishers |
| **Categories** | `GET` | `/api/v1/categories` | List 20 taxonomy categories |
| **Books** | `GET` | `/api/v1/books` | Filter & browse books by category/price/format |
| **Books** | `GET` | `/api/v1/books/{id}` | Get book details (PDP view) |
| **Search** | `GET` | `/api/v1/search` | Multi-faceted keyword book search |
| **Wishlist** | `GET` | `/api/v1/wishlist` | Get member's wishlist |
| **Wishlist** | `POST` | `/api/v1/wishlist/{bookId}` | Add book to wishlist |
| **Wishlist** | `DELETE` | `/api/v1/wishlist/{bookId}` | Remove book from wishlist |
| **Cart** | `GET` | `/api/v1/cart` | Get active cart (Guest / Member) |
| **Cart** | `POST` | `/api/v1/cart/items` | Add book to cart |
| **Cart** | `PUT` | `/api/v1/cart/items/{id}` | Update quantity with `- 1 +` stepper |
| **Cart** | `DELETE` | `/api/v1/cart/items/{id}` | Remove book from cart |
| **Cart** | `POST` | `/api/v1/cart/merge` | Merge guest cart into member account |
| **Orders** | `POST` | `/api/v1/orders/calculate` | Pre-checkout order pricing summary |
| **Orders** | `POST` | `/api/v1/orders/checkout` | Create order from cart |
| **Orders** | `GET` | `/api/v1/orders` | List order history (`My Orders`) |
| **Orders** | `GET` | `/api/v1/orders/{id}` | Get order details |
| **Orders** | `POST` | `/api/v1/orders/{id}/cancel` | Cancel order within 48h policy window |
| **Orders** | `POST` | `/api/v1/orders/{id}/buy-again` | Reorder items from past order |
| **Payments** | `POST` | `/api/v1/payments/initiate` | Process payment (Card, UPI, Wallet) |
| **Payments** | `GET` | `/api/v1/payments/wallet` | Get wallet balance |
| **Shipping** | `GET` | `/api/v1/shipping/edd` | Calculate tentative delivery date |
| **Shipping** | `GET` | `/api/v1/shipping/track/{orderId}` | Track shipment timeline |
| **Reviews** | `GET` | `/api/v1/books/{id}/reviews` | List book reviews |
| **Reviews** | `POST` | `/api/v1/books/{id}/reviews` | Submit 1–5 star review ($\le 100$ chars) |
| **Coupons** | `POST` | `/api/v1/coupons/validate` | Validate coupon discount |
| **Coupons** | `GET` | `/api/v1/loyalty/points` | Check redeemable gift points |
| **Recommendations** | `GET` | `/api/v1/recommendations/for-you` | Get "Recommended for You" shelf |
| **Recommendations** | `GET` | `/api/v1/recommendations/bestsellers` | Get "Bestsellers this Month" shelf |
| **Recommendations** | `GET` | `/api/v1/recommendations/new-launches` | Get "New Launches" shelf |
| **Recommendations** | `GET` | `/api/v1/recommendations/related/{id}` | Get "Related Reads" sidebar |
