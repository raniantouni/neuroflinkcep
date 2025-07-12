package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import core.utils.JSONSingleton;

import java.io.Serializable;

public class Parameter implements Serializable {

    private final static long serialVersionUID = -4062288602373407895L;
    @SerializedName("key")
    @Expose
    private String key;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("defaultValue")
    @Expose
    private String defaultValue;
    @SerializedName("range")
    @Expose
    private String range;
    @SerializedName("typeClass")
    @Expose
    private String typeClass;

    /**
     * No args constructor for use in serialization
     */
    public Parameter() {
    }

    /**
     * @param defaultValue
     * @param typeClass
     * @param range
     * @param value
     * @param key
     */
    public Parameter(String key, String value, String defaultValue, String range, String typeClass) {
        super();
        this.key = key;
        this.value = value;
        this.defaultValue = defaultValue;
        this.range = range;
        this.typeClass = typeClass;
    }

    public boolean isValid() {
        return key != null && defaultValue != null && range != null && typeClass != null;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Parameter withKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Parameter withValue(String value) {
        this.value = value;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Parameter withDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public Parameter withRange(String range) {
        this.range = range;
        return this;
    }

    public String getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public Parameter withTypeClass(String typeClass) {
        this.typeClass = typeClass;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(typeClass).append(range).append(value).append(key).append(defaultValue).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Parameter) == false) {
            return false;
        }
        Parameter rhs = ((Parameter) other);
        return new EqualsBuilder().append(typeClass, rhs.typeClass).append(range, rhs.range).append(value, rhs.value).append(key, rhs.key).append(defaultValue, rhs.defaultValue).isEquals();
    }

}
