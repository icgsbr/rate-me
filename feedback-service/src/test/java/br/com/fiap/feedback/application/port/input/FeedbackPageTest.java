package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase.FeedbackPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackPageTest {

    @ParameterizedTest
    @CsvSource({
            // size, totalElements, expectedTotalPages
            "20,   0,  0",   // nothing to page through
            "20,   1,  1",   // a partial first page still counts
            "20,  20,  1",   // exact multiple
            "20,  21,  2",   // remainder rolls into an extra page
            "10, 100, 10",
            "3,    7,  3"
    })
    @DisplayName("rounds the page count up so the remainder is never dropped")
    void computesTotalPages(int size, long totalElements, long expected) {
        FeedbackPage page = new FeedbackPage(List.of(), 0, size, totalElements);

        assertThat(page.totalPages()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0", "-5"})
    @DisplayName("reports zero pages for a non-positive page size instead of dividing by zero")
    void guardsAgainstANonPositiveSize(int size) {
        assertThat(new FeedbackPage(List.of(), 0, size, 100).totalPages()).isZero();
    }

    @Test
    @DisplayName("keeps the pagination metadata it was built with")
    void keepsItsMetadata() {
        FeedbackPage page = new FeedbackPage(List.of(), 2, 20, 45);

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(45);
        assertThat(page.totalPages()).isEqualTo(3);
    }
}
