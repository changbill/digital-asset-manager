package digital.asset.manager.application.user.dto.response;


import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.dto.User;

// 로그인, 회원정보 수정, 프로필 이미지 수정에서 사용할 Response
public record UserDefaultResponse(
        String profileImageUrl,
        String email,
        ProviderType providerType,
        RoleType roleType,
        String name,
        String nickname
) {

    public static UserDefaultResponse fromUser(User dto) {
        String profileImageUrl = "https://changbill.github.io/assets/digital-asset-manager/default-profile.png";   // TODO: s3 url 등록 및 작성
        if(dto.profileImageUrl() != null) {
            profileImageUrl = dto.profileImageUrl();
        }

        return new UserDefaultResponse(
                profileImageUrl,
                dto.email(),
                dto.providerType(),
                dto.roleType(),
                dto.name(),
                dto.nickname()
        );
    }
}
