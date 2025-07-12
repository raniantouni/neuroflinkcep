package spark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SparkCurlUploader {
    static final String jsonPath = "C:\\Users\\user1\\Desktop\\controller\\uploader\\scripts\\data.json";

    public static void main(String[] args) throws IOException {
        String command = "curl -X POST --header \"Content-Type:application/json\" --data @" + jsonPath + " http://master:6066/v1/submissions/create";
        Process process = Runtime.getRuntime().exec(command);
        InputStream response = process.getInputStream();
        System.out.println(convertInputStreamToString(response));
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString(StandardCharsets.UTF_8.name());
    }
}
