package digital.asset.manager.application.global.oauth.util;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NicknameUtils {

    public static String createRandomNickname(String email) {
        if (email == null && email.isEmpty()) {
            throw new ApplicationException(ErrorCode.UNSUITABLE_EMAIL);
        }
        LocalDateTime dateTime = LocalDateTime.now();
        String id = email.substring(0, email.indexOf('@'));
        String nick = dateTime.format(DateTimeFormatter.ofPattern("mmssSSS"));
        return id + nick;
    }
}
