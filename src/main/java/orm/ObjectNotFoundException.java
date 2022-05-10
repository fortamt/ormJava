package orm;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public ObjectNotFoundException() {

    }

    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
