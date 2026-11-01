package com.wams.repository;

import com.wams.entity.User;
import com.wams.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRole(Role role);

    boolean existsByRole(Role role);

    /** Locks every legacy/canonical account matching a normalized email. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(trim(u.email)) = :email order by u.id")
    List<User> findAllByNormalizedEmailForUpdate(@Param("email") String normalizedEmail);
}
