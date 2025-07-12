package core.parser.network;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

public class Site implements Serializable {

    private final static long serialVersionUID = 6710701787922380302L;

    @SerializedName("siteName")
    @Expose
    @NotEmpty(message = "Site name is mandatory.")
    private String siteName;

    @SerializedName("availablePlatforms")
    @Expose
    @NotEmpty(message = "availablePlatforms list must not be empty")
    private List<@Valid AvailablePlatform> availablePlatforms = null;

    /**
     * No args constructor for use in serialization
     */
    public Site() {
    }

    public Site(String siteName, List<AvailablePlatform> availablePlatforms) {
        super();
        this.siteName = siteName;
        this.availablePlatforms = availablePlatforms;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Site withSiteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public List<AvailablePlatform> getAvailablePlatforms() {
        return availablePlatforms;
    }

    public void setAvailablePlatforms(List<AvailablePlatform> availablePlatforms) {
        this.availablePlatforms = availablePlatforms;
    }

    public Site withAvailablePlatforms(List<AvailablePlatform> availablePlatforms) {
        this.availablePlatforms = availablePlatforms;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Site site = (Site) o;

        return siteName != null ? siteName.equals(site.siteName) : site.siteName == null;
    }

    @Override
    public int hashCode() {
        return siteName != null ? siteName.hashCode() : 0;
    }
}
