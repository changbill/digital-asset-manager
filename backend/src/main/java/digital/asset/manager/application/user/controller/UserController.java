package digital.asset.manager.application.user.controller;

import digital.asset.manager.application.common.response.Response;
import digital.asset.manager.application.user.dto.User;
import digital.asset.manager.application.user.dto.request.NicknameRequest;
import digital.asset.manager.application.user.dto.request.UserJoinRequest;
import digital.asset.manager.application.user.dto.request.UserLoginRequest;
import digital.asset.manager.application.user.dto.request.UserModifyRequest;
import digital.asset.manager.application.user.dto.response.UserJoinResponse;
import digital.asset.manager.application.user.dto.response.UserLoginResponse;
import digital.asset.manager.application.user.dto.response.UserProfileResponse;
import digital.asset.manager.application.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User 컨트롤러", description = "로그인, 회원가입, 팔로잉에 관한 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "email, password, name, nickname의 정보를 받아 회원가입을 진행한다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = UserJoinResponse.class)))
            }
    )
    @PostMapping("/users/join")
    public Response<UserJoinResponse> join(@RequestBody UserJoinRequest request) {
        User user = userService.join(request.email(), request.password(), request.name(), request.nickname());
        return Response.success(UserJoinResponse.fromUser(user));
    }

    @Operation(
            summary = "로그인",
            description = "유저의 email, password를 입력하여 로그인을 진행한다. 이후 JWT 토큰 발급",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공 및 JWT 토큰 발급", content = @Content(schema = @Schema(implementation = UserLoginResponse.class)))
            }
    )
    @PostMapping("/users/login")
    public Response<UserLoginResponse> login(HttpServletRequest request, HttpServletResponse response, @RequestBody UserLoginRequest userLoginRequest) {
        return Response.success(userService.login(request, response, userLoginRequest.email(), userLoginRequest.password()));
    }

    @Operation(
            summary = "로그아웃",
            description = "로그아웃 작업. 레디스 메모리에서 유저 정보 삭제, 리프레시 토큰 정보 삭제, 쿠키 삭제 등의 작업이 이뤄진다."
    )
    @PostMapping("/users/logout")
    public Response<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response, authentication.getName());
        return Response.success();
    }

    @Operation(
            summary = "토큰 리프레시",
            description = "액세스 토큰이 만료되었을 때 요청해야하는 API. 액세스 토큰이 만료가 되지 않았다면 기존 액세스 토큰을 다시 보내주고, " +
                    "만료가 되었다면 리프레시 토큰이 있는지 확인 후 새로 액세스 토큰을 발급해준다. 리프레시 토큰 기간이 3일 이하로 남았다면, 리프레시 토큰도 새롯 생성한다."
    )
    @GetMapping("/users/refresh")
    public Response<UserLoginResponse> refresh(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        return Response.success(userService.refreshToken(authentication.getName(), request, response));
    }

    @Operation(
            summary = "중복된 닉네임 확인 체크(참일 경우 중복 없음)",
            description = "이미 가입된 닉네임인지 체크한다. True일 경우 가입한 적이 없고, False일 경우 동일한 닉네임이 존재한다."
    )
    @PostMapping("/users/check-nickname")
    public Response<Boolean> checkDuplicateNickname(@RequestBody NicknameRequest request) {
        return Response.success(userService.checkDuplicateNickname(request.nickname()));
    }

    @Operation(
            summary = "프로필 보기",
            description = "프로필, 이름, 닉네임, 메모 등의 정보 등을 보여준다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "프로필 보기", content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
            }
    )
    @GetMapping("/users/{nickname}")
    public Response<UserProfileResponse> myProfile(Authentication authentication, @PathVariable(name = "nickname") String nickname) {
        return Response.success(userService.my(nickname));
    }

    @Operation(
            summary = "프로필 수정하기",
            description = "프로필, 비밀번호, 이름, 닉네임, 메모 계정 공개/비공개, 생일 등의 정보 등을 수정한다."
    )
    @PutMapping("/users")
    public Response<UserProfileResponse> updateMyProfile(Authentication authentication, @RequestBody UserModifyRequest request) {
        return Response.success(userService.updateMyProfile(authentication.getName(), request));
    }

    @Operation(
            summary = "계정 삭제하기"
    )
    @DeleteMapping("/users")
    public Response<Void> deleteUser(Authentication authentication) {
        userService.delete(authentication.getName());
        return Response.success();
    }
}
