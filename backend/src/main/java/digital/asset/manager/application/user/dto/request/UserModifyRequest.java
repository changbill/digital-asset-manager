package digital.asset.manager.application.user.dto.request;

import java.time.LocalDate;

public record UserModifyRequest(
        String password,
        String name,
        String nickname,
        LocalDate birthday
) {
}
