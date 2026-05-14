package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.dto.response.InterviewSlotResponse;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewSlotResponse toResponse(InterviewSlot slot) {
        if (slot == null) return null;
        InterviewSlotResponse response = new InterviewSlotResponse();
        response.setId(slot.getId());
        response.setApplicationId(slot.getApplication() == null ? null : slot.getApplication().getId());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setTimezone(slot.getTimezone());
        response.setMeetingProvider(slot.getMeetingProvider());
        response.setMeetingLink(slot.getMeetingLink());
        response.setInterviewerEmail(slot.getInterviewerEmail());
        response.setStatus(slot.getStatus());
        response.setNotes(slot.getNotes());
        response.setCreatedAt(slot.getCreatedAt());
        response.setUpdatedAt(slot.getUpdatedAt());
        return response;
    }
}
