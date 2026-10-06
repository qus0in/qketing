package kr.noco.qticket.ui.error;

import java.net.URI;
import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Resolve MVC protocol errors first; API advice then wins over SSR for overlapping controllers.
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MvcInfrastructureErrorAdvice {

    private final MessageSource messages;

    public MvcInfrastructureErrorAdvice(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(value = {NoResourceFoundException.class, NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class}, produces = "text/html")
    ModelAndView handleHtml(Exception exception, HttpServletRequest request,
            HttpServletResponse response, Locale locale) {
        copyHeaders(exception, response);
        return render(status(exception), request, locale);
    }

    @ExceptionHandler(value = {NoResourceFoundException.class, NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class}, produces = {"application/json", "application/problem+json"})
    ResponseEntity<ProblemDetail> handleJson(Exception exception, HttpServletRequest request, Locale locale) {
        HttpStatus status = status(exception);
        HttpHeaders responseHeaders = headers(exception);
        return ResponseEntity.status(status).headers(target -> target.putAll(responseHeaders))
                .body(problem(status, request, locale));
    }

    private ModelAndView render(HttpStatus status, HttpServletRequest request, Locale locale) {
        ModelAndView view = new ModelAndView(viewName(status));
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("title", messages.getMessage(titleKey(status), null, locale));
        view.addObject("message", ErrorMessageResolver.safeMessage(messages, status, locale));
        view.addObject("requestId", RequestIdResolver.resolve(request));
        return view;
    }

    private ProblemDetail problem(HttpStatus status, HttpServletRequest request, Locale locale) {
        String detail = ErrorMessageResolver.safeMessage(messages, status, locale);
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setType(URI.create("urn:qticket:error:http-" + status.value()));
        result.setTitle(messages.getMessage(titleKey(status), null, locale));
        result.setProperty("requestId", RequestIdResolver.resolve(request));
        return result;
    }

    private void copyHeaders(Exception exception, HttpServletResponse response) {
        headers(exception).forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
    }

    private HttpHeaders headers(Exception exception) {
        if (exception instanceof HttpRequestMethodNotSupportedException methodException) {
            return methodException.getHeaders();
        }
        return HttpHeaders.EMPTY;
    }

    private HttpStatus status(Exception exception) {
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return HttpStatus.METHOD_NOT_ALLOWED;
        }
        return HttpStatus.NOT_FOUND;
    }

    private String titleKey(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) return "error.404.title";
        if (status == HttpStatus.FORBIDDEN) return "error.403.title";
        if (status.is5xxServerError()) return "error.500.title";
        return "error.generic.title";
    }

    private String viewName(HttpStatus status) {
        if (status == HttpStatus.FORBIDDEN) return "error/403";
        if (status == HttpStatus.NOT_FOUND) return "error/404";
        if (status.is5xxServerError()) return "error/500";
        return "error";
    }
}
