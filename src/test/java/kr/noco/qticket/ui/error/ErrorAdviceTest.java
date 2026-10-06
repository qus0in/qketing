package kr.noco.qticket.ui.error;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import java.util.Locale;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.ui.home.HomeController;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ErrorAdviceTest {

    @Test
    void givenNotFound_whenApiCalled_thenProblemDetailReturned() throws Exception {
        MockMvc mvc = apiMvc();
        mvc.perform(get("/api/test/not-found").header("X-Request-Id", "test-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "test-123"))
                .andExpect(jsonPath("$.type", containsString("not_found")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Safe error message"))
                .andExpect(jsonPath("$.requestId").value("test-123"));
    }

    @Test
    void givenNotFound_whenSsrCalled_thenNotFoundViewRendered() throws Exception {
        MockMvc mvc = ssrMvc(new BusinessException(ErrorCode.NOT_FOUND));
        mvc.perform(get("/")).andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("message", "Safe error message"));
    }

    @Test
    void givenUnexpectedFailure_whenSsrCalled_thenSafeServerErrorRendered() throws Exception {
        MockMvc mvc = ssrMvc(new IllegalStateException("secret stack detail"));
        mvc.perform(get("/")).andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("message", "Safe error message"));
    }

    private MockMvc apiMvc() {
        MessageSource messages = messages();
        return MockMvcBuilders.standaloneSetup(new ApiFailureController())
                .setControllerAdvice(new ApiErrorAdvice(messages))
                .addFilters(new RequestIdMdcFilter()).build();
    }

    private MockMvc ssrMvc(RuntimeException failure) throws Exception {
        MessageSource messages = messages();
        when(messages.getMessage(eq("home.title"), isNull(), any(Locale.class))).thenThrow(failure);
        return MockMvcBuilders.standaloneSetup(new HomeController(messages))
                .setControllerAdvice(new SsrErrorAdvice(messages)).build();
    }

    private MessageSource messages() {
        MessageSource source = mock(MessageSource.class);
        when(source.getMessage("error.contentFallback", null, Locale.ENGLISH)).thenReturn("Safe error message");
        when(source.getMessage("error.404.title", null, Locale.ENGLISH)).thenReturn("Not found");
        when(source.getMessage("error.404.message", null, Locale.ENGLISH)).thenReturn("Safe error message");
        when(source.getMessage("error.500.title", null, Locale.ENGLISH)).thenReturn("Server error");
        when(source.getMessage("error.500.message", null, Locale.ENGLISH)).thenReturn("Safe error message");
        return source;
    }

    @RestController
    static class ApiFailureController {
        @GetMapping("/api/test/not-found")
        String notFound() {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }
}
