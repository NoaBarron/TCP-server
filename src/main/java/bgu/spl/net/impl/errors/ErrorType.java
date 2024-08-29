package bgu.spl.net.impl.errors;

public enum ErrorType {
    NOT_DEFINED(0, "Not defined"),
    FILE_NOT_FOUND(1, "File not found"),
    ACCESS_VIOLATION(2, "Access violation"),
    DISK_FULL(3, "Disk full or allocation exceeded"),
    ILLEGAL_OPERATION(4, "Illegal TFTP operation"),
    FILE_EXIST(5, "File already exists"),
    USER_NOT_LOGGED_IN(6, "User not logged in"),
    USER_ALREADY_LOGGED_IN(7, "User already logged in"),;

    private final int value;
    private final String message;

    ErrorType(int value, String message) {
        this.value = value;
        this.message = message;
    }

    public int getValue() {
        return value;
    }   

    public String getMessage() {
        return message;
    }   
}
