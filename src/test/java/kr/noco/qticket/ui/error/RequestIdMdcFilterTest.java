package kr.noco.qticket.ui.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestIdMdcFilterTest {

    @Test
    void givenRequest_whenHandled_thenMdcHeaderAndCleanupUseSameRequestId() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MdcProbeController())
                .addFilters(new RequestIdMdcFilter()).build();
        mvc.perform(get("/mdc-probe").header("X-Request-Id", "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "trace-123"))
                .andExpect(content().string("trace-123"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void givenInvalidHeaders_whenResolved_thenUuidReplacesThem() {
        assertGeneratedUuid("trace\r\nInjected: value");
        assertGeneratedUuid("a".repeat(65));
        assertGeneratedUuid("trace/invalid");
    }

    @Test
    void givenFilterChainThrows_whenFiltered_thenMdcIsCleared() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failingChain = (req, res) -> {
            assertEquals("trace-123", MDC.get("requestId"));
            throw new ServletException("failure");
        };
        assertThrows(ServletException.class,
                () -> new RequestIdMdcFilter().doFilter(request, response, failingChain));
        assertEquals("trace-123", response.getHeader("X-Request-Id"));
        assertNull(MDC.get("requestId"));
    }

    private void assertGeneratedUuid(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", header);
        String resolved = RequestIdResolver.resolve(request);
        UUID.fromString(resolved);
    }

    @RestController
    static class MdcProbeController {
        @GetMapping("/mdc-probe")
        String requestId() {
            return MDC.get("requestId");
        }
    }
}
