package kr.noco.qticket.app.queue;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import kr.noco.qticket.domain.queue.AdmissionStatus;
import org.springframework.stereotype.Service;

@Service
public class QueueAdmissionService {

    private final QueueAdmissionPort admissions;

    public QueueAdmissionService(QueueAdmissionPort admissions) {
        this.admissions = admissions;
    }

    public AdmissionStatus admit(Long performanceId, String memberId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ExternalIdentifier.requireValid(memberId);
        return admissions.admit(performanceId, memberId);
    }
}
