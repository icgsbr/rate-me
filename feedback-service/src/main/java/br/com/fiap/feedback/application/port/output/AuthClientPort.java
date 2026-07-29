package br.com.fiap.feedback.application.port.output;

import java.util.UUID;

public interface AuthClientPort {

    UUID register(String login, String password, UUID externalId);
}
