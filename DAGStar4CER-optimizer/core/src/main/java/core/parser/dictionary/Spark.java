package core.parser.dictionary;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class Spark implements Serializable {

    private final static long serialVersionUID = -6717623173201126146L;
    @SerializedName("operatorName")
    @Expose
    @NotEmpty(message = "Spark operatorName name must not me empty.")
    private String operatorName;

    /**
     * No args constructor for use in serialization
     */
    public Spark() {
    }

    /**
     * @param operatorName
     */
    public Spark(String operatorName) {
        super();
        this.operatorName = operatorName;
    }

    public boolean isValid() {
        return operatorName != null;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Spark withOperatorName(String operatorName) {
        this.operatorName = operatorName;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
