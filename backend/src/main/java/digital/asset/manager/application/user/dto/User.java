package digital.asset.manager.application.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.image.domain.ImageEntity;
import digital.asset.manager.application.image.dto.Image;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.domain.UserEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record User(
        Long id,
        String email,
        Image image,
        ProviderType providerType,
        RoleType roleType,
        String password,
        String name,
        String nickname,
        @JsonDeserialize(using = LocalDateDeserializer.class)
        @JsonSerialize(using = LocalDateSerializer.class)
        LocalDate birthday,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime createdAt,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime modifiedAt,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime deletedAt
) {
    public static User of(
            Long id,
            String email,
            ImageEntity imageEntity,
            ProviderType providerType,
            String password,
            String name,
            String nickname,
            LocalDate birthday,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt,
            LocalDateTime deletedAt
    ) {
        return new User(
                id,
                email,
                Image.fromEntity(imageEntity),
                providerType,
                RoleType.USER,
                password,
                name,
                nickname,
                birthday,
                createdAt,
                modifiedAt,
                deletedAt
        );
    }

    public static User fromEntity(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                Image.fromEntity(entity.getImageEntity()),
                entity.getProviderType(),
                entity.getRoleType(),
                entity.getPassword(),
                entity.getName(),
                entity.getNickname(),
                entity.getBirthday(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public UserEntity toEntity() {
        return UserEntity.of(
                email,
                providerType,
                roleType,
                name,
                password,
                nickname,
                birthday,
                ImageEntity.fromDto(image)
        );
    }

}
