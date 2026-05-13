package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAllByCompanyIdAndRoleIn(String companyId, Collection<Role> roles, Pageable pageable);
}
