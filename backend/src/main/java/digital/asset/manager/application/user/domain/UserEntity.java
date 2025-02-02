package digital.asset.manager.application.user.domain;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
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
     * DTO에서는 @NotNull, 엔티티에서는 @Column(nullable = false)를 함께 사용하는 것이 가장 좋은 설계
      */
    @Column(nullable = false, length = 255, unique = true)  // 이메일 유니크하게 저장
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
    @Column(nullable = false)
    private String password;

    @Setter
    @Column(nullable = false, length = 50)
    private String name;

    @Setter
    @Column(nullable = false, length = 32, unique = true)
    private String nickname;

    @Setter
    private LocalDate birthday;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private LocalDateTime deletedAt;

    /**
     * UserEntity와 ImageEntity가 무조건 함께 필요한 경우라면 EAGER도 가능하다.
     * 여기서는 UserEntity를 한번 가져오면 그 뒤로는 Redis에서 캐시하여 사용하므로 EAGER도 상관없다.
     */
    @Setter
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "image")
    private ImageEntity imageEntity;

    @PrePersist
    void createdAt() {
        this.createdAt = LocalDateTime.from(LocalDateTime.now());
    }

    @PreUpdate
    void modifiedAt() {
        this.modifiedAt = LocalDateTime.from(LocalDateTime.now());
    }

    protected UserEntity() {
    }

    private UserEntity(
            String email,
            ProviderType providerType,
            RoleType roleType,
            String password,
            String name,
            String nickname,
            LocalDate birthday,
            ImageEntity imageEntity
    ) {
        this.email = email;
        this.providerType = providerType;
        this.roleType = roleType;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.birthday = birthday;
        this.imageEntity = imageEntity;
    }

    public static UserEntity of(String email, ProviderType providerType, String password, String name, String nickname) {
        return of(email, providerType, RoleType.USER, password, name, nickname, null, null);
    }

    public static UserEntity of(String email, ProviderType providerType, String password, String name, String nickname, ImageEntity imageEntity) {
        return of(email, providerType, RoleType.USER, password, name, nickname, null, DisclosureType.VISIBLE, null, imageEntity);
    }

    public static UserEntity of(String email, ProviderType providerType, RoleType roleType, String password, String name, String nickname, String memo, DisclosureType disclosureType, LocalDate birthday, ImageEntity imageEntity) {
        return new UserEntity(email, providerType, roleType, password, name, nickname, memo, disclosureType, birthday, imageEntity);
    }

}
