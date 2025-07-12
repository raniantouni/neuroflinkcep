package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class OperatorConnection implements Serializable {

    private final static long serialVersionUID = -3898519029865268987L;
    @SerializedName("fromOperator")
    @Expose
    @NotEmpty(message = "fromOperator must not be empty")
    private String fromOperator;

    @SerializedName("fromPort")
    @Expose
    private String fromPort;
    @SerializedName("fromPortType")
    @Expose
    private String fromPortType;
    @SerializedName("toOperator")
    @Expose
    @NotEmpty(message = "toOperator must not be empty")
    private String toOperator;
    @SerializedName("toPort")
    @Expose
    private String toPort;
    @SerializedName("toPortType")
    @Expose
    private String toPortType;

    /**
     * No args constructor for use in serialization
     */
    public OperatorConnection() {
    }

    public OperatorConnection(String fromOperator, String fromPort, String fromPortType, String toOperator, String toPort, String toPortType) {
        super();
        this.fromOperator = fromOperator;
        this.fromPort = fromPort;
        this.fromPortType = fromPortType;
        this.toOperator = toOperator;
        this.toPort = toPort;
        this.toPortType = toPortType;
    }

    public boolean isValid() {
        return fromOperator != null && fromPort != null && fromPortType != null && toOperator != null && toPort != null && toPortType != null;
    }

    public String getFromOperator() {
        return fromOperator;
    }

    public void setFromOperator(String fromOperator) {
        this.fromOperator = fromOperator;
    }

    public OperatorConnection withFromOperator(String fromOperator) {
        this.fromOperator = fromOperator;
        return this;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        this.fromPort = fromPort;
    }

    public OperatorConnection withFromPort(String fromPort) {
        this.fromPort = fromPort;
        return this;
    }

    public String getFromPortType() {
        return fromPortType;
    }

    public void setFromPortType(String fromPortType) {
        this.fromPortType = fromPortType;
    }

    public OperatorConnection withFromPortType(String fromPortType) {
        this.fromPortType = fromPortType;
        return this;
    }

    public String getToOperator() {
        return toOperator;
    }

    public void setToOperator(String toOperator) {
        this.toOperator = toOperator;
    }

    public OperatorConnection withToOperator(String toOperator) {
        this.toOperator = toOperator;
        return this;
    }

    public String getToPort() {
        return toPort;
    }

    public void setToPort(String toPort) {
        this.toPort = toPort;
    }

    public OperatorConnection withToPort(String toPort) {
        this.toPort = toPort;
        return this;
    }

    public String getToPortType() {
        return toPortType;
    }

    public void setToPortType(String toPortType) {
        this.toPortType = toPortType;
    }

    public OperatorConnection withToPortType(String toPortType) {
        this.toPortType = toPortType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperatorConnection that = (OperatorConnection) o;

        if (fromOperator != null ? !fromOperator.equals(that.fromOperator) : that.fromOperator != null) return false;
        if (fromPort != null ? !fromPort.equals(that.fromPort) : that.fromPort != null) return false;
        if (fromPortType != null ? !fromPortType.equals(that.fromPortType) : that.fromPortType != null) return false;
        if (toOperator != null ? !toOperator.equals(that.toOperator) : that.toOperator != null) return false;
        if (toPort != null ? !toPort.equals(that.toPort) : that.toPort != null) return false;
        return toPortType != null ? toPortType.equals(that.toPortType) : that.toPortType == null;
    }

    @Override
    public int hashCode() {
        int result = fromOperator != null ? fromOperator.hashCode() : 0;
        result = 31 * result + (fromPort != null ? fromPort.hashCode() : 0);
        result = 31 * result + (fromPortType != null ? fromPortType.hashCode() : 0);
        result = 31 * result + (toOperator != null ? toOperator.hashCode() : 0);
        result = 31 * result + (toPort != null ? toPort.hashCode() : 0);
        result = 31 * result + (toPortType != null ? toPortType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
