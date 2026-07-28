package br.com.fiap.feedback.adapter.input.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lives in the production package so the package-private {@code jwt} field, which the
 * container would normally inject, can be set directly.
 */
@ExtendWith(MockitoExtension.class)
class MdcRequestFilterTest {

    private static final String AUTH_ID = "44444444-4444-4444-4444-444444444444";

    @Mock
    private JsonWebToken jwt;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    private MdcRequestFilter filter;
    private MultivaluedMap<String, String> requestHeaders;

    @BeforeEach
    void setUp() {
        filter = new MdcRequestFilter();
        filter.jwt = jwt;
        requestHeaders = new MultivaluedHashMap<>();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("keeps the caller-supplied correlation id")
    void keepsTheIncomingCorrelationId() {
        stubRequest("abc-123");
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn(AUTH_ID);

        filter.filter(requestContext);

        assertThat(MDC.get(MdcRequestFilter.CORRELATION_ID_KEY)).isEqualTo("abc-123");
        assertThat(requestHeaders.getFirst(MdcRequestFilter.CORRELATION_ID_HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(MdcRequestFilter.AUTH_ID_KEY)).isEqualTo(AUTH_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("generates a correlation id when the caller supplies none")
    void generatesACorrelationId(String header) {
        stubRequest(header);
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn(null);

        filter.filter(requestContext);

        String generated = MDC.get(MdcRequestFilter.CORRELATION_ID_KEY);
        assertThat(generated).isNotNull();
        assertThatCode(() -> UUID.fromString(generated)).doesNotThrowAnyException();
        assertThat(requestHeaders.getFirst(MdcRequestFilter.CORRELATION_ID_HEADER)).isEqualTo(generated);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("leaves authId out of the MDC when the token carries no usable claim")
    void skipsABlankAuthIdClaim(String claim) {
        stubRequest("abc-123");
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn(claim);

        filter.filter(requestContext);

        assertThat(MDC.get(MdcRequestFilter.AUTH_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("still sets the correlation id on public routes where there is no valid token")
    void survivesAnUnauthenticatedRequest() {
        stubRequest("abc-123");
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM))
                .thenThrow(new IllegalStateException("no token on this request"));

        assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();

        assertThat(MDC.get(MdcRequestFilter.CORRELATION_ID_KEY)).isEqualTo("abc-123");
        assertThat(MDC.get(MdcRequestFilter.AUTH_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("echoes the correlation id on the response and clears the whole MDC")
    void echoesTheIdAndClearsTheMdc() {
        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);

        MDC.put(MdcRequestFilter.CORRELATION_ID_KEY, "abc-123");
        MDC.put(MdcRequestFilter.AUTH_ID_KEY, AUTH_ID);
        MDC.put(MdcRequestFilter.ROLE_KEY, "ADMIN");

        filter.filter(requestContext, responseContext);

        assertThat(responseHeaders.getFirst(MdcRequestFilter.CORRELATION_ID_HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(MdcRequestFilter.CORRELATION_ID_KEY)).isNull();
        assertThat(MDC.get(MdcRequestFilter.AUTH_ID_KEY)).isNull();
        assertThat(MDC.get(MdcRequestFilter.ROLE_KEY)).isNull();
    }

    @Test
    @DisplayName("does not touch the response headers when the MDC has no correlation id")
    void skipsTheHeaderWithoutACorrelationId() {
        // MDC is empty here, so the filter must not even look the headers up.
        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).getHeaders();
    }

    private void stubRequest(String correlationIdHeader) {
        when(requestContext.getHeaderString(MdcRequestFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationIdHeader);
        when(requestContext.getHeaders()).thenReturn(requestHeaders);
    }
}
