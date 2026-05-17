package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.common.api.RequestIdFilter;
import com.example.aftersale.common.observability.ObservabilityConstants;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatedRequestIdIsAvailableDuringRequestAndClearedAfterCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] observedRequestId = new String[1];
        FilterChain chain = (servletRequest, servletResponse) ->
                observedRequestId[0] = MDC.get(ObservabilityConstants.REQUEST_ID);

        filter.doFilter(request, response, chain);

        assertThat(observedRequestId[0]).isNotBlank();
        assertThat(response.getHeader(ObservabilityConstants.REQUEST_ID_HEADER)).isEqualTo(observedRequestId[0]);
        assertThat(MDC.get(ObservabilityConstants.REQUEST_ID)).isNull();
    }

    @Test
    void providedRequestIdIsAvailableDuringRequestAndClearedAfterCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(ObservabilityConstants.REQUEST_ID_HEADER, "REQ-PROVIDED-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] observedRequestId = new String[1];
        FilterChain chain = (servletRequest, servletResponse) ->
                observedRequestId[0] = MDC.get(ObservabilityConstants.REQUEST_ID);

        filter.doFilter(request, response, chain);

        assertThat(observedRequestId[0]).isEqualTo("REQ-PROVIDED-1");
        assertThat(response.getHeader(ObservabilityConstants.REQUEST_ID_HEADER)).isEqualTo("REQ-PROVIDED-1");
        assertThat(MDC.get(ObservabilityConstants.REQUEST_ID)).isNull();
    }
}
