package com.bookworm.catalog.repository;

import com.bookworm.catalog.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    Optional<Author> findByNameIgnoreCase(String name);

    Page<Author> findAllByOrderByNameAsc(Pageable pageable);

    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Author> searchAuthors(@Param("query") String query, Pageable pageable);
}
