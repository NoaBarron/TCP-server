package bgu.spl.net.impl.errors;


public class ERROR{
    private final ErrorType errorType;

    public ERROR(ErrorType errorType) {
        this.errorType = errorType;
    }

    public ERROR(int errorType) {
        this(ErrorType.values()[errorType]);
    }
    

    public byte[] getError() {
        byte[] errorMsgInBytes = errorType.getMessage().getBytes();
        int length = errorMsgInBytes.length + 5;
        byte[] result = new byte[length];
        result[0] = 0;
        result[1] = 5;
        result[2] = 0;
        result[3] = (byte) errorType.getValue();
        System.arraycopy(errorMsgInBytes, 0, result, 4, errorMsgInBytes.length);
        result[result.length - 1] = 0;
        return result;
    }

}
