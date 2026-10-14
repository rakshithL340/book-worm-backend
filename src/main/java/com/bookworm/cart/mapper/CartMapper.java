package com.bookworm.cart.mapper;

import com.bookworm.cart.dto.CartItemResponse;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.entity.Cart;
import com.bookworm.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "totalItems", expression = "java(cart.getItems() != null ? cart.getItems().stream().mapToInt(com.bookworm.cart.entity.CartItem::getQuantity).sum() : 0)")
    @Mapping(target = "subtotal", expression = "java(cart.getItems() != null ? cart.getItems().stream().map(i -> i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : java.math.BigDecimal.ZERO)")
    @Mapping(target = "items", source = "items")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "bookId", source = "book.bookId")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "authorName", source = "book.author.name")
    @Mapping(target = "format", expression = "java(item.getBook().getFormat() != null ? item.getBook().getFormat().name() : null)")
    @Mapping(target = "coverImageUrl", source = "book.coverImageUrl")
    @Mapping(target = "lineTotal", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    @Mapping(target = "tentativeDeliveryDate", expression = "java(java.time.LocalDate.now().plusDays(2))")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponseList(List<CartItem> items);
}
