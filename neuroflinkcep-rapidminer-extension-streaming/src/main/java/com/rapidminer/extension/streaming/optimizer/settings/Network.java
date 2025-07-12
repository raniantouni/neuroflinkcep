/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.*;
import java.util.Map.Entry;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.extension.streaming.connection.StreamConnectionHandler;
import com.rapidminer.extension.streaming.connection.StreamingConnectionHelper;
import com.rapidminer.extension.streaming.deploy.StreamRunnerConstants;


/**
 * This is a container class holding the information about available execution sites (and platforms
 * on the sites) for the INFORE optimizer. The method {@link #create(String, Map)} provides the
 * functionality to create such a{@link Network} from a collection of {@link
 * ConnectionInformationContainerIOObject}s.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class Network {

    private String network = null;

    private List<OptimizerSite> sites = null;

    /**
     * Creates a new {@link Network} instance with the fields set to default values.
     */
    public Network() {
    }

    /**
     * Returns the name of this {@link Network} configuration.
     *
     * @return name of this {@link Network} configuration
     */
    public String getNetwork() {
        return network;
    }

    /**
     * Sets name of this {@link Network} configuration to the provided one. Returns itself, so that
     * set methods can be chained.
     *
     * @param network new name of this {@link Network} configuration
     * @return this {@link Network}
     */
    public Network setNetwork(String network) {
        this.network = network;
        return this;
    }

    /**
     * Returns the list of available {@link OptimizerSite}s in this {@link Network}.
     *
     * @return list of available {@link OptimizerSite}s in this {@link Network}
     */
    public List<OptimizerSite> getSites() {
        return sites;
    }

    /**
     * Sets list of available {@link OptimizerSite}s in this {@link Network} to the provided one.
     * Returns itself, so that set methods can be chained.
     *
     * @param sites new list of available {@link OptimizerSite}s in this {@link Network}
     * @return this {@link DictPlatform}
     */
    public Network setSites(List<OptimizerSite> sites) {
        this.sites = sites;
        return this;
    }

    /**
     * Creates a {@link Network} configuration for the provided collection of {@link
     * ConnectionInformationContainerIOObject}s.
     * <p>
     * The method loops through the connection objects and creates for each connection an {@link
     * OptimizerSite} (with the name of the connection as the site name) with one available(with the type of the connection as platform name). This means that even
     * for connection objects which describes connections to the same physical computing site, there
     * are placed for now into different {@link OptimizerSite}s.
     * <p>
     * The  has hardcoded default values for driver memory, executors,
     * executor cores and executor memory. The topic key is set to: "&lt;siteName&gt;_&lt;
     * platformName&gt;1".
     * <p>
     * The name of the {@link Network} configuration is set to "network1".
     *
     * @param availableSites collection of {@link ConnectionInformationContainerIOObject}s for which the {@link Network}
     *                       configuration is created
     * @return {@link Network} configuration for the provided collection of {@link
     * ConnectionInformationContainerIOObject}s
     */
    public static Network create(String name,
                                 Map<String, List<ConnectionInformationContainerIOObject>> availableSites) {

        List<OptimizerSite> sites = new ArrayList<>();

        for (Entry<String, List<ConnectionInformationContainerIOObject>> entry : availableSites.entrySet()) {
            String siteName = entry.getKey();
            List<OptimizerPlatform> platforms = new ArrayList<>();
            for (ConnectionInformationContainerIOObject connection : entry.getValue()) {
                // Connection and appropriate handler (based on the connection)
                ConnectionConfiguration connConfig = connection.getConnectionInformation()
                        .getConfiguration();
                String connType = connConfig.getType();
                StreamConnectionHandler handler = StreamingConnectionHelper.CONNECTION_HANDLER_MAP.get(connType);
                Properties connectionProperties = handler.buildClusterConfiguration(connConfig);
                String platformName = connType.replace("streaming:", "");

                platforms.add(new OptimizerPlatform().setPlatformName(platformName)
                        .setConnectionHost(connectionProperties.getProperty(StreamRunnerConstants.RM_CONF_CLUSTER_HOST))
                        .setConnectionPort(connectionProperties.getProperty(StreamRunnerConstants.RM_CONF_CLUSTER_PORT))
                        .setStateStorageLocation(connectionProperties.getProperty(StreamRunnerConstants.RM_CONF_CLUSTER_STATE_STORAGE_LOCATION))
                        .setFramework(platformName));
            }
            sites.add(new OptimizerSite().setSiteName(siteName).setAvailablePlatforms(platforms));
        }

        return new Network().setNetwork(name).setSites(sites);
    }

}
