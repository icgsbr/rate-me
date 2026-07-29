package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class Admin {

    private final UUID id;
    private final String name;

    private final UUID authId;

    private final String role;
}
