package digital.asset.manager.application.user.service;

import digital.asset.manager.application.common.config.properties.AppProperties;
import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import digital.asset.manager.application.global.auth.util.AuthToken;
import digital.asset.manager.application.global.auth.util.AuthTokenProvider;
import digital.asset.manager.application.global.email.repository.EmailCacheRepository;
import digital.asset.manager.application.global.email.service.EmailService;
import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.global.oauth.util.CookieUtils;
import digital.asset.manager.application.global.oauth.util.HeaderUtils;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.domain.UserEntity;
import digital.asset.manager.application.user.domain.UserRefreshToken;
import digital.asset.manager.application.user.dto.User;
import digital.asset.manager.application.user.dto.request.UserModifyRequest;
import digital.asset.manager.application.user.dto.response.UserDefaultResponse;
import digital.asset.manager.application.user.dto.response.UserLoginResponse;
import digital.asset.manager.application.user.dto.response.UserProfileResponse;
import digital.asset.manager.application.user.repository.UserCacheRepository;
import digital.asset.manager.application.user.repository.UserRefreshTokenRepository;
import digital.asset.manager.application.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserCacheRepository userCacheRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final EmailCacheRepository emailCacheRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder encoder;

    private final AuthTokenProvider tokenProvider;
    private final AppProperties appProperties;

    @Value("${mail.auth-code-expired-ms}")
    private Long mailExpiredMs;
    private final static long THREE_DAYS_MS = 259200000;
    private final static String REFRESH_TOKEN_KEY = "refresh_token";

    public Optional<User> loadUserByEmail(String email) {
        return Optional.ofNullable(userCacheRepository.getUser(email)
                .orElseGet(() ->
                        userRepository.findByEmail(email)
                                .map(User::fromEntity)
                                .orElseThrow(() ->
                                        new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
                                )
                )
        );
    }

    // 이메일 인증 요청 시도
    @Transactional
    public void sendCodeByEmail(String email) {
        validateEmailPattern(email);
        validateEmailDuplication(email);
        String authCode = generateMailCode();  // 코드 생성

        emailService.sendEmail(email, authCode);  // 메일 보내기
        emailCacheRepository.setEmailCode(email, authCode, mailExpiredMs);  // 레디스에 저장
    }

    // 이메일 인증코드 검증
    public boolean verifyEmailCode(String email, String userAuthCode) {
        validateEmailPattern(email);
        validateEmailDuplication(email);
        String authCode = emailCacheRepository.getEmailCode(email).orElseThrow(() -> new ApplicationException(ErrorCode.NEVER_ATTEMPT_EMAIL_AUTH));

        if (!userAuthCode.equals(authCode)) {
            throw new ApplicationException(ErrorCode.INVALID_EMAIL_CODE);
        }
        return true;
    }

    // 이메일 중복 체크(참일 경우 중복되는 이메일 없음)
    public boolean checkDuplicateEmail(String email) {
        return !userRepository.existsByEmail(email);
    }

    // 닉네임 중복 체크(참일 경우 중복되는 닉네임 없음)
    public boolean checkDuplicateNickname(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    /**
     * 로그인
     * 1. JWT 토큰 생성
     * 2. 리프레시 토큰 생성
     * 3. 유저 - 리프레시 토큰이 있는지 확인
     * 4. 없으면 새로 등록
     * 5. 있으면 DB에 업데이트
     * 6. 기존 쿠키 삭제하고 새로 추가
     * 7. JWT 토큰 리턴
     */
    public UserLoginResponse login(HttpServletRequest request, HttpServletResponse response, String email, String password) {
        // 회원가입 여부 체크
        User user = loadUserByEmail(email).orElseThrow(
                () -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
        );
        userCacheRepository.setUser(user);
        // 비밀번호 체크
        if (!encoder.matches(password, user.password())) {
            throw new ApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        AuthToken accessToken = tokenProvider.createAuthToken(email, user.roleType().getCode(), appProperties.getAuth().getTokenExpiry());
        long refreshTokenExpiry = appProperties.getAuth().getRefreshTokenExpiry();
        AuthToken refreshToken = tokenProvider.createAuthToken(appProperties.getAuth().getTokenSecret(), refreshTokenExpiry);

        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserId(email);
        if (userRefreshToken == null) { // 토큰이 없는 경우. 새로 등록
            userRefreshToken = new UserRefreshToken(email, refreshToken.getToken());
            userRefreshTokenRepository.saveAndFlush(userRefreshToken);
        } else {  // 토큰 발견 -> 리프레시 토큰 업데이트
            userRefreshToken.setRefreshToken(refreshToken.getToken());
        }

        int cookieMaxAge = (int) refreshTokenExpiry / 60;
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN_KEY);
        CookieUtils.addCookie(response, REFRESH_TOKEN_KEY, refreshToken.getToken(), cookieMaxAge);
        UserDefaultResponse defaultResponse = UserDefaultResponse.fromUser(user);

        return new UserLoginResponse(defaultResponse, accessToken.getToken());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, String email) {
        //레디스에서 삭제
        userCacheRepository.deleteUser(email);
        // 리프레시 토큰 정보 삭제
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserId(email);
        userRefreshTokenRepository.delete(userRefreshToken);

        // 헤더 토큰 삭제
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN_KEY);
    }

    /**
     * 리프레시 토큰
     * 1. 액세스 토큰 기존 헤더에서 가져오기
     * 2. 액세스 토큰 (String)-> (Token)으로 변환
     * 3. 유효한 토큰인지 검증
     * 4. 만료된 토큰인지 검증
     * 5. claims에서 이메일 가져오기
     * 6. Claims에서 role타입 가져오기
     * 7. 프론트로 부터 쿠키에서 리프레시 토큰 가져오기
     * 8. 유효한지 확인
     * 9. 유저-리프레시 레포에서 토큰 가져오기 - 없을 경우 에러 - 로그인할 때 만들어지기 때문에 없을리 없음
     * 10. 새로운 액세스 토큰 발급
     * 11. 리프레시 토큰이 3일 이하로 남았을 경우 리프레시 토큰 갱신
     * 12. DB에 업데이트
     * 13. 액세스 토큰 리턴
     */
    public UserLoginResponse refreshToken(String email, HttpServletRequest request, HttpServletResponse response) {
        // 1. 헤더로 부터 액세스 토큰 가져오기
        String accessToken = HeaderUtils.getAccessToken(request);
        AuthToken authToken = tokenProvider.convertAuthToken(accessToken);
        User user = loadUserByEmail(email).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
        );
        UserDefaultResponse defaultResponse = UserDefaultResponse.fromUser(user);

        // 2-1. 토큰이 유효한지 체크
        if (authToken.validate()) {   // 유효하다면 지금 토큰 그대로 반환
            return new UserLoginResponse(defaultResponse, authToken.getToken());
        }

        // 2-2. 토큰이 유효하지 않다면 리프레시 토큰이 있는지 확인하자
        Claims claims = authToken.getExpiredClaims();  // 만료되었을 경우 만료 토큰을 가져옴.
        if (claims == null) {
            return new UserLoginResponse(defaultResponse, accessToken);  // 아직 만료 안됨
        }

        RoleType roleType = RoleType.of(claims.get("role", String.class));

        //refresh token
        String refreshToken = CookieUtils.getCookie(request, REFRESH_TOKEN_KEY)
                .map(Cookie::getValue)
                .orElse(null);
        AuthToken authRefreshToken = tokenProvider.convertAuthToken(refreshToken);

        if (!authRefreshToken.validate()) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB 확인
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserIdAndRefreshToken(email, refreshToken);
        if (userRefreshToken == null) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Date now = new Date();
        AuthToken newAccessToken = tokenProvider.createAuthToken(email, roleType.getCode(), appProperties.getAuth().getTokenExpiry());

        long validTime = authRefreshToken.extractClaims().getExpiration().getTime() - now.getTime();

        //refresh 토큰 기간이 3일 이하일 경우 새로 갱신
        if (validTime <= THREE_DAYS_MS) {
            authRefreshToken = tokenProvider.createAuthToken(
                    appProperties.getAuth().getTokenSecret(),
                    appProperties.getAuth().getRefreshTokenExpiry()
            );
            userRefreshToken.setRefreshToken(authRefreshToken.getToken());

            int cookieMaxAge = (int) appProperties.getAuth().getRefreshTokenExpiry() / 60;
            CookieUtils.deleteCookie(request, response, REFRESH_TOKEN_KEY);
            CookieUtils.addCookie(response, REFRESH_TOKEN_KEY, authRefreshToken.getToken(), cookieMaxAge);
        }


        return new UserLoginResponse(defaultResponse, newAccessToken.getToken());
    }

    // 회원가입 - 이메일 인증
    @Transactional
    public User join(String email, String password, String name, String nickname) {
        validateEmailPattern(email);
        validateEmailDuplication(email);
        if (satisfyNickname(nickname)) {
            UserEntity userEntity = UserEntity.of(email, ProviderType.LOCAL, encoder.encode(password), name, nickname, null);
            return User.fromEntity(userRepository.save(userEntity));
        }
        throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 회원가입 - 소셜로그인
    @Transactional
    public User join(String email, ProviderType providerType, String password, String name, String nickname) {
        validateEmailDuplication(email);
        if (satisfyNickname(nickname)) {
            UserEntity userEntity = UserEntity.of(email, providerType, encoder.encode(password), name, nickname);
            return User.fromEntity(userRepository.save(userEntity));
        }
        throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    //회원정보 조회
    @Transactional(readOnly = true)
    public UserProfileResponse my(String nickName) {
        UserEntity userEntity = userRepository.findByNickname(nickName).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", nickName)));
        User user = User.fromEntity(userEntity);
        return UserProfileResponse.fromUser(
                user
        );
    }

    //회원정보 수정
    @Transactional
    public UserProfileResponse updateMyProfile(String email, UserModifyRequest request) {

        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", email)));
        //닉네임 중복체크
        if (!userEntity.getNickname().equals(request.nickname())) {
            satisfyNickname(request.nickname());
            userEntity.setNickname(request.nickname());
        }
        if (request.password() != null) {
            userEntity.setPassword(encoder.encode(request.password()));
        }
        if (request.name() != null) {
            userEntity.setName(request.name());
        }
        if (request.birthDate() != null) {
            userEntity.setBirthDate(request.birthDate());
        }
        //null이어도 되는 필드
        userCacheRepository.updateUser(User.fromEntity(userEntity));
        userRepository.saveAndFlush(userEntity);    // TODO: save() 써도 되는지 확인

        User user = User.fromEntity(userEntity);
        return UserProfileResponse.fromUser(
                user
        );
    }

    //회원 탈퇴
    @Transactional
    public void delete(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(
                                ErrorCode.USER_NOT_FOUND,
                                String.format("%s is not founded", email)
                        )
                );

        userRepository.delete(userEntity);
    }

    //이메일 인증코드 생성
    private String generateMailCode() {
        int length = 6;
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("이메일 인증 코드 생성 중 에러 발생 : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean satisfyNickname(String nickname) {
        validateNicknamePattern(nickname);

        userRepository.findByNickname(nickname).ifPresent(it -> {
                    throw new ApplicationException(ErrorCode.DUPLICATED_USER_NICKNAME, String.format("%s is duplcated", nickname));
                }
        );
        return true;
    }

    private void validateEmailPattern(String email) {
        final String REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern regex = Pattern.compile(REGEX);
        Matcher matcher = regex.matcher(email);

        if (!matcher.matches()) {
            throw new ApplicationException(ErrorCode.UNSUITABLE_EMAIL, String.format("%s doesn't meet the conditions", email));
        }
    }

    private void validateNicknamePattern(String nickname) {
        //오직 소문자, 숫자, 4자 이상의 패턴만 가능
        final String REGEX = "^(?=.*[a-z])[a-z0-9_]{4,}$";
        Pattern regex = Pattern.compile(REGEX);
        Matcher matcher = regex.matcher(nickname);

        if (!matcher.matches()) {
            throw new ApplicationException(ErrorCode.UNSUITABLE_NICKNAME, String.format("%s doesn't meet the conditions", nickname));
        }
    }

    // 해당 이메일이 이미 존재하는 이메일인지 확인 체크
    private void validateEmailDuplication(String email) {
        if(userRepository.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.DUPLICATED_USER_EMAIL, String.format("%s is duplicated", email));
        }
    }

    // 닉네임으로 프로필 조회하기
    @Transactional
    public String getProfileImageUrl(String nickname) {
        UserEntity user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", nickname))
        );
        return user.getProfileImageUrl();
    }
}
