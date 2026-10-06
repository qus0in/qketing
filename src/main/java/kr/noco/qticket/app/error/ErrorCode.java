package kr.noco.qticket.app.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "error.404.title"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.403.title"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.generic.title"),
    CONFLICT(HttpStatus.CONFLICT, "error.generic.title"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "error.500.title");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
