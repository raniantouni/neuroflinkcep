package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import core.utils.JSONSingleton;

import java.io.Serializable;
import java.util.List;

public class Schema implements Serializable {

    private final static long serialVersionUID = -1051483081276331724L;
    @SerializedName("fromMetaData")
    @Expose
    private boolean fromMetaData;
    @SerializedName("size")
    @Expose
    private int size;
    @SerializedName("attributes")
    @Expose
    private List<Attribute> attributes = null;

    /**
     * No args constructor for use in serialization
     */
    public Schema() {
    }


    /**
     * @param size
     * @param attributes
     * @param fromMetaData
     */
    public Schema(boolean fromMetaData, int size, List<Attribute> attributes) {
        super();
        this.fromMetaData = fromMetaData;
        this.size = size;
        this.attributes = attributes;
    }

    public boolean isValid() {
        if (attributes == null) {
            return false;
        }
        for (Attribute atr : attributes) {
            if (!atr.isValid()) {
                return false;
            }
        }
        return true;
    }

    public boolean isFromMetaData() {
        return fromMetaData;
    }

    public void setFromMetaData(boolean fromMetaData) {
        this.fromMetaData = fromMetaData;
    }

    public Schema withFromMetaData(boolean fromMetaData) {
        this.fromMetaData = fromMetaData;
        return this;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Schema withSize(int size) {
        this.size = size;
        return this;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Schema withAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(attributes).append(size).append(fromMetaData).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Schema)) {
            return false;
        }
        Schema rhs = ((Schema) other);
        return new EqualsBuilder().append(attributes, rhs.attributes).append(size, rhs.size).append(fromMetaData, rhs.fromMetaData).isEquals();
    }

}
