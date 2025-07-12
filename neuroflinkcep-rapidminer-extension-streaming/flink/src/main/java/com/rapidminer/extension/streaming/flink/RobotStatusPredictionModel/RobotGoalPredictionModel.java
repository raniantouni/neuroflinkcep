package com.rapidminer.extension.streaming.flink.RobotStatusPredictionModel;

import com.rapidminer.extension.streaming.flink.translate.FlinkCEPTranslator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RobotGoalPredictionModel implements Serializable {


    private static final long serialVersionUID = 1L;
    private String modelFile;
    private transient SavedModelBundle model; // Avoid serialization issues
    private static final Logger LOG = LoggerFactory.getLogger(RobotGoalPredictionModel.class);

    private static final List<String> GOALSTATUSTYPES = goalStatusClasses();
    private static final int BATCH_SIZE = 50;
    private static final int MODEL_FEATURES = 6;
    private static final List<String> FEATURES = initFeatureMap();
    private static final float[] MEANS = {
            4999.95f, 48.8510255f, 49.1862566f,
            4.5413205f, 0.000519043618f, 0.00031580447f
    };
    private static final float[] STDS = {
            2886.75135f, 22.3976279f, 22.5668030f,
            2.90732199f, 1.10898374f, 1.10367765f
    };

    public RobotGoalPredictionModel(String modelFile) {
        this.modelFile = modelFile;
    }

    public void loadModel() throws IOException, URISyntaxException {
        String modelResourcePath = "/model";
        // Get the default temp directory path (e.g., /tmp on UNIX-like systems)
        String defaultTempDir = System.getProperty("java.io.tmpdir");

        String subFolderName = "tf-model";
        Path tempModelDir = Paths.get(defaultTempDir, subFolderName);

        if (Files.exists(tempModelDir)) {
            System.out.println("Using existing temp directory: " + tempModelDir);
        } else {
            Files.createDirectory(tempModelDir);
            extractDirectory(modelResourcePath, tempModelDir);
        }


        this.model = SavedModelBundle.load(tempModelDir.toString(), "serve");
    }

    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    // Returns predicted class for the next step
    public Map<String, String> predict(List<JSONObject> batch) {

        float[][] modelInput = transformBatchToModelInput(batch);

        float[][] scaledInput = scaleInputs(modelInput);
        return runModel(scaledInput);

    }

    private float[][] transformBatchToModelInput(List<JSONObject> batch) {
        float[][] input = new float[BATCH_SIZE][MODEL_FEATURES];

        for (int i = 0; i < batch.size(); i++) {
            JSONObject record = batch.get(i);
            for (int j = 0; j < MODEL_FEATURES; j++)
                input[i][j] = Float.parseFloat(record.getString(FEATURES.get(j)));

        }
        return input;
    }

    private Map<String, String> runModel(float[][] modelInput) {
        if (model == null) {
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }

        Map<String, String> predictions = new HashMap<>();
        // Reshape (50,6) -> (1,50,6)
        float[][][] input3D = new float[1][BATCH_SIZE][MODEL_FEATURES];
        for (int i = 0; i < BATCH_SIZE; i++) {
            for (int j = 0; j < MODEL_FEATURES; j++) {
                input3D[0][i][j] = modelInput[i][j];
            }
        }

        try (Tensor<Float> inputTensor = Tensor.create(input3D, Float.class)) {

            List<Tensor<?>> outputs = model.session().runner()
                    // Feed the input
                    .feed("serving_default_input_1:0", inputTensor)
                    // Fetch each output you want:
                    // Deadlock_Bool_output
                    .fetch("StatefulPartitionedCall:0")
                    // RobotBodyContact_output
                    .fetch("StatefulPartitionedCall:1")
                    // goal_status_output
                    .fetch("StatefulPartitionedCall:2")
                    // idle_output
                    .fetch("StatefulPartitionedCall:3")
                    // linear_output
                    .fetch("StatefulPartitionedCall:4")
                    // rotational_output
                    .fetch("StatefulPartitionedCall:5")

                    .run();


            // Deadlock_Bool_output
            try (Tensor<Float> t = outputs.get(0).expect(Float.class)) {
                float[][] val = new float[1][1];
                t.copyTo(val);
                boolean value = val[0][0] > 0.5;
                predictions.put("deadlock", String.valueOf(value));
            }

            // RobotBodyContact_output
            try (Tensor<Float> t = outputs.get(1).expect(Float.class)) {
                float[][] val = new float[1][1];
                t.copyTo(val);
                boolean value = val[0][0] > 0.5;
                predictions.put("robotBodyContact", String.valueOf(value));
            }

            // goal_status_output (multi-class softmax)
            try (Tensor<Float> t = outputs.get(2).expect(Float.class)) {
                float[][] goalStatusArray = new float[1][22];
                t.copyTo(goalStatusArray);
                int classIndex = getMaxIndex(goalStatusArray[0]);
                System.out.println("PREDICTED EVENT " + GOALSTATUSTYPES.get(classIndex));
                predictions.put("event", GOALSTATUSTYPES.get(classIndex));
            }

            // idle_output
            try (Tensor<Float> t = outputs.get(3).expect(Float.class)) {
                float[][] val = new float[1][1];
                t.copyTo(val);
                boolean value = val[0][0] > 0.5;
                predictions.put("idle", String.valueOf(value));
            }

            // linear_output
            try (Tensor<Float> t = outputs.get(4).expect(Float.class)) {
                float[][] val = new float[1][1];
                t.copyTo(val);
                boolean value = val[0][0] > 0.5;
                predictions.put("linear", String.valueOf(value));
            }

            // rotational_output
            try (Tensor<Float> t = outputs.get(5).expect(Float.class)) {
                float[][] val = new float[1][1];
                t.copyTo(val);
                boolean value = val[0][0] > 0.5;
                predictions.put("rotational", String.valueOf(value));
            }

            return predictions;
//            try (Tensor<Float> goalStatusTensor = outputs.get(2).expect(Float.class)) {
//                // shape should be (1, 22) for a single sequence
//                float[][] goalStatusArray = new float[1][22];
//                goalStatusTensor.copyTo(goalStatusArray);
//                int classIndex = getMaxIndex(goalStatusArray[0]);
//                return GOALSTATUSTYPES.get(classIndex);
//            }
        }

    }

    private int getMaxIndex(float[] arr) {
        int maxIndex = 0;
        float maxValue = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > maxValue) {
                maxValue = arr[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private static List<String> initFeatureMap() {
        List<String> features = new ArrayList<>();
        features.add("current_time_step");
        features.add("px");
        features.add("py");
        features.add("pz");
        features.add("vx");
        features.add("vy");

        return features;

    }

    private static List<String> initOutputFields() {
        List<String> outputFields = new ArrayList<>();
        outputFields.add("deadlock");
        outputFields.add("robotBodyContact");
        outputFields.add("goalStatus");
        outputFields.add("idle");
        outputFields.add("linear");
        outputFields.add("rotational");

        return outputFields;
    }

    private static List<String> goalStatusClasses() {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUV".toCharArray();
        List<String> eventArray;
        Map<Integer, String> goalStatusClassMap = new HashMap<>();
        goalStatusClassMap.put(0, "A");
        for (int i = 1; i < alphabet.length; i++) {
            if (i == 11)
                goalStatusClassMap.put(i, "B");
            else if (i < 11)
                goalStatusClassMap.put(i, alphabet[i + 1] + "");
            else
                goalStatusClassMap.put(i, alphabet[i] + "");

        }
        eventArray = new ArrayList<>(goalStatusClassMap.values());
        goalStatusClassMap.clear();

        return eventArray;
    }


    private static float[][] scaleInputs(float[][] input) {
        float[][] scaled = new float[input.length][input[0].length];
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[i].length; j++) {
                scaled[i][j] = (input[i][j] - MEANS[j]) / STDS[j];
            }
        }
        return scaled;
    }

    private static void extractDirectory(String resourcePath, Path outputPath) throws IOException, URISyntaxException {
        try (InputStream in = RobotGoalPredictionModel.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            URI uri = Objects.requireNonNull(RobotGoalPredictionModel.class.getResource(resourcePath)).toURI();
            if (uri.getScheme().equals("jar")) {
                // Extract from JAR
                try (FileSystem fs = FileSystems.newFileSystem(uri, java.util.Collections.emptyMap())) {
                    Path jarPath = fs.getPath(resourcePath);
                    try (Stream<Path> stream = Files.walk(jarPath)) {
                        stream.forEach(source -> {
                            try {
                                Path destination = outputPath.resolve(jarPath.relativize(source).toString());
                                if (Files.isDirectory(source)) {
                                    Files.createDirectories(destination);
                                } else {
                                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                    }
                }
            } else {
                // If not running from JAR (e.g., running locally)
                Path srcPath = Paths.get(uri);
                try (Stream<Path> stream = Files.walk(srcPath)) {
                    stream.forEach(source -> {
                        try {
                            Path destination = outputPath.resolve(srcPath.relativize(source).toString());
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(destination);
                            } else {
                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }
        }
    }
}
