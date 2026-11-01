package com.wams.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Vendor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndIdNot(Long userId, Long id);
}
