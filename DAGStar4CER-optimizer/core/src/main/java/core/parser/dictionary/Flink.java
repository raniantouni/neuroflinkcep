package core.parser.dictionary;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class Flink implements Serializable {

    private final static long serialVersionUID = -9216914003731165144L;
    @SerializedName("operatorName")
    @Expose
    @NotEmpty(message = "Flink operatorName name must not me empty.")
    private String operatorName;

    /**
     * No args constructor for use in serialization
     */
    public Flink() {
    }

    /**
     * @param operatorName
     */
    public Flink(String operatorName) {
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

    public Flink withOperatorName(String operatorName) {
        this.operatorName = operatorName;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
