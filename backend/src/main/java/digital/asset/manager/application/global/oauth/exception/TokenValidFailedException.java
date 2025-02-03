package digital.asset.manager.application.global.oauth.exception;

public class TokenValidFailedException extends RuntimeException {
    public TokenValidFailedException() {
        super("Failed to validate token");
    }
}
