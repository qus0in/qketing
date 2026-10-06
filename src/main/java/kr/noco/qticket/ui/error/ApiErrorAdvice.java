package kr.noco.qticket.ui.error;

import java.net.URI;
import java.util.Locale;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
@Order(1)
public class ApiErrorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiErrorAdvice.class);

    private final MessageSource messages;

    public ApiErrorAdvice(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException exception, HttpServletRequest request, Locale locale) {
        LOGGER.warn("API business error code={}", exception.getErrorCode());
        return problem(exception.getErrorCode(), request, locale);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request, Locale locale) {
        LOGGER.error("Unexpected API error", exception);
        return problem(ErrorCode.INTERNAL, request, locale);
    }

    private ProblemDetail problem(ErrorCode code, HttpServletRequest request, Locale locale) {
        String detail = ErrorMessageResolver.safeMessage(messages, code.getStatus(), locale);
        ProblemDetail result = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        result.setType(URI.create("urn:qticket:error:" + code.name().toLowerCase(Locale.ROOT)));
        result.setTitle(messages.getMessage(code.getMessageKey(), null, locale));
        result.setProperty("requestId", RequestIdResolver.resolve(request));
        return result;
    }
}
