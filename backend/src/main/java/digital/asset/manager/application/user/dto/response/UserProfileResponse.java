package digital.asset.manager.application.user.dto.response;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.dto.User;

import java.time.LocalDate;

// 프로필 자세히 보기에서 사용할 Response
public record UserProfileResponse(
        String profileImageUrl,
        String email,
        ProviderType providerType,
        RoleType roleType,
        String name,
        String nickname,
        LocalDate birthDate

) {

    public static UserProfileResponse fromUser(User dto) {
        String profileImageUrl = "https://changbill.github.io/assets/digital-asset-manager/default-profile.png";   // TODO: s3 url 등록 및 작성
        if(dto.profileImageUrl() != null) {
            profileImageUrl = dto.profileImageUrl();
        }

        return new UserProfileResponse(
                profileImageUrl,
                dto.email(),
                dto.providerType(),
                dto.roleType(),
                dto.name(),
                dto.nickname(),
                dto.birthDate()
        );
    }
}
