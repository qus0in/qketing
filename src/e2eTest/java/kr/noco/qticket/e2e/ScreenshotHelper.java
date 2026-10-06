package kr.noco.qticket.e2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.microsoft.playwright.Page;

public final class ScreenshotHelper {

    private final Path directory = Path.of(System.getProperty("e2e.screenshots", "build/reports/e2e"));

    public Path capture(Page page, String testName) {
        createDirectory();
        Path path = directory.resolve(safeName(testName) + ".png").toAbsolutePath();
        page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(path));
        return path;
    }

    private void createDirectory() {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String safeName(String testName) {
        return testName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
