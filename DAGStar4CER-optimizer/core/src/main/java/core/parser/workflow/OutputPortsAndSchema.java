package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import core.utils.JSONSingleton;

import java.io.Serializable;

public class OutputPortsAndSchema implements Serializable {

    private final static long serialVersionUID = -52670698823440437L;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("objectClass")
    @Expose
    private String objectClass;
    @SerializedName("portType")
    @Expose
    private String portType;
    @SerializedName("isConnected")
    @Expose
    private boolean isConnected;
    @SerializedName("schema")
    @Expose
    private Schema schema;

    /**
     * No args constructor for use in serialization
     */
    public OutputPortsAndSchema() {
    }

    /**
     * @param schema
     * @param portType
     * @param name
     * @param objectClass
     * @param isConnected
     */
    public OutputPortsAndSchema(String name, String objectClass, String portType, boolean isConnected, Schema schema) {
        super();
        this.name = name;
        this.objectClass = objectClass;
        this.portType = portType;
        this.isConnected = isConnected;
        this.schema = schema;
    }

    public boolean isValid() {
        if (name == null || portType == null) {
            return false;
        }
        if (schema != null) {
            return schema.isValid();
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OutputPortsAndSchema withName(String name) {
        this.name = name;
        return this;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public OutputPortsAndSchema withObjectClass(String objectClass) {
        this.objectClass = objectClass;
        return this;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public OutputPortsAndSchema withPortType(String portType) {
        this.portType = portType;
        return this;
    }

    public boolean isIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public OutputPortsAndSchema withIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
        return this;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public OutputPortsAndSchema withSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(objectClass).append(isConnected).append(schema).append(portType).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof OutputPortsAndSchema) == false) {
            return false;
        }
        OutputPortsAndSchema rhs = ((OutputPortsAndSchema) other);
        return new EqualsBuilder().append(name, rhs.name).append(objectClass, rhs.objectClass).append(isConnected, rhs.isConnected).append(schema, rhs.schema).append(portType, rhs.portType).isEquals();
    }

}
