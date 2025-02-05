package digital.asset.manager.application.user.dto.response;

public record UserLoginResponse(
        UserDefaultResponse user,
        String token
) {
}
