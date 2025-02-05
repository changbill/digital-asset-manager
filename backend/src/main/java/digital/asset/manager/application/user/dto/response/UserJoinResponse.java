package digital.asset.manager.application.user.dto.response;

import digital.asset.manager.application.user.dto.User;

public record UserJoinResponse(
        Long id,
        String email,
        String name,
        String nickname
) {
    public static UserJoinResponse fromUser(User dto) {
        return new UserJoinResponse(
                dto.id(),
                dto.email(),
                dto.name(),
                dto.nickname()
        );
    }
}
