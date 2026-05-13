package com.hireflow.hireflow.data.model;

import com.hireflow.hireflow.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hmanager_invitations")
@Getter
@Setter
@NoArgsConstructor
public class HManagerInvitation extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column
    private String companyId;

    @Column(nullable = false)
    private String invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
}
