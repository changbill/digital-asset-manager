package digital.asset.manager.application.global.oauth.handler;

import digital.asset.manager.application.common.config.properties.AppProperties;
import digital.asset.manager.application.global.auth.dto.BoardPrincipal;
import digital.asset.manager.application.global.auth.util.AuthToken;
import digital.asset.manager.application.global.auth.util.AuthTokenProvider;
import digital.asset.manager.application.global.oauth.domain.OAuth2UserInfo;
import digital.asset.manager.application.global.oauth.domain.OAuth2UserInfoFactory;
import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.global.oauth.repository.OAuth2AuthorizationRequestBasedOnCookieRepository;
import digital.asset.manager.application.global.oauth.util.CookieUtils;
import digital.asset.manager.application.user.domain.RoleType;
import digital.asset.manager.application.user.domain.UserRefreshToken;
import digital.asset.manager.application.user.repository.UserRefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import static digital.asset.manager.application.global.oauth.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;
import static digital.asset.manager.application.global.oauth.repository.OAuth2AuthorizationRequestBasedOnCookieRepository.REFRESH_TOKEN;

/**
 * OAuth2 인증 성공 시 호출되는 핸들러. 처음 프론트엔드에서 백엔드로 로그인 요청시 mode 쿼리 파라미터에 담긴 값에 따라 분기하여 처리.
 * mode 값이 login이면 사용자 정보를 DB에 저장하고, 서비스 자체 액세스 토큰, 리프레시 토큰을 생성하고, 리프레시 토큰을 DB에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestBasedOnCookieRepository;

    // OAuth2 인증 성공 시 onAuthenticationSuccess() 실행
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already bean committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new IllegalArgumentException("인증되지 않은 redirect URI");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        ProviderType providerType = ProviderType.valueOf(authToken.getAuthorizedClientRegistrationId().toUpperCase());

        BoardPrincipal user = (BoardPrincipal) authentication.getPrincipal();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, user.getAttributes());
        Collection<? extends GrantedAuthority> authorities = ((OidcUser) authentication.getPrincipal()).getAuthorities();
        // 권한이 있다면 ADMIN 권한을, 없다면 USER 권한을 부여
        RoleType roleType = hasAuthority(authorities, RoleType.ADMIN.getCode()) ? RoleType.ADMIN : RoleType.USER;

        // Access 토큰 생성
        AuthToken accessToken = tokenProvider.createAuthToken(userInfo.getEmail(), roleType.getCode(), appProperties.getAuth().getTokenExpiry());

        // Refresh 토큰 생성
        long refreshTokenExpiry = appProperties.getAuth().getRefreshTokenExpiry();
        AuthToken refreshToken = tokenProvider.createAuthToken(appProperties.getAuth().getTokenSecret(), appProperties.getAuth().getTokenExpiry());

        //Refresh 토큰 DB 저장
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserId(userInfo.getEmail());
        if (userRefreshToken != null) {  //이미 저장되어 있다면 값만 수정
            userRefreshToken.setRefreshToken(refreshToken.getToken());
        } else {
            userRefreshToken = new UserRefreshToken(userInfo.getEmail(), refreshToken.getToken());
            userRefreshTokenRepository.saveAndFlush(userRefreshToken);  // TODO: save()로 해도 문제없는지 확인할 것
        }
        log.debug("리프레시 토큰 {} : {}", userInfo, userRefreshToken);

        int cookieMaxAge = (int) refreshTokenExpiry / 60;
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
        CookieUtils.addCookie(response, REFRESH_TOKEN, refreshToken.getToken(), cookieMaxAge);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", accessToken.getToken())
                .build().toUriString();
    }

    // OAuth2 인증이 성공시 프론트엔드에서 특정 URI로 리디렉트하는데 인증되지 않은 URI로 리디렉트되는 보안 문제(CSRF)를 방지
    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);    // host와 port 추출 (URI 객체)
        return appProperties.getOAuth2().getAuthorizedRedirectUris()    // application.yml 에서 미리 정의된 허용된 리디렉션 URI 목록
                .stream()
                .anyMatch(authorizedRedirectUri -> {
                    URI authorizeURI = URI.create(authorizedRedirectUri);
                    return authorizeURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost()) && authorizeURI.getPort() == clientRedirectUri.getPort();
                });
    }

    private boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String authority) {
        if (authorities == null) {
            return false;
        }

        for (GrantedAuthority grantedAuthority : authorities) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestBasedOnCookieRepository.removeAuthorizationRequestCookies(request, response);
    }
}
