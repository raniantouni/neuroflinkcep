package core.parser.dictionary;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class Akka implements Serializable {

    private final static long serialVersionUID = -215198194417000703L;
    @SerializedName("operatorName")
    @Expose
    @NotEmpty(message = "Akka operatorName name must not me empty.")
    private String operatorName;

    /**
     * No args constructor for use in serialization
     */
    public Akka() {
    }

    /**
     * @param operatorName
     */
    public Akka(String operatorName) {
        super();
        this.operatorName = operatorName;
    }


    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Akka withOperatorName(String operatorName) {
        this.operatorName = operatorName;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
