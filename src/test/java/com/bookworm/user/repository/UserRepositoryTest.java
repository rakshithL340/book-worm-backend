package com.bookworm.user.repository;

import com.bookworm.AbstractIntegrationTest;
import com.bookworm.user.entity.User;
import com.bookworm.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test.user@bookworm.example.com")
                .passwordHash("$2a$10$hashedPassword")
                .firstName("Test")
                .lastName("Reader")
                .role(UserRole.ROLE_CUSTOMER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find active user by email")
    void shouldFindUserByEmail() {
        Optional<User> found = userRepository.findByEmail("test.user@bookworm.example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Test");
        assertThat(found.get().getLastName()).isEqualTo("Reader");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckExistsByEmail() {
        boolean exists = userRepository.existsByEmail("test.user@bookworm.example.com");
        boolean notExists = userRepository.existsByEmail("non.existent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should search users by keyword matching first name or email")
    void shouldSearchUsersByKeyword() {
        Page<User> results = userRepository.searchUsers("Reader", PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent().get(0).getEmail()).isEqualTo("test.user@bookworm.example.com");
    }
}
