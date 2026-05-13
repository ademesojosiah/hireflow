package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.HManagerInvitation;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.HManagerInvitationRepository;
import com.hireflow.hireflow.dto.request.AcceptInviteRequest;
import com.hireflow.hireflow.dto.request.InviteHManagerRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.enums.InvitationStatus;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.security.util.JwtUtil;
import com.hireflow.hireflow.service.CompanyService;
import com.hireflow.hireflow.service.InvitationService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final HManagerInvitationRepository invitationRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${hireflow.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void inviteHManager(InviteHManagerRequest request, User admin) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        if (invitationRepository.existsByEmailAndStatus(request.getEmail(), InvitationStatus.PENDING)) {
            throw new DuplicateResourceException("A pending invitation already exists for this email");
        }

        String token = UUID.randomUUID().toString();
        String inviteLink = frontendBaseUrl + "/accept-invite?token=" + token;

        HManagerInvitation invitation = new HManagerInvitation();
        invitation.setToken(token);
        invitation.setEmail(request.getEmail());
        invitation.setCompanyId(request.getCompanyId());
        invitation.setInvitedBy(admin.getId());
        invitation.setStatus(InvitationStatus.PENDING);
        invitationRepository.save(invitation);

        eventPublisher.publishEvent(EmailNotificationEvent.hManagerInvite(request.getEmail(), inviteLink));

        log.info("HR manager invitation sent to {} by admin {}", request.getEmail(), admin.getId());
    }

    @Override
    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        HManagerInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or invalid"));

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new CustomException("This invitation has already been used");
        }

        if (userService.existsByEmail(invitation.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(invitation.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.HMANAGER);
        user.setVerified(true);

        if (invitation.getCompanyId() != null) {
            Company company = companyService.findCompanyById(invitation.getCompanyId());
            user.setCompany(company);
        }

        userService.save(user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("HR manager account created for {} via invitation", invitation.getEmail());

        String token = jwtUtil.generateToken(user.getEmail());
        return userMapper.toAuthResponse(user, token);
    }
}
