package digital.asset.manager.application.global.exception;

public class TokenValidFailedException extends RuntimeException {
    public TokenValidFailedException() {
        super("Failed to validate token");
    }
}
