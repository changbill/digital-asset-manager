package digital.asset.manager.application.user.domain;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import digital.asset.manager.application.image.domain.ImageEntity;
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

    /**
     * - @OneToMany(mappedBy = "fromUser") : 한 명의 사용자(UserEntity)가 여러 개의 팔로우(FollowEntity)를 가짐,
     * mappedBy = "fromUser" → FollowEntity의 fromUser 필드가 외래 키(FK) 관계를 관리함 (주인이 FollowEntity임)
     * - orphanRemoval = true : 부모 Entity인 UserEntity에서 FollowEntity를 제거하면 FollowEntity도 자동 삭제됨
     * CascadeType.REMOVE와 차이점: CascadeType.REMOVE는 부모 엔티티 자체가 삭제될 때 연관된 엔티티도 삭제됨
     * - cascade = CascadeType.ALL : 모든 영속성 전이 옵션 사용. UserEntity가 저장되거나 삭제될 때, FollowEntity도 함께 저장/삭제됨.
     *      - PERSIST → 부모 저장 시 자식도 저장
     *      - MERGE → 부모 병합 시 자식도 병합
     *      - REMOVE → 부모 삭제 시 자식도 삭제
     *      - REFRESH → 부모 새로고침 시 자식도 새로고침
     *      - DETACH → 부모가 영속성 컨텍스트에서 분리되면 자식도 분리
     * - fetch = FetchType.LAZY : user.getFollowEntities()를 호출하기 전까지 실제 데이터를 가져오지 않음
     */
//    @OneToMany(mappedBy = "fromUser", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<FollowEntity> followEntities = new ArrayList<>();

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
        return of(email, providerType, RoleType.USER, password, name, nickname, null, imageEntity);
    }

    public static UserEntity of(String email, ProviderType providerType, RoleType roleType, String password, String name, String nickname, LocalDate birthday, ImageEntity imageEntity) {
        return new UserEntity(email, providerType, roleType, password, name, nickname, birthday, imageEntity);
    }

}
