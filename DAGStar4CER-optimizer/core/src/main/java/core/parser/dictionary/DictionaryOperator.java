package core.parser.dictionary;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.validate.PlatformsConstraint;
import core.utils.JSONSingleton;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class DictionaryOperator implements Serializable {
    private final static long serialVersionUID = -2655826387490233223L;

    @SerializedName("classKey")
    @Expose
    @NotEmpty(message = "classKey must not be empty.")
    private String classKey;

    @SerializedName("platforms")
    @Expose
    @PlatformsConstraint
    private @Valid Platforms platforms;

    /**
     * No args constructor for use in serialization
     */
    public DictionaryOperator() {
    }

    /**
     * @param classKey
     * @param platforms
     */
    public DictionaryOperator(String classKey, Platforms platforms) {
        super();
        this.classKey = classKey;
        this.platforms = platforms;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DictionaryOperator that = (DictionaryOperator) o;

        return classKey != null ? classKey.equals(that.classKey) : that.classKey == null;
    }

    @Override
    public int hashCode() {
        return classKey != null ? classKey.hashCode() : 0;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String classKey) {
        this.classKey = classKey;
    }

    public DictionaryOperator withClassKey(String classKey) {
        this.classKey = classKey;
        return this;
    }

    public Platforms getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Platforms platforms) {
        this.platforms = platforms;
    }

    public DictionaryOperator withPlatform(Platforms platforms) {
        this.platforms = platforms;
        return this;
    }
}
