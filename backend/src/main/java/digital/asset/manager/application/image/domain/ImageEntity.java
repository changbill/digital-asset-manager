package digital.asset.manager.application.image.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import digital.asset.manager.application.image.dto.Image;
import digital.asset.manager.application.user.domain.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "\"image\"")
@Getter
@ToString
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(nullable = false)
    @OneToOne(mappedBy = "imageEntity", orphanRemoval = true)
    private UserEntity userEntity;

    @Setter
    @Column(nullable = false, length = 512)
    private String url;

    @Setter
    @Column(length = 512)
    private String thumbnailUrl;

    private ImageEntity(UserEntity userEntity, String url, String thumbnailUrl) {
        this.userEntity = userEntity;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static ImageEntity of(UserEntity userEntity, String url, String thumbnailUrl) {
        return new ImageEntity(userEntity, url, thumbnailUrl);
    }

    public static ImageEntity of(UserEntity userEntity, String url) {
        return new ImageEntity(userEntity, url, null);
    }

    public static ImageEntity fromDto(Image dto) {
        return of(
                dto.userEntity(),
                dto.url(),
                dto.thumbnailUrl()
        );
    }

}
