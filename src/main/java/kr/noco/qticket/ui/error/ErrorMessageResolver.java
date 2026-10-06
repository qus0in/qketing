package kr.noco.qticket.ui.error;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

final class ErrorMessageResolver {

    private ErrorMessageResolver() {
    }

    static String safeMessage(MessageSource messages, HttpStatus status, Locale locale) {
        return messages.getMessage(messageKey(status), null, locale);
    }

    private static String messageKey(HttpStatus status) {
        if (status == HttpStatus.FORBIDDEN) return "error.403.message";
        if (status == HttpStatus.NOT_FOUND) return "error.404.message";
        if (status.is5xxServerError()) return "error.500.message";
        return "error.contentFallback";
    }
}
