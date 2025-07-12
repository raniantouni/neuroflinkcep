package core.parser.workflow;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.parser.network.AvailablePlatform;
import core.parser.network.Site;
import core.utils.JSONSingleton;

import javax.persistence.ElementCollection;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacementSite implements Serializable {

    private final static long serialVersionUID = 5721156392186570393L;
    @SerializedName("siteName")
    @Expose
    @NotEmpty(message = "siteName must not be empty")
    private String siteName;

    @SerializedName("availablePlatforms")
    @Expose
    @ElementCollection
    @NotNull(message = "availablePlatforms must be present (can be empty)")
    private List<@Valid PlacementPlatform> availablePlatforms = null;

    /**
     * No args constructor for use in serialization
     */
    public PlacementSite() {
    }

    public PlacementSite(String siteName, List<PlacementPlatform> availablePlatforms) {
        super();
        this.siteName = siteName;
        this.availablePlatforms = availablePlatforms;
    }

    public PlacementSite(Site site) {
        super();
        this.siteName = site.getSiteName();
        this.availablePlatforms = new ArrayList<>();
        for (AvailablePlatform platform : site.getAvailablePlatforms()) {
            PlacementPlatform placementPlatform = new PlacementPlatform();
            placementPlatform.setAddress("");
            placementPlatform.setOperators(new ArrayList<>());
            placementPlatform.setPlatformName(platform.getPlatformName());
            this.availablePlatforms.add(placementPlatform);
        }
    }

    public boolean isValid() {
        if (siteName == null || availablePlatforms == null) {
            return false;
        }
        for (PlacementPlatform p : availablePlatforms) {
            if (!p.isValid()) {
                return false;
            }
        }
        return true;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public PlacementSite withSiteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public List<PlacementPlatform> getAvailablePlatforms() {
        return availablePlatforms;
    }

    public void setAvailablePlatforms(List<PlacementPlatform> availablePlatforms) {
        this.availablePlatforms = availablePlatforms;
    }

    public PlacementSite withAvailablePlatforms(List<PlacementPlatform> availablePlatforms) {
        this.availablePlatforms = availablePlatforms;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }

}
