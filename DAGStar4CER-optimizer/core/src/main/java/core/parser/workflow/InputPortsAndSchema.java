package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import core.utils.JSONSingleton;

import java.io.Serializable;

public class InputPortsAndSchema implements Serializable {

    private final static long serialVersionUID = 2892947926352262617L;
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
    public InputPortsAndSchema() {
    }

    /**
     * @param schema
     * @param portType
     * @param name
     * @param objectClass
     * @param isConnected
     */
    public InputPortsAndSchema(String name, String objectClass, String portType, boolean isConnected, Schema schema) {
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

    public InputPortsAndSchema withName(String name) {
        this.name = name;
        return this;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public InputPortsAndSchema withObjectClass(String objectClass) {
        this.objectClass = objectClass;
        return this;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public InputPortsAndSchema withPortType(String portType) {
        this.portType = portType;
        return this;
    }

    public boolean isIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public InputPortsAndSchema withIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
        return this;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public InputPortsAndSchema withSchema(Schema schema) {
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
        if ((other instanceof InputPortsAndSchema) == false) {
            return false;
        }
        InputPortsAndSchema rhs = ((InputPortsAndSchema) other);
        return new EqualsBuilder().append(name, rhs.name).append(objectClass, rhs.objectClass).append(isConnected, rhs.isConnected).append(schema, rhs.schema).append(portType, rhs.portType).isEquals();
    }

}
