package digital.asset.manager.application.image.dto;

import digital.asset.manager.application.image.domain.ImageEntity;

public record Image(
        Long id,
        String url,
        String thumbnailUrl
) {
    public static Image fromEntity(ImageEntity imageEntity){
        if (imageEntity == null) return null;
        return new Image(
                imageEntity.getId(),
                imageEntity.getUrl(),
                imageEntity.getThumbnailUrl()
        );
    }

    public ImageEntity toEntity(){
        return ImageEntity.of(
                this.url,
                this.thumbnailUrl
        );
    }
}
