package kr.noco.qticket.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PlaywrightExtension.class)
class QticketE2ETest extends E2ETestSupport {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void homePageRendersAndCapturesScreenshot(BrowserSession browser) {
        Page page = browser.page();
        Response response = page.navigate(url("/"));
        assertEquals(200, response.status());
        assertEquals("QTicket · 홈", page.title());
        assertFalse(page.locator("h1").textContent().isBlank());
        browser.screenshot("homePageRendersAndCapturesScreenshot");
    }

    @Test
    void missingPageRendersHtmlAndCapturesScreenshot(BrowserSession browser) {
        Page page = browser.page();
        Response response = page.navigate(url("/no-such-page"));
        assertEquals(404, response.status());
        assertTrue(page.title().contains("페이지를 찾을 수 없습니다"));
        assertTrue(page.locator("h1").textContent().contains("페이지를 찾을 수 없습니다"));
        assertFalse(page.content().contains("&lt;title"));
        browser.screenshot("missingPageRendersHtmlAndCapturesScreenshot");
    }

    @Test
    void missingPageReturnsJsonProblemDetailWithRequestId(BrowserSession browser) throws Exception {
        APIResponse response = browser.request().get(url("/no-such-page"),
                RequestOptions.create().setHeader("Accept", "application/json")
                        .setHeader("X-Request-Id", "e2e-request-25"));
        JsonNode body = json.readTree(response.text());
        assertEquals(404, response.status());
        assertEquals(404, body.path("status").asInt());
        assertNotNull(body.path("requestId").textValue());
        assertEquals("e2e-request-25", response.headers().get("x-request-id"));
        assertEquals(response.headers().get("x-request-id"), body.path("requestId").textValue());
    }

    @Test
    void unsupportedMethodReturnsAllowHeader(BrowserSession browser) {
        APIResponse response = browser.request().post(url("/"),
                RequestOptions.create().setHeader("Accept", "text/html"));
        assertEquals(405, response.status());
        assertTrue(response.headers().get("allow").contains("GET"));
    }

    @Test
    void actuatorHealthReportsUp(BrowserSession browser) throws Exception {
        APIResponse response = browser.request().get(url("/actuator/health"),
                RequestOptions.create().setHeader("Accept", "application/json"));
        JsonNode body = json.readTree(response.text());
        assertEquals(200, response.status());
        assertEquals("UP", body.path("status").asText());
    }
}
