package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import core.utils.JSONSingleton;

import java.io.Serializable;

public class Attribute implements Serializable {

    private final static long serialVersionUID = -7121217526726487625L;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("specialRole")
    @Expose
    private String specialRole;

    /**
     * No args constructor for use in serialization
     */
    public Attribute() {
    }

    /**
     * @param specialRole
     * @param name
     * @param type
     */
    public Attribute(String name, String type, String specialRole) {
        super();
        this.name = name;
        this.type = type;
        this.specialRole = specialRole;
    }

    public boolean isValid() {
        return name != null && type != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Attribute withName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Attribute withType(String type) {
        this.type = type;
        return this;
    }

    public String getSpecialRole() {
        return specialRole;
    }

    public void setSpecialRole(String specialRole) {
        this.specialRole = specialRole;
    }

    public Attribute withSpecialRole(String specialRole) {
        this.specialRole = specialRole;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(specialRole).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Attribute) == false) {
            return false;
        }
        Attribute rhs = ((Attribute) other);
        return new EqualsBuilder().append(name, rhs.name).append(specialRole, rhs.specialRole).append(type, rhs.type).isEquals();
    }

}
