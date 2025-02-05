package digital.asset.manager.application.user.controller;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import digital.asset.manager.application.common.response.Response;
import digital.asset.manager.application.user.dto.User;
import digital.asset.manager.application.user.dto.request.EmailRequest;
import digital.asset.manager.application.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email 컨트롤러", description = "이메일 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmailController {
    private final UserService userService;

    @Operation(
            summary = "이메일로 닉네임 찾기",
            description = "email의 정보로 닉네임 찾기 - 소셜로그인 때 사용"
    )
    @PostMapping("/email/find-nickname")
    public Response<String> findNickname(@RequestBody @Valid EmailRequest request) {
        User user = userService.loadUserByEmail(request.email()).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND));
        return Response.success(user.nickname());
    }

    @Operation(
            summary = "이메일 인증코드 요청",
            description = "email의 정보를 받아 현재 DB에 저장되어 있지 않은 메일인지 체크 후 인증 코드를 해당 메일로 보냄"
    )
    @PostMapping("/email/verification-request")
    public Response<Void> sendMessage(@RequestBody @Valid EmailRequest request) {
        userService.sendCodeByEmail(request.email());
        return Response.success();
    }

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "응답이 true일 경우 검증 성공, 그 외에는 에러를 반환"
    )
    @GetMapping("/email/verification")
    public Response<Boolean> verifyEmailCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        return Response.success(userService.verifyEmailCode(email, code));
    }

    @Operation(
            summary = "중복된 이메일 확인체크(True일 경우 중복에 해당하지 않음)",
            description = "이미 가입된 이메일인지 체크한다. True일 경우 가입한 적이 없고, False일 경우 동일한 이메일로 가입한 기록이 있다."
    )
    @PostMapping("/email/check-exist")
    public Response<Boolean> checkDuplicateEmail(@RequestBody @Valid EmailRequest request) {
        return Response.success(userService.checkDuplicateEmail(request.email()));
    }
}
