package digital.asset.manager.application.user.domain;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 계정 정보를 관리하는 JPA 엔티티
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@Table(name = "\"user_account\"")
@SQLDelete(sql = "UPDATE user_account SET deleted_at = NOW() where id=?")   // soft delete 구현
@Where(clause = "deleted_at is NULL")   // soft delete 된 데이터 제외하고 조회
public class UserEntity {

    @JsonIgnore     // 직렬화 방지(id를 API 응답에서 제외)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @NotNull: spring 애플리케이션에서 미리 검증하여 null의 경우 400 Bad Request로 막힘
     * @Column(nullable = false): DB 차원에서 무조건 NULL 방지
     * @NotNull(Bean Validation) 애노테이션 역시 DDL로 변환.
     * 결론: @NotNull을 쓰는 편이 스프링과 DB 둘다 관리할 수 있어 더욱 안정적이다.
      */
    @Column(length = 255, unique = true)  // 이메일 유니크하게 저장
    @NotNull
    private String email;

    @Column(name = "provider_type", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private ProviderType providerType;

    @Column(name = "role_type", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private RoleType roleType;

    @JsonIgnore
    @Setter
    @NotNull
    private String password;

    @Setter
    @Column(length = 50)
    @NotNull
    private String name;

    @Setter
    @Column(length = 32, unique = true)
    @NotNull
    private String nickname;

    @Setter
    @Column(name = "birth_date", length = 20)
    private LocalDate birthDate;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private LocalDateTime deletedAt;

    @Setter
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @PrePersist
    void createdAt() {
        this.createdAt = LocalDateTime.from(LocalDateTime.now());
    }

    @PreUpdate
    void modifiedAt() {
        this.modifiedAt = LocalDateTime.from(LocalDateTime.now());
    }

    private UserEntity(
            String email,
            ProviderType providerType,
            RoleType roleType,
            String password,
            String name,
            String nickname,
            LocalDate birthDate,
            String profileImageUrl
    ) {
        this.email = email;
        this.providerType = providerType;
        this.roleType = roleType;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.profileImageUrl = profileImageUrl;
    }

    public static UserEntity of(String email, ProviderType providerType, String password, String name, String nickname) {
        return of(email, providerType, RoleType.USER, password, name, nickname, null, null);
    }

    public static UserEntity of(String email, ProviderType providerType, String password, String name, String nickname, String profileImageUrl) {
        return of(email, providerType, RoleType.USER, password, name, nickname, null, profileImageUrl);
    }

    public static UserEntity of(String email, ProviderType providerType, RoleType roleType, String password, String name, String nickname, LocalDate birthDate, String profileImageUrl) {
        return new UserEntity(email, providerType, roleType, password, name, nickname, birthDate, profileImageUrl);
    }

}
