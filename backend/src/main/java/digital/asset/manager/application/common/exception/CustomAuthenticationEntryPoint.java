package digital.asset.manager.application.common.exception;

import digital.asset.manager.application.common.response.Response;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Spring Security에서 인증되지 않은 사용자가 보호된 리소스에 접근하려 할 때 처리하는 AuthenticationEntryPoint를 구현
 */
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // commence 메서드는 인증 오류가 발생했을 때 호출됩니다. 이 메서드에서 클라이언트에게 반환할 응답을 설정
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        // 응답 본문을 작성. Response.error() 메서드는 오류 응답을 생성하고, toStream()은 그 응답을 스트림 형식으로 변환하여 클라이언트에게 전송
        response.getWriter().write(Response.error(ErrorCode.INVALID_TOKEN.name()).toStream());
    }
}
