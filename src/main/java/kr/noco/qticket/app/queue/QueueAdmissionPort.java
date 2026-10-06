package kr.noco.qticket.app.queue;

import kr.noco.qticket.domain.queue.AdmissionStatus;

public interface QueueAdmissionPort {

    AdmissionStatus admit(Long performanceId, String memberId);

    boolean isActive(Long performanceId, String memberId);
}
