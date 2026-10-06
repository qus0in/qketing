package kr.noco.qticket.ui.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class MvcInfrastructureErrorAdviceTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void givenUnknownHtmlPath_whenRequested_thenNotFoundErrorViewIsRendered() throws Exception {
        mvc().perform(get("/no-such-page").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(not(containsString("&lt;title"))))
                .andExpect(content().string(containsString(
                        "<h1 id=\"error-title\">페이지를 찾을 수 없습니다</h1>")));
    }

    @Test
    void givenUnknownJsonPath_whenRequested_thenProblemDetailIncludesRequestId() throws Exception {
        MvcResult result = mvc().perform(get("/no-such-page").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.type").value("urn:qticket:error:http-404"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("페이지를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.detail").value("요청한 페이지를 찾을 수 없습니다. 주소를 확인해 주세요."))
                .andReturn();
        String headerId = result.getResponse().getHeader("X-Request-Id");
        String bodyId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");
        assertEquals(headerId, bodyId);
    }

    @Test
    void givenPostAndHtmlAccept_whenGetOnlyPagePosted_thenAllowHeaderIsPreserved() throws Exception {
        mvc().perform(post("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(view().name("error"));
    }

    @Test
    void givenPostAndJsonAccept_whenGetOnlyPagePosted_thenAllowHeaderIsPreserved() throws Exception {
        mvc().perform(post("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new RequestIdMdcFilter()).build();
    }
}
