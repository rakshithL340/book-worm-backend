package com.bookworm.catalog.repository;

import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.entity.BookFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Page<Book> findByCategorySlug(String slug, Pageable pageable);

    Page<Book> findByAuthorAuthorId(UUID authorId, Pageable pageable);

    // Multi-faceted search and filter query
    @Query("SELECT b FROM Book b WHERE " +
           "(:category IS NULL OR LOWER(b.category.slug) = LOWER(:category) OR LOWER(b.category.name) = LOWER(:category)) AND " +
           "(:language IS NULL OR LOWER(b.language) = LOWER(:language)) AND " +
           "(:format IS NULL OR b.format = :format) AND " +
           "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.price <= :maxPrice)")
    Page<Book> findWithFilters(
            @Param("category") String category,
            @Param("language") String language,
            @Param("format") BookFormat format,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Global keyword search across title, author name, and ISBN
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchByKeyword(@Param("query") String query, Pageable pageable);

    // Bestsellers this month (sorted by sales velocity)
    @Query("SELECT b FROM Book b ORDER BY b.salesCount DESC")
    List<Book> findBestsellers(Pageable pageable);

    // New Launches (sorted by creation date)
    @Query("SELECT b FROM Book b ORDER BY b.createdAt DESC")
    List<Book> findNewLaunches(Pageable pageable);

    // Related Reads for PDP (matching category or author excluding current book)
    @Query("SELECT b FROM Book b WHERE (b.category.categoryId = :categoryId OR b.author.authorId = :authorId) " +
           "AND b.bookId <> :currentBookId ORDER BY b.salesCount DESC")
    List<Book> findRelatedBooks(
            @Param("categoryId") UUID categoryId,
            @Param("authorId") UUID authorId,
            @Param("currentBookId") UUID currentBookId,
            Pageable pageable);
}
