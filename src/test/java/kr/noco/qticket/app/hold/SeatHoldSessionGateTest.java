package kr.noco.qticket.app.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** hold 획득 전 active session 확인 게이트 검증 (#40 T40-12). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeatHoldSessionGateTest {
    private static final AtomicLong IDS = new AtomicLong(9000L);
    @Autowired private SeatHoldService holds;
    @Autowired private QueueAdmissionService admissions;
    @DynamicPropertySource
    static void sessionTtl(DynamicPropertyRegistry registry) {
        registry.add("qticket.valkey.session.ttl", () -> "1s");
    }
    @Test
    void givenNoAdmission_whenHold_thenConflictWithoutHold() {
        Long pid = IDS.incrementAndGet();
        String holder = member();
        assertThatThrownBy(() -> holds.hold(pid, 1L, holder))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));
        assertThat(holds.release(pid, 1L, holder)).isFalse();
    }
    @Test
    void givenAdmitted_whenHold_thenSucceeds() {
        Long pid = IDS.incrementAndGet();
        String holder = member();
        admissions.admit(pid, holder);
        assertThat(holds.hold(pid, 2L, holder)).isTrue();
    }
    @Test
    void givenExpiredSession_whenHold_thenConflict() throws Exception {
        Long pid = IDS.incrementAndGet();
        String holder = member();
        admissions.admit(pid, holder);
        Thread.sleep(1300L);
        assertThatThrownBy(() -> holds.hold(pid, 3L, holder))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));
    }
    @Test
    void givenInvalidHolderId_whenHold_thenInvalidInput() {
        Long pid = IDS.incrementAndGet();
        assertThatThrownBy(() -> holds.hold(pid, 1L, "holder id!"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private String member() {
        return "member-" + UUID.randomUUID();
    }
}
