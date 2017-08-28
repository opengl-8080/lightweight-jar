package lwjar;

public class TooManyCompileErrorException extends RuntimeException {
    public TooManyCompileErrorException(String message) {
        super(message);
    }
}
