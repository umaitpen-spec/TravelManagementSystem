package com.umaitpen.travelx.repository;

import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderId(String providerId);
    List<User> findByRole(Role role);
    long countByRole(Role role);
}
