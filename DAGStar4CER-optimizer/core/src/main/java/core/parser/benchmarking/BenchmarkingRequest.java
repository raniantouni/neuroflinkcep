package core.parser.benchmarking;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class BenchmarkingRequest implements Serializable {

    @SerializedName("stoppingFunction")
    @Expose
    @javax.persistence.Id
    @NotEmpty(message = "Field stoppingFunction must not be empty.")
    private String stoppingFunction;

    @SerializedName("numberOfIterations")
    @Expose
    @Min(value = 1, message = "Field numberOfIterations must be greater than zero")
    private int numberOfIterations;

    @SerializedName("jarFilePath")
    @Expose
    @NotEmpty(message = "Field jarFilePath must not be empty.")
    private String jarFilePath;

    public BenchmarkingRequest() {
    }

    public BenchmarkingRequest(String stoppingFunction, int numberOfIterations, String jarFilePath) {
        this.stoppingFunction = stoppingFunction;
        this.numberOfIterations = numberOfIterations;
        this.jarFilePath = jarFilePath;
    }

    public String getStoppingFunction() {
        return stoppingFunction;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public String getJarFilePath() {
        return jarFilePath;
    }
}
