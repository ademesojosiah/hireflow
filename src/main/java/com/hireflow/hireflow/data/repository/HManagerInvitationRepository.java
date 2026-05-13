package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.HManagerInvitation;
import com.hireflow.hireflow.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HManagerInvitationRepository extends JpaRepository<HManagerInvitation, String> {

    Optional<HManagerInvitation> findByToken(String token);

    boolean existsByEmailAndStatus(String email, InvitationStatus status);
}
