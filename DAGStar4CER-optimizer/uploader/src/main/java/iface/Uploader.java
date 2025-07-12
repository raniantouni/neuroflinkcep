package iface;

public interface Uploader {

    /**
     * Upload the JAR file.
     *
     * @param jarPath The path of the JAR file.
     */
    void upload(String jarPath) throws Exception;
}
