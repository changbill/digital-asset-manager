package digital.asset.manager.application.image.domain;

import digital.asset.manager.application.image.dto.Image;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"image\"")
@Getter
@ToString
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotNull
    @Column(nullable = false, length = 512)
    private String url;

    @Setter
    @Column(length = 512)
    private String thumbnailUrl;

    protected ImageEntity() {
    }

    private ImageEntity(String url, String thumbnailUrl) {
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static ImageEntity of(String url, String thumbnailUrl) {
        return new ImageEntity(url, thumbnailUrl);
    }

    public static ImageEntity of(String url) {
        return new ImageEntity(url, null);
    }

    public static ImageEntity fromDto(Image dto) {
        return of(
                dto.url(),
                dto.thumbnailUrl()
        );
    }

}
