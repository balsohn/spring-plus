package org.example.expert.domain.log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.expert.domain.common.entity.Timestamped;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "log")
public class ManagerLog extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestUserId;
    private Long todoId;
    private Long targetManagerUserId;
    private String requestType;
    private String status;
    private String errorMessage;

    public ManagerLog(Long requestUserId, Long todoId, Long targetManagerUserId, String requestType, String status, String errorMessage) {
        this.requestUserId = requestUserId;
        this.todoId = todoId;
        this.targetManagerUserId = targetManagerUserId;
        this.requestType = requestType;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // 성공 로그 생성용 생성자
    public static ManagerLog createSuccessLog(Long requestUserId, Long todoId, Long targetManagerUserId) {
        return new ManagerLog(requestUserId, todoId, targetManagerUserId,
                "MANAGER_REGISTER", "SUCCESS", null);
    }

    // 실패 로그 생성용 생성자
    public static ManagerLog createFailedLog(Long requestUserId, Long todoId, Long targetManagerUserId, String errorMessage) {
        return new ManagerLog(requestUserId, todoId, targetManagerUserId,
                "MANAGER_REGISTER", "FAILED", errorMessage);
    }
}
