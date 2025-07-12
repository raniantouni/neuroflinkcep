package core.parser.network;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class AvailablePlatform implements Serializable {

    private final static long serialVersionUID = -8767187830920671531L;
    @SerializedName("platformName")
    @Expose
    @NotEmpty(message = "Platform name should be present.")
    private String platformName;

    @SerializedName("driverMemoryMB")
    @Expose
    private int driverMemoryMB;
    @SerializedName("executors")
    @Expose
    private int executors;
    @SerializedName("executorCores")
    @Expose
    private int executorCores;
    @SerializedName("executorMemoryMB")
    @Expose
    private int executorMemoryMB;
    @SerializedName("topicKey")
    @Expose
    private String topicKey;
    @SerializedName("jobmanagerMemoryMB")
    @Expose
    private int jobmanagerMemoryMB;
    @SerializedName("taskManagers")
    @Expose
    private int taskManagers;
    @SerializedName("taskSlots")
    @Expose
    private int taskSlots;
    @SerializedName("taskManagerMemoryMB")
    @Expose
    private int taskManagerMemoryMB;
    @SerializedName("memoryMB")
    @Expose
    private int memoryMB;
    @SerializedName("cores")
    @Expose
    private int cores;

    /**
     * No args constructor for use in serialization
     */
    public AvailablePlatform() {
    }

    public AvailablePlatform(String name) {
        this.platformName = name;
    }

    /**
     * @param topicKey
     * @param taskManagerMemoryMB
     * @param memoryMB
     * @param cores
     * @param executorCores
     * @param jobmanagerMemoryMB
     * @param driverMemoryMB
     * @param executors
     * @param taskManagers
     * @param taskSlots
     * @param platformName
     * @param executorMemoryMB
     */
    public AvailablePlatform(String platformName, int driverMemoryMB, int executors, int executorCores, int executorMemoryMB, String topicKey, int jobmanagerMemoryMB, int taskManagers, int taskSlots, int taskManagerMemoryMB, int memoryMB, int cores) {
        super();
        this.platformName = platformName;
        this.driverMemoryMB = driverMemoryMB;
        this.executors = executors;
        this.executorCores = executorCores;
        this.executorMemoryMB = executorMemoryMB;
        this.topicKey = topicKey;
        this.jobmanagerMemoryMB = jobmanagerMemoryMB;
        this.taskManagers = taskManagers;
        this.taskSlots = taskSlots;
        this.taskManagerMemoryMB = taskManagerMemoryMB;
        this.memoryMB = memoryMB;
        this.cores = cores;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public AvailablePlatform withPlatformName(String platformName) {
        this.platformName = platformName;
        return this;
    }

    public int getDriverMemoryMB() {
        return driverMemoryMB;
    }

    public void setDriverMemoryMB(int driverMemoryMB) {
        this.driverMemoryMB = driverMemoryMB;
    }

    public AvailablePlatform withDriverMemoryMB(int driverMemoryMB) {
        this.driverMemoryMB = driverMemoryMB;
        return this;
    }

    public int getExecutors() {
        return executors;
    }

    public void setExecutors(int executors) {
        this.executors = executors;
    }

    public AvailablePlatform withExecutors(int executors) {
        this.executors = executors;
        return this;
    }

    public int getExecutorCores() {
        return executorCores;
    }

    public void setExecutorCores(int executorCores) {
        this.executorCores = executorCores;
    }

    public AvailablePlatform withExecutorCores(int executorCores) {
        this.executorCores = executorCores;
        return this;
    }

    public int getExecutorMemoryMB() {
        return executorMemoryMB;
    }

    public void setExecutorMemoryMB(int executorMemoryMB) {
        this.executorMemoryMB = executorMemoryMB;
    }

    public AvailablePlatform withExecutorMemoryMB(int executorMemoryMB) {
        this.executorMemoryMB = executorMemoryMB;
        return this;
    }

    public String getTopicKey() {
        return topicKey;
    }

    public void setTopicKey(String topicKey) {
        this.topicKey = topicKey;
    }

    public AvailablePlatform withTopicKey(String topicKey) {
        this.topicKey = topicKey;
        return this;
    }

    public int getJobmanagerMemoryMB() {
        return jobmanagerMemoryMB;
    }

    public void setJobmanagerMemoryMB(int jobmanagerMemoryMB) {
        this.jobmanagerMemoryMB = jobmanagerMemoryMB;
    }

    public AvailablePlatform withJobmanagerMemoryMB(int jobmanagerMemoryMB) {
        this.jobmanagerMemoryMB = jobmanagerMemoryMB;
        return this;
    }

    public int getTaskManagers() {
        return taskManagers;
    }

    public void setTaskManagers(int taskManagers) {
        this.taskManagers = taskManagers;
    }

    public AvailablePlatform withTaskManagers(int taskManagers) {
        this.taskManagers = taskManagers;
        return this;
    }

    public int getTaskSlots() {
        return taskSlots;
    }

    public void setTaskSlots(int taskSlots) {
        this.taskSlots = taskSlots;
    }

    public AvailablePlatform withTaskSlots(int taskSlots) {
        this.taskSlots = taskSlots;
        return this;
    }

    public int getTaskManagerMemoryMB() {
        return taskManagerMemoryMB;
    }

    public void setTaskManagerMemoryMB(int taskManagerMemoryMB) {
        this.taskManagerMemoryMB = taskManagerMemoryMB;
    }

    public AvailablePlatform withTaskManagerMemoryMB(int taskManagerMemoryMB) {
        this.taskManagerMemoryMB = taskManagerMemoryMB;
        return this;
    }

    public int getMemoryMB() {
        return memoryMB;
    }

    public void setMemoryMB(int memoryMB) {
        this.memoryMB = memoryMB;
    }

    public AvailablePlatform withMemoryMB(int memoryMB) {
        this.memoryMB = memoryMB;
        return this;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public AvailablePlatform withCores(int cores) {
        this.cores = cores;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AvailablePlatform platform = (AvailablePlatform) o;

        return platformName != null ? platformName.equals(platform.platformName) : platform.platformName == null;
    }

    @Override
    public int hashCode() {
        return platformName != null ? platformName.hashCode() : 0;
    }
}
