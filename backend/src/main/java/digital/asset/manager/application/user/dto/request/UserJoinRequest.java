package digital.asset.manager.application.user.dto.request;

public record UserJoinRequest(
        String email,
        String password,
        String name,
        String nickname
) {
}
