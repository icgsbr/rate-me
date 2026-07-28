package br.com.fiap.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriticalNotificationTest {

    private static final OffsetDateTime WHEN = OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("rejects a null or blank descricao")
    void rejectsBlankDescricao(String descricao) {
        assertThatThrownBy(() -> new CriticalNotification(descricao, Urgencia.ALTA, WHEN))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("descricao is required");
    }

    @Test
    @DisplayName("accepts a descricao of exactly MAX_DESCRICAO_LENGTH characters")
    void acceptsDescricaoAtTheLimit() {
        String atLimit = "x".repeat(CriticalNotification.MAX_DESCRICAO_LENGTH);

        CriticalNotification notification = new CriticalNotification(atLimit, Urgencia.MEDIA, WHEN);

        assertThat(notification.descricao()).hasSize(CriticalNotification.MAX_DESCRICAO_LENGTH);
    }

    @Test
    @DisplayName("rejects a descricao one character over the limit")
    void rejectsDescricaoOverTheLimit() {
        String tooLong = "x".repeat(CriticalNotification.MAX_DESCRICAO_LENGTH + 1);

        assertThatThrownBy(() -> new CriticalNotification(tooLong, Urgencia.MEDIA, WHEN))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("descricao must be at most 2000 characters");
    }

    @Test
    @DisplayName("rejects a null urgencia")
    void rejectsNullUrgencia() {
        assertThatThrownBy(() -> new CriticalNotification("boom", null, WHEN))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("urgencia is required");
    }

    @Test
    @DisplayName("defaults a null dataEnvio to now")
    void defaultsNullDataEnvioToNow() {
        OffsetDateTime before = OffsetDateTime.now();

        CriticalNotification notification = new CriticalNotification("boom", Urgencia.CRITICA, null);

        assertThat(notification.dataEnvio())
                .isNotNull()
                .isBetween(before, OffsetDateTime.now());
    }

    @Test
    @DisplayName("keeps the supplied dataEnvio untouched")
    void keepsSuppliedDataEnvio() {
        CriticalNotification notification = new CriticalNotification("boom", Urgencia.BAIXA, WHEN);

        assertThat(notification.dataEnvio()).isEqualTo(WHEN);
        assertThat(notification.descricao()).isEqualTo("boom");
        assertThat(notification.urgencia()).isEqualTo(Urgencia.BAIXA);
    }
}
