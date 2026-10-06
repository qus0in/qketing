package kr.noco.qticket.ui.error;

import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;

final class RequestIdResolver {

    private static final String HEADER = "X-Request-Id";
    private static final String ATTRIBUTE = RequestIdResolver.class.getName() + ".requestId";
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private RequestIdResolver() {
    }

    static String resolve(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTRIBUTE);
        if (existing instanceof String requestId) return requestId;
        String candidate = request.getHeader(HEADER);
        String requestId = valid(candidate) ? candidate : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, requestId);
        return requestId;
    }

    private static boolean valid(String candidate) {
        return candidate != null && VALID_ID.matcher(candidate).matches();
    }
}
