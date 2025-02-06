package digital.asset.manager.application.global.oauth.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProviderType {
    LOCAL("local"),
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String registrationId;

    /**
     * @RequestBody가 붙은 DTO로 요청 Json을 Spring MVC가 변환 시 @JsonCreator가 있는 from()메서드를 호출하여 Enum 값 변환
     * from("google") → ProviderType.GOOGLE로 변환
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderType from(@JsonProperty("provider") String provider) {
        for (ProviderType type : ProviderType.values()) {
            if (type.name().equalsIgnoreCase(provider)) {
                return type;
            }
        }
        throw new ApplicationException(ErrorCode.INVALID_PROVIDER_TYPE);
    }
}
