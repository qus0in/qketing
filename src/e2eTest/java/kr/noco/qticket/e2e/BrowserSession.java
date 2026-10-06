package kr.noco.qticket.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public final class BrowserSession implements AutoCloseable {

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final ScreenshotHelper screenshots = new ScreenshotHelper();

    private BrowserSession(Playwright playwright, Browser browser, BrowserContext context) {
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = context.newPage();
    }

    static BrowserSession launch() {
        Playwright playwright = Playwright.create();
        try {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            return new BrowserSession(playwright, browser, browser.newContext());
        } catch (RuntimeException exception) {
            playwright.close();
            throw exception;
        }
    }

    public Page page() {
        return page;
    }

    public APIRequestContext request() {
        return context.request();
    }

    public String url(int port, String path) {
        return "http://127.0.0.1:" + port + path;
    }

    public void screenshot(String testName) {
        screenshots.capture(page, testName);
    }

    public void close() {
        try {
            context.close();
        } finally {
            try {
                browser.close();
            } finally {
                playwright.close();
            }
        }
    }
}
