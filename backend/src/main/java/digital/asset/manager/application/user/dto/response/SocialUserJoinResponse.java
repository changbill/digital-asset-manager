package digital.asset.manager.application.user.dto.response;

import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.user.dto.User;

public record SocialUserJoinResponse(
        Long id,
        String email,
        ProviderType providerType,
        String name,
        String nickname
) {
    public static SocialUserJoinResponse fromUser(User dto) {
        return new SocialUserJoinResponse(
                dto.id(),
                dto.email(),
                dto.providerType(),
                dto.name(),
                dto.nickname()
        );
    }
}
