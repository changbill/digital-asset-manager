package digital.asset.manager.application.global.auth.dto;

import digital.asset.manager.application.user.domain.RoleType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 인증 유저 데이터를 가진 객체다.
 * 사용자를 대표하는 객체로 Spring Security에서는 UserDetails나 OAuth2User 인터페이스를 구현한 객체가 Principal 역할을 한다.
 */
public record BoardPrincipal(
        String email,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        Map<String, Object> attributes
) implements UserDetails, OAuth2User, OidcUser {

    public static BoardPrincipal of(String email, String password, Map<String, Object> attributes) {
        return new BoardPrincipal(
                email,
                password,
                Collections.singletonList(new SimpleGrantedAuthority(RoleType.USER.getCode())),
                attributes

        );
    }

    public static BoardPrincipal of(String email, String password, Collection<? extends GrantedAuthority> authorities) {
        return new BoardPrincipal(email, password, authorities, Map.of());
    }

    public static BoardPrincipal of(String email, String password) {
        return of(email, password, Map.of());
    }

    public static BoardPrincipal from(User dto) {
        return of(
                dto.email(),
                dto.password()
        );
    }

    public static BoardPrincipal from(User dto, Map<String, Object> attributes) {
        return of(
                dto.email(),
                dto.password(),
                attributes
        );
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public Map<String, Object> getClaims() {
        return null;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return null;
    }
}


