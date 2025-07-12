package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.persistence.ElementCollection;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

public class PlacementPlatform implements Serializable {

    private final static long serialVersionUID = -6656470345433150080L;
    @SerializedName("platformName")
    @Expose
    @NotEmpty(message = "platformName must not be empty")
    private String platformName;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("operators")
    @Expose
    @NotNull(message = "operators must be present (can be empty)")
    @ElementCollection //Added by Xenia
    //private List<String> operators = null;
    private List<@Valid Operator> operators = null; //Above line adapted by Xenia

    /**
     * No args constructor for use in serialization
     */
    public PlacementPlatform() {
    }

    /**
     * @param address
     * @param operators
     * @param platformName
     */
//    public PlacementPlatform(String platformName, String address, List<String> operators) {
//        super();
//        this.platformName = platformName;
//        this.address = address;
//        this.operators = operators;
//    }
    //Added by Xenia
    public PlacementPlatform(String platformName, String address, List<Operator> operators) {
        super();
        this.platformName = platformName;
        this.address = address;
        this.operators = operators;
    }

//    public boolean isValid() {
//        if (platformName == null || address == null || operators == null) {
//            return false;
//        }
//        for (String s : operators) {
//            if (s == null) {
//                return false;
//            }
//        }
//        return true;
//    }

    //Added by Xenia
    public boolean isValid() {
        if (platformName == null || address == null || operators == null) {
            return false;
        }
        for (Operator s : operators) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }


    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public PlacementPlatform withPlatformName(String platformName) {
        this.platformName = platformName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PlacementPlatform withAddress(String address) {
        this.address = address;
        return this;
    }

//    public List<String> getOperators() {
//        return operators;
//    }
    //Added by Xenia
    public List<Operator> getOperators() {
        return operators;
    }

//    public void setOperators(List<String> operators) {
//        this.operators = operators;
//    }
//Added by Xenia
    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

//    public PlacementPlatform withOperators(List<String> operators) {
//        this.operators = operators;
//        return this;
//    }
   //Added by Xenia
    public PlacementPlatform withOperators(List<Operator> operators) {
        this.operators = operators;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}