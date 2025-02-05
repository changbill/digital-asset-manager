package digital.asset.manager.application.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record EmailRequest(
        @Schema(description = "사용자 이메일", nullable = false, example = "example@example.com")
        @Email
        @NotEmpty(message = "이메일을 입력해주세요.")
        String email
) {
}