package com.bookworm.wishlist.mapper;

import com.bookworm.wishlist.dto.WishlistItemResponse;
import com.bookworm.wishlist.dto.WishlistResponse;
import com.bookworm.wishlist.entity.Wishlist;
import com.bookworm.wishlist.entity.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WishlistMapper {

    @Mapping(target = "items", source = "items")
    WishlistResponse toResponse(Wishlist wishlist);

    @Mapping(target = "bookId", source = "book.bookId")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "authorName", source = "book.author.name")
    @Mapping(target = "price", source = "book.price")
    @Mapping(target = "format", expression = "java(item.getBook().getFormat() != null ? item.getBook().getFormat().name() : null)")
    @Mapping(target = "coverImageUrl", source = "book.coverImageUrl")
    WishlistItemResponse toItemResponse(WishlistItem item);

    List<WishlistItemResponse> toItemResponseList(List<WishlistItem> items);
}
