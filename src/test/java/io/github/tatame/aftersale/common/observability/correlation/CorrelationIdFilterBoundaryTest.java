package io.github.tatame.aftersale.common.observability.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.health.diskspace.enabled=false"
})
class CorrelationIdFilterBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void missingCorrelationAndRequestHeadersAreGeneratedForHealthEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        assertSafeResponseHeader(result, CorrelationHeaders.CORRELATION_ID_HEADER);
        assertSafeResponseHeader(result, CorrelationHeaders.REQUEST_ID_HEADER);
        assertThat(result.getResponse().getContentAsString()).contains("status");
    }

    @Test
    void safeCorrelationAndRequestHeadersAreReused() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationHeaders.CORRELATION_ID_HEADER, "corr.demo-001")
                        .header(CorrelationHeaders.REQUEST_ID_HEADER, "req.demo-001"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(CorrelationHeaders.CORRELATION_ID_HEADER))
                .isEqualTo("corr.demo-001");
        assertThat(result.getResponse().getHeader(CorrelationHeaders.REQUEST_ID_HEADER)).isEqualTo("req.demo-001");
    }

    @Test
    void unsafeCorrelationAndRequestHeadersAreNotEchoed() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationHeaders.CORRELATION_ID_HEADER, "token-demo")
                        .header(CorrelationHeaders.REQUEST_ID_HEADER, "https://example.invalid/request"))
                .andExpect(status().isOk())
                .andReturn();

        assertSafeResponseHeader(result, CorrelationHeaders.CORRELATION_ID_HEADER);
        assertSafeResponseHeader(result, CorrelationHeaders.REQUEST_ID_HEADER);
        assertThat(result.getResponse().getHeader(CorrelationHeaders.CORRELATION_ID_HEADER))
                .isNotEqualTo("token-demo");
        assertThat(result.getResponse().getHeader(CorrelationHeaders.REQUEST_ID_HEADER))
                .isNotEqualTo("https://example.invalid/request");
    }

    @Test
    void mdcValuesAreAvailableDuringRequestAndClearedAfterCompletion() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationHeaders.CORRELATION_ID_HEADER, "corr-unit-001");
        request.addHeader(CorrelationHeaders.REQUEST_ID_HEADER, "req-unit-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] observedCorrelationId = new String[1];
        String[] observedRequestId = new String[1];
        FilterChain chain = (servletRequest, servletResponse) -> {
            observedCorrelationId[0] = MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY);
            observedRequestId[0] = MDC.get(CorrelationHeaders.REQUEST_ID_MDC_KEY);
        };

        filter.doFilter(request, response, chain);

        assertThat(observedCorrelationId[0]).isEqualTo("corr-unit-001");
        assertThat(observedRequestId[0]).isEqualTo("req-unit-001");
        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).isEqualTo("corr-unit-001");
        assertThat(response.getHeader(CorrelationHeaders.REQUEST_ID_HEADER)).isEqualTo("req-unit-001");
        assertThat(MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationHeaders.REQUEST_ID_MDC_KEY)).isNull();
    }

    private static void assertSafeResponseHeader(MvcResult result, String headerName) {
        String value = result.getResponse().getHeader(headerName);

        assertThat(value).isNotBlank();
        assertThat(CorrelationIds.isSafe(value)).isTrue();
    }
}
