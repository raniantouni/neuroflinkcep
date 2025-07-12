package core.exception;

public class OptimizerException extends Exception {

    public OptimizerException(Exception e) {
        super(e);
    }

    public OptimizerException(String s) {
        this(new IllegalStateException(s));
    }
}
