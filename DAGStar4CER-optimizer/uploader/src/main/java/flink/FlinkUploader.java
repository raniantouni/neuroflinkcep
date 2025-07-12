package flink;

import iface.Uploader;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;


/**
 * 2 step Flink REST API JAR submission. Upload the JAR and then run it.
 */
public class FlinkUploader implements Uploader {
    private static final Logger LOGGER = LogManager.getLogger(FlinkUploader.class.getName());

    private String targetIP;

    public FlinkUploader(String ip) {
        this.targetIP = ip;
    }

    public void upload(String uploadPath) {
        //Upload and then schedule the flink jar for execution
        HttpResponse<JsonNode> uploadResponse;
        try {
            String uploadTarget = String.format("http://%s:%s/jars/upload", this.targetIP, 8081);
            LOGGER.info("Flink target: " + uploadTarget);
            uploadResponse = Unirest.post(uploadTarget)
                    .header("accept", "application/x-java-archive")
                    .field("jarfile", new File(uploadPath))
                    .asJson();
        } catch (Exception e) {
            LOGGER.error("Flink upload exception: " + e.getMessage());
            return;
        }

        //Log the upload response from Flink RM.
        LOGGER.log(Level.INFO, "Flink JAR uploaded with body: " + uploadResponse.getBody().toString());

        //Execute the uploaded JAR
        //Since we are parsing flink output we need to consider the fact that flink master may be running in
        //windows which would result to a response with double '/' .
        try {
            String filename = uploadResponse.getBody().getObject().get("filename").toString();
            String[] jar_path_tokens = filename.split("/");
            String jar_id = jar_path_tokens[jar_path_tokens.length - 1];
            LOGGER.info("jar_id: " + jar_id);

            String execute_target = String.format("http://%s:%s/jars/%s/run", this.targetIP, 8081, jar_id);

            //JAR options
            JSONObject flinkReqBody = new JSONObject();
            flinkReqBody.put("parallelism", 2);

            //Submit the JAR
            HttpResponse<JsonNode> submitResp = Unirest.post(execute_target)
                    .header("Content-Type", "application/json")
                    .body(flinkReqBody)
                    .asJson();

            LOGGER.info("Flink JAR successful submission with body: " + submitResp.getBody().toString());
        } catch (Exception e) {
            LOGGER.error("Flink submission exception: " + e.getMessage());
        }
    }
}
