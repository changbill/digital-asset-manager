package digital.asset.manager.application.global.annotation;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.global.auth.util.AuthTokenProvider;
import digital.asset.manager.application.global.oauth.util.HeaderUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static digital.asset.manager.application.common.exception.ErrorCode.INVALID_ACCESS_TOKEN;

@RequiredArgsConstructor
public class ExtractPayloadArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthTokenProvider authTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExtractPayload.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String accessToken = HeaderUtils.getAccessToken(request)
                .orElseThrow(() -> new ApplicationException(INVALID_ACCESS_TOKEN));
        authTokenProvider.validToken(accessToken);
        return authTokenProvider.getId(accessToken);
    }
}
