package digital.asset.manager.application.global.oauth.service;


import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import digital.asset.manager.application.global.auth.dto.UserPrincipal;
import digital.asset.manager.application.global.exception.OAuth2AuthenticationProcessingException;
import digital.asset.manager.application.global.oauth.domain.OAuth2UserInfo;
import digital.asset.manager.application.global.oauth.domain.OAuth2UserInfoFactory;
import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.global.oauth.util.NicknameUtils;
import digital.asset.manager.application.user.domain.UserEntity;
import digital.asset.manager.application.user.dto.User;
import digital.asset.manager.application.user.repository.UserCacheRepository;
import digital.asset.manager.application.user.repository.UserRepository;
import digital.asset.manager.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * 스프링 시큐리티 OAuth2LoginAuthenticationFilter에서 시작된 OAuth2 인증 과정 중에 호출된다.
 * 호출 시점 : 액세스 토큰을 OAuth2 제공자로부터 받았을 때.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCacheRepository userCacheRepository;
    private final BCryptPasswordEncoder encoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        try {
            return process(userRequest, oAuth2User);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalAuthenticationServiceException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Access Token을 얻고 난 후
     * 토큰으로 유저 정보 받아오기
     * 해당 이메일로 된 계정이 없는 경우 -> 회원가입
     * 해당 이메일로 된 계정이 있는 경우 -> 토큰 발급
     */
    @Transactional
    protected OAuth2User process(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {

        ProviderType providerType = ProviderType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, oAuth2User.getAttributes());

        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }
        // 이미 가입한 유저인지 체크
        UserEntity savedUser = userRepository.findByEmail(userInfo.getEmail()).orElse(null);
        if (savedUser == null) {  // 없는 유저 회원가입 필요
            String dummyPassword = encoder.encode("{bcrypt}" + UUID.randomUUID());
            userService.join(userInfo.getEmail(), providerType, dummyPassword, userInfo.getName(), NicknameUtils.createRandomNickname(userInfo.getEmail()));
        }
        // 레디스에 유저 정보 저장 후 리턴
        return userRepository.findByEmail(userInfo.getEmail())
                // flatMap: Optional<UserEntity> -> Optional<Optional<User>> 로 만드는 과정중 Optional<User>로 평탄화
                .flatMap(userEntity -> {
                    User user = User.fromEntity(userEntity);
                    userCacheRepository.setUser(user);
                    return Optional.of(user);
                })
                .map(user -> UserPrincipal.from(user, oAuth2User.getAttributes()))
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userInfo.getEmail()))
                );
    }

}
