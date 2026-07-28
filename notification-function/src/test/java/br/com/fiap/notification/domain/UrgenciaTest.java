package br.com.fiap.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrgenciaTest {

    @ParameterizedTest
    @CsvSource({
            "BAIXA,   BAIXA",
            "media,   MEDIA",
            "'  alta  ', ALTA",
            "CrItIcA, CRITICA"
    })
    @DisplayName("parses case-insensitively and trims surrounding whitespace")
    void parsesLenientValues(String raw, Urgencia expected) {
        assertThat(Urgencia.from(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("rejects a null or blank value")
    void rejectsBlank(String raw) {
        assertThatThrownBy(() -> Urgencia.from(raw))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("urgencia is required");
    }

    @Test
    @DisplayName("rejects an unknown value and lists the accepted ones")
    void rejectsUnknownValue() {
        assertThatThrownBy(() -> Urgencia.from("URGENTISSIMO"))
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("urgencia must be one of BAIXA, MEDIA, ALTA, CRITICA (got 'URGENTISSIMO')");
    }

    @Test
    @DisplayName("declares exactly the four documented levels")
    void declaresFourLevels() {
        assertThat(Urgencia.values())
                .containsExactly(Urgencia.BAIXA, Urgencia.MEDIA, Urgencia.ALTA, Urgencia.CRITICA);
    }
}
