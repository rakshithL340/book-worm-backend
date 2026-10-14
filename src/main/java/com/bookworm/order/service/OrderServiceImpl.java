package com.bookworm.order.service;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.entity.Cart;
import com.bookworm.cart.entity.CartItem;
import com.bookworm.cart.repository.CartItemRepository;
import com.bookworm.cart.repository.CartRepository;
import com.bookworm.cart.service.CartService;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.InsufficientStockException;
import com.bookworm.common.exception.OrderNotCancellableException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.CancelOrderResponse;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.order.dto.OrderCheckoutResponse;
import com.bookworm.order.dto.OrderDetailResponse;
import com.bookworm.order.dto.OrderPriceCalculationResponse;
import com.bookworm.order.dto.OrderSummaryResponse;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderItem;
import com.bookworm.order.entity.OrderStatus;
import com.bookworm.order.entity.OrderStatusHistory;
import com.bookworm.order.mapper.OrderMapper;
import com.bookworm.order.repository.OrderItemRepository;
import com.bookworm.order.repository.OrderRepository;
import com.bookworm.order.repository.OrderStatusHistoryRepository;
import com.bookworm.payment.entity.PaymentStatus;
import com.bookworm.payment.entity.PaymentTransaction;
import com.bookworm.payment.entity.RefundTransaction;
import com.bookworm.payment.repository.PaymentTransactionRepository;
import com.bookworm.payment.repository.RefundTransactionRepository;
import com.bookworm.promotion.entity.Coupon;
import com.bookworm.promotion.entity.DiscountType;
import com.bookworm.promotion.entity.LoyaltyAccount;
import com.bookworm.promotion.repository.LoyaltyAccountRepository;
import com.bookworm.promotion.service.CouponService;
import com.bookworm.promotion.service.LoyaltyService;
import com.bookworm.store.entity.Store;
import com.bookworm.store.entity.StorePolicy;
import com.bookworm.store.repository.StorePolicyRepository;
import com.bookworm.store.repository.StoreRepository;
import com.bookworm.user.entity.Address;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.AddressRepository;
import com.bookworm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BookRepository bookRepository;
    private final StoreRepository storeRepository;
    private final StorePolicyRepository storePolicyRepository;
    private final CouponService couponService;
    private final LoyaltyService loyaltyService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final OrderMapper orderMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public OrderPriceCalculationResponse calculatePrice(UUID userId, OrderCalculateRequest request) {
        log.info("Calculating order price for user: {}, coupon: {}, redeemPoints: {}",
                userId, request.getCouponCode(), request.getRedeemGiftPoints());

        Cart cart = getActiveCartWithItems(userId);
        StorePolicy policy = getEffectiveStorePolicy();

        BigDecimal subtotal = calculateCartSubtotal(cart);
        int totalItemCount = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        // Delivery fee calculation
        boolean isFreeDelivery = subtotal.compareTo(policy.getFreeDeliveryThreshold()) >= 0;
        BigDecimal deliveryCharge = isFreeDelivery ? BigDecimal.ZERO : policy.getStandardDeliveryFee();

        // Tax calculation based on store policy tax rate percentage
        BigDecimal taxRate = policy.getTaxRatePercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        // Coupon calculation
        BigDecimal couponDiscount = BigDecimal.ZERO;
        String couponCodeApplied = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                Coupon coupon = couponService.getValidCouponEntity(request.getCouponCode().trim(), subtotal, userId);
                couponDiscount = computeCouponDiscount(coupon, subtotal);
                couponCodeApplied = coupon.getCode();
            } catch (Exception e) {
                log.warn("Coupon [{}] not applied: {}", request.getCouponCode(), e.getMessage());
            }
        }

        // Loyalty gift points calculation (1 point = 1 INR)
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        int requestedPoints = request.getRedeemGiftPoints() != null ? request.getRedeemGiftPoints() : 0;
        if (requestedPoints > 0) {
            LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByUserUserId(userId).orElse(null);
            int availablePoints = loyaltyAccount != null ? loyaltyAccount.getPointsBalance() : 0;
            int redeemablePoints = Math.min(requestedPoints, availablePoints);
            BigDecimal potentialPointsDiscount = BigDecimal.valueOf(redeemablePoints)
                    .multiply(policy.getLoyaltyRedemptionRate()).setScale(2, RoundingMode.HALF_UP);

            BigDecimal currentTotal = subtotal.add(taxAmount).add(deliveryCharge).subtract(couponDiscount);
            pointsDiscount = potentialPointsDiscount.min(currentTotal.max(BigDecimal.ZERO));
        }

        BigDecimal totalPayable = subtotal.add(taxAmount).add(deliveryCharge)
                .subtract(couponDiscount).subtract(pointsDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return OrderPriceCalculationResponse.builder()
                .itemCount(totalItemCount)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryCharge(deliveryCharge)
                .isFreeDelivery(isFreeDelivery)
                .couponCodeApplied(couponCodeApplied)
                .discountAmount(couponDiscount)
                .pointsDiscount(pointsDiscount)
                .totalPayableAmount(totalPayable)
                .build();
    }

    @Override
    @Transactional
    public OrderCheckoutResponse checkout(UUID userId, OrderCheckoutRequest request) {
        log.info("Processing checkout for user: {}, address: {}", userId, request.getShippingAddressId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        Address address = addressRepository.findByAddressIdAndUserUserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid delivery address selected"));

        Cart cart = getActiveCartWithItems(userId);
        Store store = storeRepository.findFirstByActiveTrue()
                .orElseGet(() -> storeRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Store", "active", true)));

        StorePolicy policy = getEffectiveStorePolicy();

        // Stock validation and inventory reservation
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            if (book.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for book: '" +
                        book.getTitle() + "'. Available: " + book.getStockQuantity() + ", In Cart: " + item.getQuantity());
            }
        }

        BigDecimal subtotal = calculateCartSubtotal(cart);
        boolean isFreeDelivery = subtotal.compareTo(policy.getFreeDeliveryThreshold()) >= 0;
        BigDecimal deliveryCharge = isFreeDelivery ? BigDecimal.ZERO : policy.getStandardDeliveryFee();
        BigDecimal taxRate = policy.getTaxRatePercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        // Coupon application
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            appliedCoupon = couponService.getValidCouponEntity(request.getCouponCode().trim(), subtotal, userId);
            couponDiscount = computeCouponDiscount(appliedCoupon, subtotal);
        }

        // Loyalty points redemption
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        int pointsToRedeem = 0;
        int requestedPoints = request.getRedeemGiftPoints() != null ? request.getRedeemGiftPoints() : 0;
        if (requestedPoints > 0) {
            LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByUserUserId(userId).orElse(null);
            int availablePoints = loyaltyAccount != null ? loyaltyAccount.getPointsBalance() : 0;
            pointsToRedeem = Math.min(requestedPoints, availablePoints);
            BigDecimal potentialPointsDiscount = BigDecimal.valueOf(pointsToRedeem)
                    .multiply(policy.getLoyaltyRedemptionRate()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentTotal = subtotal.add(taxAmount).add(deliveryCharge).subtract(couponDiscount);
            pointsDiscount = potentialPointsDiscount.min(currentTotal.max(BigDecimal.ZERO));
        }

        BigDecimal totalDiscount = couponDiscount.add(pointsDiscount);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryCharge).subtract(totalDiscount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        String orderNumber = generateOrderNumber();
        LocalDate tentativeDeliveryDate = LocalDate.now().plusDays(3);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .store(store)
                .shippingAddress(address)
                .status(OrderStatus.CREATED)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryCharge(deliveryCharge)
                .discountAmount(totalDiscount)
                .totalAmount(totalAmount)
                .tentativeDeliveryDate(tentativeDeliveryDate)
                .build();

        // Create Order Items and update stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();

            // Deduct stock and increment sales count
            book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
            book.setSalesCount((book.getSalesCount() != null ? book.getSalesCount() : 0) + cartItem.getQuantity());
            bookRepository.save(book);

            BigDecimal lineTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .totalPrice(lineTotal)
                    .build();
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        // Status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CREATED)
                .notes("Order placed successfully by customer")
                .build();
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        log.info("Created order with orderNumber: {} and ID: {}", savedOrder.getOrderNumber(), savedOrder.getOrderId());

        // Record coupon redemption
        if (appliedCoupon != null && couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
            couponService.recordCouponRedemption(appliedCoupon, user, savedOrder, couponDiscount);
        }

        // Deduct redeemed loyalty points
        if (pointsToRedeem > 0) {
            loyaltyService.redeemPoints(user, savedOrder, pointsToRedeem);
        }

        // Clear user's active cart
        cartItemRepository.deleteByCartCartId(cart.getCartId());
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderCheckoutResponse.builder()
                .orderId(savedOrder.getOrderId())
                .orderNumber(savedOrder.getOrderNumber())
                .status(savedOrder.getStatus().name())
                .subtotal(savedOrder.getSubtotal())
                .taxAmount(savedOrder.getTaxAmount())
                .deliveryCharge(savedOrder.getDeliveryCharge())
                .discountAmount(savedOrder.getDiscountAmount())
                .totalPayableAmount(savedOrder.getTotalAmount())
                .tentativeDeliveryDate(savedOrder.getTentativeDeliveryDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(UUID userId, Pageable pageable) {
        log.info("Fetching order history for user: {}", userId);
        Page<Order> orderPage = orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
        List<OrderSummaryResponse> summaries = orderMapper.toSummaryResponseList(orderPage.getContent());
        return PageResponse.of(summaries, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID userId, UUID orderId) {
        log.info("Fetching order details for order: {} and user: {}", orderId, userId);
        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return orderMapper.toDetailResponse(order);
    }

    @Override
    @Transactional
    public CancelOrderResponse cancelOrder(UUID userId, UUID orderId, CancelOrderRequest request) {
        log.info("Processing cancellation request for order: {} by user: {}", orderId, userId);

        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        StorePolicy policy = getEffectiveStorePolicy();
        int cancellationWindowHours = policy.getCancellationWindowHours() != null ? policy.getCancellationWindowHours() : 48;

        // Strict Business Constraint: cancellation within 48 hours
        if (order.getCreatedAt() != null) {
            long hoursElapsed = Duration.between(order.getCreatedAt(), Instant.now()).toHours();
            if (hoursElapsed > cancellationWindowHours) {
                log.warn("Cancellation rejected: order [{}] was placed {} hours ago (limit: {} hours)",
                        orderId, hoursElapsed, cancellationWindowHours);
                throw new OrderNotCancellableException(
                        "Orders can only be cancelled within " + cancellationWindowHours + " hours of placement");
            }
        }

        // Strict Status Invariant: cannot cancel once dispatched or delivered or already cancelled
        if (order.getStatus() == OrderStatus.DISPATCHED ||
            order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.CANCELLED ||
            order.getStatus() == OrderStatus.RETURNED) {
            log.warn("Cancellation rejected: invalid order status [{}]", order.getStatus());
            throw new OrderNotCancellableException(
                    "Order cannot be cancelled in status: " + order.getStatus().name());
        }

        // Restore inventory stock
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Book book = item.getBook();
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                int currentSales = book.getSalesCount() != null ? book.getSalesCount() : 0;
                book.setSalesCount(Math.max(0, currentSales - item.getQuantity()));
                bookRepository.save(book);
            }
        }

        // Update status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());

        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Customer requested cancellation";
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .notes("Order cancelled. Reason: " + reason)
                .build();
        order.addStatusHistory(history);
        orderRepository.save(order);

        // Check if payment was captured and initiate automatic refund
        boolean refundInitiated = false;
        Optional<PaymentTransaction> successfulPayment = paymentTransactionRepository.findByOrderOrderId(orderId)
                .stream()
                .filter(tx -> tx.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();

        if (successfulPayment.isPresent()) {
            PaymentTransaction payment = successfulPayment.get();
            RefundTransaction refund = RefundTransaction.builder()
                    .paymentTransaction(payment)
                    .refundAmount(order.getTotalAmount())
                    .reason("Order cancellation: " + reason)
                    .status("PROCESSED")
                    .refundReference("REF-" + System.currentTimeMillis())
                    .build();
            refundTransactionRepository.save(refund);
            refundInitiated = true;
            log.info("Initiated refund of ₹{} for cancelled order {}", order.getTotalAmount(), orderId);
        }

        return CancelOrderResponse.builder()
                .orderId(order.getOrderId())
                .status(OrderStatus.CANCELLED.name())
                .cancelledAt(order.getCancelledAt())
                .refundInitiated(refundInitiated)
                .message("Order cancelled successfully" + (refundInitiated ? ". Refund initiated." : "."))
                .build();
    }

    @Override
    @Transactional
    public void buyAgain(UUID userId, UUID orderId) {
        log.info("Processing 'Buy It Again' for order: {} by user: {}", orderId, userId);

        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("Order contains no items to re-order");
        }

        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            if (book.getStockQuantity() > 0) {
                int quantityToAdd = Math.min(item.getQuantity(), book.getStockQuantity());
                cartService.addToCart(userId, null, AddToCartRequest.builder()
                        .bookId(book.getBookId())
                        .quantity(quantityToAdd)
                        .build());
            }
        }
    }

    private Cart getActiveCartWithItems(UUID userId) {
        Cart cart = cartRepository.findByUserUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new BadRequestException("Your shopping cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Your shopping cart is empty. Please add items to proceed to checkout.");
        }
        return cart;
    }

    private BigDecimal calculateCartSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StorePolicy getEffectiveStorePolicy() {
        return storePolicyRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> StorePolicy.builder()
                        .taxRatePercent(new BigDecimal("12.00"))
                        .freeDeliveryThreshold(new BigDecimal("500.00"))
                        .standardDeliveryFee(new BigDecimal("50.00"))
                        .loyaltyRedemptionRate(new BigDecimal("1.00"))
                        .cancellationWindowHours(48)
                        .returnWindowDays(7)
                        .build());
    }

    private BigDecimal computeCouponDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discount = subtotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
            return discount.min(subtotal);
        } else {
            return coupon.getDiscountValue().min(subtotal);
        }
    }

    private String generateOrderNumber() {
        long timestamp = System.currentTimeMillis();
        int randomCode = 1000 + secureRandom.nextInt(9000);
        return "ORD-" + timestamp + "-" + randomCode;
    }
}
