package com.rapidminer.extension.streaming.flink.MobileFraudDetectionModel;

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
import java.util.stream.Stream;

public class MobileFraudDetectionModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private String modelFile;
    private transient SavedModelBundle model;
    private static final Logger LOG = LoggerFactory.getLogger(MobileFraudDetectionModel.class);

    private static final int MODEL_FEATURES = 7;
    private static final float[] MEANS = {
            12.83453363f, 2.18066755e+03f, 4.17294489e-02f
    };
    private static final float[] STDS = {
            5.13712116f, 2.59130454e+04f, 1.57571679e+00f
    };
    private static final List<String> NUMERIC_KEYS = Arrays.asList(
            "call_start_time", "conversation_duration", "total_call_charge_amount"
    );
    private static final List<String> ONE_HOT_COLUMNS = Arrays.asList(
            "other_party_tel_number_prefix=0",
            "other_party_tel_number_prefix=385",
            "other_party_tel_number_prefix=960",
            "other_party_tel_number_prefix=null"
    );

    public MobileFraudDetectionModel(String modelFile) throws IOException, URISyntaxException {
        this.modelFile = modelFile;
    }

    public SavedModelBundle getModel() {
        return model;
    }

    public void loadModel() throws IOException, URISyntaxException {
        String modelResourcePath = "/mobileFraudDetectionModel"; // packaged inside JAR or resource dir
        String defaultTempDir = System.getProperty("java.io.tmpdir");
        Path tempModelDir = Paths.get(defaultTempDir, "tf-mobile-fraud-model");

        if (Files.exists(tempModelDir)) {
            System.out.println("Using existing temp directory: " + tempModelDir);
        } else {
            Files.createDirectory(tempModelDir);
            extractDirectory(modelResourcePath, tempModelDir);
        }

        this.model = SavedModelBundle.load(tempModelDir.toString(), "serve");
        if (this.model == null) {
            System.out.println("Model could not be loaded");
        }
    }

    public Map<String, String> predict(JSONObject record) {
        float[] features = new float[MODEL_FEATURES];
        int index = 0;

        // Standardize numeric
        for (int i = 0; i < NUMERIC_KEYS.size(); i++) {
            float rawVal = Float.parseFloat(record.optString(NUMERIC_KEYS.get(i), "0"));
            System.out.println(rawVal);
            features[index++] = (rawVal - MEANS[i]) / STDS[i];
        }

        // One-hot encode categoricals
        for (String oneHot : ONE_HOT_COLUMNS) {
            String[] parts = oneHot.split("=");
            String key = parts[0];
            String expectedValue = parts[1];
            String actualValue = record.optString(key, "");
            features[index++] = expectedValue.equals(actualValue) ? 1.0f : 0.0f;
            System.out.println(features[index - 1]);
        }
        float[][] modelInput = new float[1][MODEL_FEATURES];
        modelInput[0] = features;
        try (Tensor<Float> inputTensor = Tensor.create(modelInput, Float.class)) {
            List<Tensor<?>> outputs = this.model.session().runner()
                    .feed("serving_default_dense_input:0", inputTensor)
                    .fetch("StatefulPartitionedCall:0")
                    .run();

            float[][] preds = new float[1][4];
            outputs.get(0).copyTo(preds);
            System.out.println("OUTPUTS: " + Arrays.deepToString(preds));
            Map<String, String> result = new LinkedHashMap<>();
            String[] labels = {"A", "B", "C", "D"};
            for (int i = 0; i < labels.length; i++) {
                result.put(labels[i], preds[0][i] > 0.5 ? "1" : "0");
            }
            System.out.println("Result for the prediction: " + result);
            return result;
        }
    }
    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    private static void extractDirectory(String resourcePath, Path outputPath) throws IOException, URISyntaxException {
        try (InputStream in = MobileFraudDetectionModel.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            URI uri = Objects.requireNonNull(MobileFraudDetectionModel.class.getResource(resourcePath)).toURI();
            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
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
