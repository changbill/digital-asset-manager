package digital.asset.manager.application.user.dto.response;


import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.dto.User;

// 로그인, 회원정보 수정, 프로필 이미지 수정에서 사용할 Response
public record UserDefaultResponse(
        String imageUrl,
        String email,
        ProviderType providerType,
        RoleType roleType,
        String name,
        String nickname
) {

    public static UserDefaultResponse fromUser(User dto) {
        String imageUrl = "https://[s3-url 작성]/profiles/defaultProfileImage.png";   // TODO: s3 url 등록 및 작성
        if(dto.image() != null) {
            imageUrl = dto.image().url();
        }

        return new UserDefaultResponse(
                imageUrl,
                dto.email(),
                dto.providerType(),
                dto.roleType(),
                dto.name(),
                dto.nickname()
        );
    }
}
