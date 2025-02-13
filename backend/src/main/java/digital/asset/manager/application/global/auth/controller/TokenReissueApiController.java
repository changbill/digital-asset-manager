package digital.asset.manager.application.global.auth.controller;

import digital.asset.manager.application.global.annotation.ExtractPayload;
import digital.asset.manager.application.global.annotation.ExtractToken;
import digital.asset.manager.application.global.auth.dto.TokenResponseDto;
import digital.asset.manager.application.global.auth.service.TokenReissueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "토큰 재발급", description = "AccessToken 만료로 RefreshToken을 통해 재발급 요청하는 API")
@RestController
@RequestMapping("/api/v1/token/reissue")
@RequiredArgsConstructor
public class TokenReissueApiController {
    private final TokenReissueService tokenReissueService;

    @PostMapping
    public ResponseEntity<TokenResponseDto> reissueTokens(@ExtractPayload Long userId, @ExtractToken String refreshToken) {
        return ResponseEntity.ok(tokenReissueService.reissueTokens(userId, refreshToken));
    }
}
