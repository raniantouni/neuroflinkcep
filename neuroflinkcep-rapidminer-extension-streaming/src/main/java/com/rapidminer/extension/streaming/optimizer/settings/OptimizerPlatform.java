/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

/**
 * This is a container class to hold the information of a single platform (streaming technology) on
 * an {@link OptimizerSite} which are needed by the INFORE optimizer to perform the optimization and
 * decide on the placement of the operators on the different platforms and sites.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class OptimizerPlatform {

    public String platformName = null;

    public String stateStorageLocation = null;

    public String connectionHost = null;

    public String connectionPort = null;

    public String framework = null;
    
    /**
     * Creates a new {@link OptimizerPlatform} instance with the fields set to default values.
     */
    public OptimizerPlatform() {
    }

    /**
     * Returns the name of the {@link OptimizerPlatform}. This is also the streaming technology of
     * the platform.
     *
     * @return name of the {@link OptimizerPlatform}
     */
    public String getPlatformName() {
        return platformName;
    }

    /**
     * Sets the name of the {@link OptimizerPlatform} to the provided one. This is also the
     * streaming technology of the platform. Returns itself, so that set methods can be chained.
     *
     * @param platformName new name of the {@link OptimizerPlatform}
     * @return this {@link OptimizerPlatform}
     */
    public OptimizerPlatform setPlatformName(String platformName) {
        this.platformName = platformName;
        return this;
    }

    /**
     * Returns the location where the states can be stored on the {@link OptimizerPlatform}.
     *
     * @return location where the states can be stored on the {@link OptimizerPlatform}
     */
    public String getStateStorageLocation() {
        return stateStorageLocation;
    }

    /**
     * Sets the location where the states can be stored on the {@link OptimizerPlatform} to the provided one. Returns itself, so that set methods can be chained.
     *
     * @param stateStorageLocation new location where the states can be stored on the {@link OptimizerPlatform}
     * @return this {@link OptimizerPlatform}
     */
    public OptimizerPlatform setStateStorageLocation(String stateStorageLocation) {
        this.stateStorageLocation = stateStorageLocation;
        return this;
    }

    /**
     * Returns the host of the connection to the {@link OptimizerPlatform}.
     *
     * @return host of the connection to the {@link OptimizerPlatform}
     */
    public String getConnectionHost() {
        return connectionHost;
    }

    /**
     * Sets the host of the connection to the {@link OptimizerPlatform} to the provided one. Returns itself, so that set methods can be chained.
     *
     * @param connectionHost new host of the connection to the {@link OptimizerPlatform}
     * @return this {@link OptimizerPlatform}
     */
    public OptimizerPlatform setConnectionHost(String connectionHost) {
        this.connectionHost = connectionHost;
        return this;
    }

    /**
     * Returns the port of the connection to the {@link OptimizerPlatform}.
     *
     * @return port of the connection to the {@link OptimizerPlatform}
     */
    public String getConnectionPort() {
        return connectionPort;
    }

    /**
     * Sets the port of the connection to the {@link OptimizerPlatform} to the provided one. Returns itself, so that set methods can be chained.
     *
     * @param connectionPort new port of the connection to the {@link OptimizerPlatform}
     * @return this {@link OptimizerPlatform}
     */
    public OptimizerPlatform setConnectionPort(String connectionPort) {
        this.connectionPort = connectionPort;
        return this;
    }

    /**
     * Returns the framework (used streaming technology) of the {@link OptimizerPlatform}.
     *
     * @return framework (used streaming technology) of the {@link OptimizerPlatform}
     */
    public String getFramework() {
        return framework;
    }

    /**
     * Sets the framework (used streaming technology) of the {@link OptimizerPlatform} to the provided one. Returns itself, so that set methods can be chained.
     *
     * @param framework new framework (used streaming technology) of the {@link OptimizerPlatform}
     * @return this {@link OptimizerPlatform}
     */
    public OptimizerPlatform setFramework(String framework) {
        this.framework = framework;
        return this;
    }

}
