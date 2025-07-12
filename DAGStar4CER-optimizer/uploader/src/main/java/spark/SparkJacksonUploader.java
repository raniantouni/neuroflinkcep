package spark;

import iface.Uploader;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SparkJacksonUploader implements Uploader {
    private static final Logger LOGGER = Logger.getLogger(SparkJacksonUploader.class.getName());
    private String ip;

    public SparkJacksonUploader(String ip) {
        this.ip = ip;
    }

    public void upload(String jarPath) {
        String[] tokens = jarPath.split("\\\\");
        String opNameWithSuffix = tokens[tokens.length - 1];
        String opName = opNameWithSuffix.substring(0, opNameWithSuffix.indexOf("."));

        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "start", "\"\"",
                "spark_rest_submit.bat",
                jarPath.replace("\\", "\\\\"),
                opName);

        //CHANGE THIS TO THE CORRECT LOCAL DIR OR FIND A BETTER SOLUTION
        processBuilder.directory(new File("spark"));

        try {
            Process proc = processBuilder.start();
//            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//            LOGGER.info(br.readLine());
            proc.waitFor();
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public void deploy(String jarPath) {
        SparkProperties props = new SparkProperties()
                .withSparkAppName("test")
                .withSparkDriverMemory("1g")
                .withSparkExecutorMemory("1g")
                .withSparkJars(jarPath)
                .withSparkDriverSupervise("false")
                .withSparkMaster(String.format("spark://%s:6066", this.ip))
                .withSparkSubmitDeployMode("cluster");

        EnvironmentVariables env = new EnvironmentVariables()
                .withSPARKENVLOADED("1");

        SparkRequestData data = new SparkRequestData()
                .withAction("CreateSubmissionRequest")
                .withAppResource(jarPath)
                .withClientSparkVersion("2.4.4")
//                .withAppArgs(Arrays.asList("--key1=val1", "--key2=val2"))
                .withEnvironmentVariables(env)
                .withMainClass("A")
                .withSparkProperties(props);


        try {
            HttpResponse<JsonNode> response = Unirest.post(String.format("http://%s:6066/v1/submissions/create", this.ip))
                    .header("accept", "application/json")
                    .body(data)
                    .asJson();

            LOGGER.log(Level.INFO, response.getBody().toPrettyString());
        } catch (UnirestException e) {
            LOGGER.severe(e.getMessage());
        }

        LOGGER.log(Level.INFO, "Completed");
    }
}
