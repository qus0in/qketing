package kr.noco.qticket.app.validation;

import java.util.regex.Pattern;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

/** Valkey 키 경로에 들어가는 외부 식별자(memberId·holderId) 형식 검증 (#40 T40-15). */
public final class ExternalIdentifier {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private ExternalIdentifier() {
    }

    public static void requireValid(String value) {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
