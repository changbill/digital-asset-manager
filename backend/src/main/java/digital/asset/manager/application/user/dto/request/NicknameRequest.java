package digital.asset.manager.application.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameRequest(
        @Schema(description = "사용자 닉네임", example = "abc_def")
        String nickname
) {
}
