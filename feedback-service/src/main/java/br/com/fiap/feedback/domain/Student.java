package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class Student {

    private final UUID id;
    private final String name;
    private final String registrationNumber;

    private final UUID authId;
}
