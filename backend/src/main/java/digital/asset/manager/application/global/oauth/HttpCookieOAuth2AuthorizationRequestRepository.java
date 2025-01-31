package digital.asset.manager.application.global.oauth;

import digital.asset.manager.application.global.oauth.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 인증과정 중 state, redirect_uri 등의 파라미터를 어딘가에 저장해야하는데 이를 쿠키에 저장하는 방식을 구현한 사용자 정의 클래스.
 * 스프링 빈으로 등록하고 SecurityConfig에서 authorizationRequestRepository로 설정
 * 스프링 시큐리티 OAuth2 관련필터인 OAuth2AuthorizationRequestRedirectFilter와 OAuth2LoginAuthenticationFilter에서 인증과정중에 호출
 *
 * 최초 프론트에서 로그인 요청시 리다이렉트할 OAuth2 제공자별 URL 정보를 쿠키에 저장하여 리다이렉트한다.
 * 그 이후 사용자가 로그인 성공시 백엔드로 리다이렉트될 때 인증 과정 및 사용자 정보를 불러오는 과정을 마친 후 쿠키에 저장된 정보를 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";    // OAuth2 인증 요청을 저장하는 쿠키 이름
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri"; // 로그인 후 리다이렉트할 URI를 저장하는 쿠키 이름
    public static final String MODE_PARAM_COOKIE_NAME = "mode"; // 로그인 모드를 저장하는 쿠키 이름 (예: 일반 로그인 vs. 소셜 로그인)
    private static final int COOKIE_EXPIRE_SECONDS = 180;   // 쿠키 유효 시간(초)

    /**
     * 인증 요청 불러오기
     * 요청에서 OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME 쿠키를 찾아서 OAuth2AuthorizationRequest 객체로 변환하여 반환
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     * 인증 요청 저장하기
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        // authorizationRequest가 null이면 기존에 저장된 쿠키들 삭제
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
            return;
        }

        // authorizationRequest가 있으면 OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME 쿠키에 저장
        CookieUtils.addCookie(response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS);

        // redirect_uri 파라미터가 있으면 REDIRECT_URI_PARAM_COOKIE_NAME 쿠키에 저장
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response,
                    REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin,
                    COOKIE_EXPIRE_SECONDS);
        }

        // mode 파라미터가 있으면 MODE_PARAM_COOKIE_NAME 쿠키에 저장
        String mode = request.getParameter(MODE_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(mode)) {
            CookieUtils.addCookie(response,
                    MODE_PARAM_COOKIE_NAME,
                    mode,
                    COOKIE_EXPIRE_SECONDS);
        }
    }

    /**
     * 인증 요청 제거하기
     * OAuth2LoginAuthenticationFilter에서 인증이 완료되면 호출.
     * 로그인 완료 후 loadAuthorizationRequest()서 호출한 인증 요청 정보를 통해 리다이렉트 URL로 이동
     * 바로 쿠키를 삭제하면 OAuth2 과정 중 정보가 날아갈 위험이 있기 때문에 별도로 존재
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    // 쿠키 삭제를 별도로 하는 이유
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
    }
}
