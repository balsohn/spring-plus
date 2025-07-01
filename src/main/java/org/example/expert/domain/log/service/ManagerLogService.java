package org.example.expert.domain.log.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.log.entity.ManagerLog;
import org.example.expert.domain.log.repository.ManagerLogRepository;
import org.example.expert.domain.manager.entity.Manager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerLogService {

    private final ManagerLogRepository managerLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccessLog(Long requestUserId, Long todoId, Long targetManagerUserId) {
        try {
            ManagerLog successLog = ManagerLog.createSuccessLog(requestUserId, todoId, targetManagerUserId);
            managerLogRepository.save(successLog);
            log.info("매니저 등록 성공 로그 저장 완료 - 요청자: {}, 할일: {}, 대상매니저: {}",
                    requestUserId, todoId, targetManagerUserId);
        } catch (Exception e) {
            log.error("성공 로그 저장 중 오류 발생", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedLog(Long requestUserId, Long todoId, Long targetManagerUserId, String errorMessage) {
        try {
            ManagerLog failedLog = ManagerLog.createFailedLog(requestUserId, todoId, targetManagerUserId, errorMessage);
            managerLogRepository.save(failedLog);
            log.info("매니저 등록 실패 로그 저장 완료 - 요청자: {}, 할일: {}, 대상메니저: {}, 오류: {}",
                    requestUserId, todoId, targetManagerUserId, errorMessage);
        } catch (Exception e) {
            log.error("실패 로그 저장 중 오류 발생", e);
        }
    }
}
