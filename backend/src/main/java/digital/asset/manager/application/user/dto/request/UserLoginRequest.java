package digital.asset.manager.application.user.dto.request;

public record UserLoginRequest(
        String email,
        String password
) {
}
