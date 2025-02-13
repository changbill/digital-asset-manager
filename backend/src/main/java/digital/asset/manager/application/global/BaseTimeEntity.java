package digital.asset.manager.application.global;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;

@Getter
// 테이블로 생성되지 않고, 상속받는 엔티티 클래스의 테이블에 컬럼 추가
@MappedSuperclass
// Spring Data JPA의 Auditing을 사용하여 자동으로 날짜 기록
// Spring Boot 설정에서 @EnableJpaAuditing을 추가해야함
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
