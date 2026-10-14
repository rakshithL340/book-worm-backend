package com.bookworm.order.mapper;

import com.bookworm.order.dto.OrderDetailResponse;
import com.bookworm.order.dto.OrderItemResponse;
import com.bookworm.order.dto.OrderSummaryResponse;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderItem;
import com.bookworm.user.mapper.AddressMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {AddressMapper.class})
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus() != null ? order.getStatus().name() : null)")
    @Mapping(target = "itemCount", expression = "java(order.getItems() != null ? order.getItems().stream().mapToInt(com.bookworm.order.entity.OrderItem::getQuantity).sum() : 0)")
    @Mapping(target = "isCancellable", expression = "java(isOrderCancellable(order))")
    OrderSummaryResponse toSummaryResponse(Order order);

    List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders);

    @Mapping(target = "status", expression = "java(order.getStatus() != null ? order.getStatus().name() : null)")
    @Mapping(target = "isCancellable", expression = "java(isOrderCancellable(order))")
    @Mapping(target = "shippingAddress", source = "shippingAddress")
    @Mapping(target = "items", source = "items")
    OrderDetailResponse toDetailResponse(Order order);

    @Mapping(target = "bookId", source = "book.bookId")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    default boolean isOrderCancellable(Order order) {
        if (order == null || order.getCreatedAt() == null) {
            return false;
        }
        boolean within48Hours = Duration.between(order.getCreatedAt(), Instant.now()).toHours() <= 48;
        boolean validStatus = order.getStatus() != null && 
                (order.getStatus() == com.bookworm.order.entity.OrderStatus.CREATED ||
                 order.getStatus() == com.bookworm.order.entity.OrderStatus.PENDING_PAYMENT ||
                 order.getStatus() == com.bookworm.order.entity.OrderStatus.CONFIRMED ||
                 order.getStatus() == com.bookworm.order.entity.OrderStatus.PROCESSING);
        return within48Hours && validStatus;
    }
}
