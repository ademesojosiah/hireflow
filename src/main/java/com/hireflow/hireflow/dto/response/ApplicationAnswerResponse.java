package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnswerResponse {

    private String id;
    private String questionId;
    private String question;
    private String answer;
    private Instant createdAt;
}
