package digital.asset.manager.application.image.dto;

import digital.asset.manager.application.image.domain.ImageEntity;
import digital.asset.manager.application.user.domain.UserEntity;
import javax.validation.constraints.NotNull;

public record Image(
        Long id,
        @NotNull UserEntity userEntity,
        @NotNull String url,
        String thumbnailUrl
) {
    public static Image fromEntity(ImageEntity imageEntity){
        if (imageEntity == null) return null;
        return new Image(
                imageEntity.getId(),
                imageEntity.getUserEntity(),
                imageEntity.getUrl(),
                imageEntity.getThumbnailUrl()
        );
    }

    public ImageEntity toEntity(){
        return ImageEntity.of(
                this.userEntity,
                this.url,
                this.thumbnailUrl
        );
    }
}
