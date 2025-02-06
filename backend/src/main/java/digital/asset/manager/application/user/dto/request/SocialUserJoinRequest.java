package digital.asset.manager.application.user.dto.request;

import digital.asset.manager.application.global.oauth.domain.ProviderType;

public record SocialUserJoinRequest(
        String email,
        ProviderType providerType,
        String password,
        String name,
        String nickname
) {
}
