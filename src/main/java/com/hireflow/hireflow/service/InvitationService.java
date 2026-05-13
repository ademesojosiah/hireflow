package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.AcceptInviteRequest;
import com.hireflow.hireflow.dto.request.InviteHManagerRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;

public interface InvitationService {

    void inviteHManager(InviteHManagerRequest request, User admin);

    AuthResponse acceptInvite(AcceptInviteRequest request);
}
