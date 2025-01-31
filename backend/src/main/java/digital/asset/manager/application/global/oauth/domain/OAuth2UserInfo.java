package digital.asset.manager.application.global.oauth.domain;

import lombok.ToString;

import java.util.Map;

/**
 * OAuth2 제공자별로 리턴하는 사용자 정보 데이터의 구조와 필드의 이름 등이 다르다.
 * 서비스별로 다른 구조를 통합하기 위한 인터페이스 정의.
 * attributes는 OAuth 2.0 제공자로부터 받은 사용자 정보(Claims)를 저장하는 Map이다.
 */
@ToString
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();

    public abstract String getName();

    public abstract String getEmail();

    public abstract String getImageUrl();
}
