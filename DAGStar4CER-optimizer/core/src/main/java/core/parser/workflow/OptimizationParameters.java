package core.parser.workflow;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class OptimizationParameters implements Serializable {
    private final static long serialVersionUID = 1121154292186570367L;

    @SerializedName("continuous")
    @Expose
    @NotNull(message = "Continuous accepts only true/false")
    private boolean continuous;

    @SerializedName("networkName")
    @Expose
    @NotEmpty(message = "Field networkName must not be empty")
    private String networkName;

    @SerializedName("dictionaryName")
    @Expose
    @NotEmpty(message = "Field dictionaryName must not be empty")
    private String dictionaryName;

    @SerializedName("algorithm")
    @Expose
    @NotEmpty(message = "Field algorithm must not be empty")
    private String algorithm;

    @SerializedName("parallelism")
    @Expose
    @Min(1)
    private Long parallelism = 1L;

    @SerializedName("description")
    @Expose
    private String description;

    @SerializedName("cost_model")
    @Expose
    private String cost_model; // e.g [ model1, bo ]

    @SerializedName("timeout_ms")
    @Expose
    @Min(value = 1L, message = "Field timeout_ms must be greater than zero")
    private long timeout_ms = 300000;

    @SerializedName("numOfPlans")
    @Expose
    @Min(value = 1L, message = "Field numOfPlans should be positive.")
    private int numOfPlans = 1;

    //Nullable
    @SerializedName("benchmarkingParameters")
    @Expose
    private BenchmarkingParameters benchmarkingParameters;

    /**
     * No args constructor for use in serialization
     */
    public OptimizationParameters() {
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getDictionaryName() {
        return dictionaryName;
    }

    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Long getParallelism() {
        return parallelism;
    }

    public void setParallelism(Long parallelism) {
        this.parallelism = parallelism;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimeout_ms() {
        return timeout_ms;
    }

    public void setTimeout_ms(long timeout_ms) {
        this.timeout_ms = timeout_ms;
    }

    public String getCost_model() {
        return cost_model;
    }

    public void setCost_model(String cost_model) {
        this.cost_model = cost_model;
    }

    public int getNumOfPlans() {
        return numOfPlans;
    }

    public void setNumOfPlans(int numOfPlans) {
        this.numOfPlans = numOfPlans;
    }

    public BenchmarkingParameters getBenchmarkingParameters() {
        return benchmarkingParameters;
    }

    public void setBenchmarkingParameters(BenchmarkingParameters benchmarkingParameters) {
        this.benchmarkingParameters = benchmarkingParameters;
    }
}
