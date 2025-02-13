package digital.asset.manager.application.common.config.security;

import digital.asset.manager.application.common.exception.CustomAuthenticationEntryPoint;
import digital.asset.manager.application.global.auth.config.AuthenticationConfig;
import digital.asset.manager.application.global.oauth.service.CustomOAuth2UserService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security의 SecurityFilterChain을 설정하여, 애플리케이션의 인증 및 인가 로직을 관리
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final CustomOAuth2UserService oAuth2UserService;
    private final AuthenticationConfig authenticationConfig;

    // AUTH_WHITELIST: Swagger, GraphQL 같은 문서 및 UI 관련 URL들을 인증없이 접근 가능하도록 허용
    private static final String[] AUTH_WHITELIST = {
            "/graphiql", "/graphql",
            "/swagger-ui/**", "/api-docs", "/swagger-ui-custom.html",
            "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html"
    };

    // OPEN_API_URLS: 회원가입, 로그인, 이메일 중복 체크, 팔로우 관련 API 등 공개 API를 인증없이 접근 가능하도록 허용
    private static final String[] OPEN_API_URLS = {
            "/price",
            "/api/*/users/join",
            "/api/*/users/social-join",
            "/api/*/users/login",
            "/api/*/users/check-nickname",
            "/api/*/email/**",
            "/api/*/users/{nickname}",
            "/api/*/{nickname}/request-profile",
            "/api/*/{nickname}/count-followers",
            "/api/*/{nickname}/count-followings"
    };

    /**
     * Spring Security의 필터 체인을 정의
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정(authenticationConfig에서 정의한 특정 도메인에서만 API 요청을 허용)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(authenticationConfig.corsConfigurationSource()))
                // JWT 및 OAuth2 기반 인증 CSRF 보호 기능(허용된 출처에서만 요청이 가능하도록 설정)을 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // OPEN_API_URLS 및 AUTH_WHITELIST에 포함된 URL은 모두 허용, /api/** 경로의 나머지 요청은 인증이 필요 (authenticated())
                .authorizeHttpRequests(authorize ->
                        authorize.requestMatchers(OPEN_API_URLS).permitAll()
                                .requestMatchers(AUTH_WHITELIST).permitAll()
                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                .requestMatchers("/api/**").authenticated()
                )
                // JWT 기반 인증이므로, 서버에서 세션을 관리하지 않고(Stateless) 요청마다 인증을 수행
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2Login(configure -> configure
                        // authorizationEndpoint: OAuth2 요청을 저장하는 Repository 설정
                        .authorizationEndpoint(config -> config
                                .authorizationRequestRepository(authenticationConfig.oAuth2AuthorizationRequestBasedOnCookieRepository())
                                .baseUri("/oauth2/authorization")
                        )
                        // userInfoEndpoint: OAuth2 로그인 후 사용자 정보를 가져오는 서비스 등록
                        .userInfoEndpoint(config -> config.userService(oAuth2UserService))
                        // redirectionEndpoint: OAuth2 인증 후 리디렉션할 URL 설정
                        .redirectionEndpoint(config -> config.baseUri("/*/oauth2/code/*"))
                        // successHandler, failureHandler: 로그인 성공 및 실패 처리기 등록
                        .successHandler(authenticationConfig.oAuth2AuthenticationSuccessHandler())
                        .failureHandler(authenticationConfig.oAuth2AuthenticationFailureHandler())
                )
                // 인증되지 않은 사용자가 보호된 리소스에 접근할 경우, CustomAuthenticationEntryPoint에서 예외 처리
                .exceptionHandling(exceptionManager ->
                        exceptionManager.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                )
                .addFilterBefore(authenticationConfig.tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
