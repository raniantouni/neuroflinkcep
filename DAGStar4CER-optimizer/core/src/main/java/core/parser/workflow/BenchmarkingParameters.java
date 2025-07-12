package core.parser.workflow;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class BenchmarkingParameters implements Serializable {
    private final static long serialVersionUID = 112111234186570367L;

    @SerializedName("stoppingFunction")
    @Expose
    @NotNull(message = "stoppingFunction must not be null")
    private String stoppingFunction;

    @SerializedName("numberOfIterations")
    @Expose
    @NotNull(message = "numberOfIterations must not be null")
    private int numberOfIterations;

    @SerializedName("jarFilePath")
    @Expose
    @NotNull(message = "jarFilePath must not be null")
    private String jarFilePath;

    public BenchmarkingParameters() {
    }

    public BenchmarkingParameters(String stoppingFunction, int numberOfIterations, String jarFilePath) {
        this.stoppingFunction = stoppingFunction;
        this.numberOfIterations = numberOfIterations;
        this.jarFilePath = jarFilePath;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getStoppingFunction() {
        return stoppingFunction;
    }

    public void setStoppingFunction(String stoppingFunction) {
        this.stoppingFunction = stoppingFunction;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    public String getJarFilePath() {
        return jarFilePath;
    }

    public void setJarFilePath(String jarFilePath) {
        this.jarFilePath = jarFilePath;
    }
}
