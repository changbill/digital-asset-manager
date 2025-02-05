package digital.asset.manager.application.global.email.service;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 전송 서비스
 * EmailConfig를 통해 빈 등록을 한 JavaMailSender 객체를 통해 이메일을 보낸다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmailService {
    /**
     * spring-boot-starter-mail 에서 제공하는 이메일 전송 객체
     * application.yml에 설정된 SMTP 서버를 통해 이메일을 보낸다.
     */
    private final JavaMailSender emailSender;

    public void sendEmail(String toEmail, String authCode) {
        String title = "[가상자산 매니저] 안녕하세요. 이메일 인증 코드입니다.";
        String content = "[가상자산 매니저] 서비스에 방문해주셔서 감사합니다." +
                "\n\n" +
                "인증번호는 " + authCode +
                "입니다." +
                "\n" +
                "5분안에 입력해주세요." +
                "\n\n" +
                "감사합니다." +
                "\n\n" +
                "- 가상자산 매니저 서비스팀 -";
        SimpleMailMessage emailForm = createEmailForm(toEmail, title, content);

        try {
            emailSender.send(emailForm);
        } catch (RuntimeException e) {
            log.debug("MailService.sendEmail exception occur toEmail: {}, title: {}, text: {}", toEmail, title, content);
            throw new ApplicationException(ErrorCode.UNABLE_TO_SEND_EMAIL);
        }
    }

    private SimpleMailMessage createEmailForm(String toEmail, String title, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(title);
        message.setText(text);

        return message;
    }
}
