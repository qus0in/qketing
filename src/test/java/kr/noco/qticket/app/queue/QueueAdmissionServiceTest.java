package kr.noco.qticket.app.queue;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** QueueAdmissionService memberId 형식 검증 (#40 T40-15). */
class QueueAdmissionServiceTest {

    private final QueueAdmissionPort admissions = mock(QueueAdmissionPort.class);
    private final QueueAdmissionService service = new QueueAdmissionService(admissions);

    @Test
    void givenInvalidMemberId_whenAdmit_thenInvalidInputWithoutCallingAdapter() {
        assertInvalidInput(() -> service.admit(7L, "bad id!"));
        assertInvalidInput(() -> service.admit(7L, "a/b"));
        assertInvalidInput(() -> service.admit(7L, "m".repeat(65)));
        verifyNoInteractions(admissions);
    }

    private void assertInvalidInput(ThrowingCallable executable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(executable)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
