package com.bookworm.catalog.mapper;

import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.dto.BookDetailResponse;
import com.bookworm.catalog.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {AuthorMapper.class, PublisherMapper.class, CategoryMapper.class})
public interface BookMapper {

    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "authorId", source = "author.authorId")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "format", expression = "java(book.getFormat() != null ? book.getFormat().name() : null)")
    @Mapping(target = "tentativeDeliveryDate", expression = "java(java.time.LocalDate.now().plusDays(2))")
    BookCardResponse toCardResponse(Book book);

    List<BookCardResponse> toCardResponseList(List<Book> books);

    @Mapping(target = "format", expression = "java(book.getFormat() != null ? book.getFormat().name() : null)")
    @Mapping(target = "tentativeDeliveryDate", expression = "java(java.time.LocalDate.now().plusDays(2))")
    BookDetailResponse toDetailResponse(Book book);
}
