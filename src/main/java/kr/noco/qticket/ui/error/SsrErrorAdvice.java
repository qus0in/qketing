package kr.noco.qticket.ui.error;

import java.util.Locale;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = {
        "kr.noco.qticket.ui.home", "kr.noco.qticket.ui.auth",
        "kr.noco.qticket.ui.performance", "kr.noco.qticket.ui.queue",
        "kr.noco.qticket.ui.seat", "kr.noco.qticket.ui.booking",
        "kr.noco.qticket.ui.chat"
})
@Order(2)
public class SsrErrorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(SsrErrorAdvice.class);

    private final MessageSource messages;

    public SsrErrorAdvice(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    ModelAndView handleBusiness(BusinessException exception, HttpServletRequest request, Locale locale) {
        LOGGER.warn("SSR business error code={}", exception.getErrorCode());
        return render(exception.getErrorCode(), request, locale);
    }

    @ExceptionHandler(Exception.class)
    ModelAndView handleUnexpected(Exception exception, HttpServletRequest request, Locale locale) {
        LOGGER.error("Unexpected SSR error", exception);
        return render(ErrorCode.INTERNAL, request, locale);
    }

    private ModelAndView render(ErrorCode code, HttpServletRequest request, Locale locale) {
        HttpStatus status = code.getStatus();
        ModelAndView view = new ModelAndView(viewName(status));
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("title", messages.getMessage(code.getMessageKey(), null, locale));
        view.addObject("message", ErrorMessageResolver.safeMessage(messages, status, locale));
        view.addObject("requestId", RequestIdResolver.resolve(request));
        return view;
    }

    private String viewName(HttpStatus status) {
        if (status == HttpStatus.FORBIDDEN) return "error/403";
        if (status == HttpStatus.NOT_FOUND) return "error/404";
        if (status.is5xxServerError()) return "error/500";
        return "error";
    }
}
