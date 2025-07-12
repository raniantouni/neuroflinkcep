/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import com.rapidminer.extension.streaming.operator.*;
import com.rapidminer.extension.streaming.operator.athena.StreamAthenaOMLOperator;
import com.rapidminer.extension.streaming.operator.athena.StreamSDEOperator;
import com.rapidminer.extension.streaming.operator.demokritos.StreamCEFOperator;
import com.rapidminer.extension.streaming.operator.marinetraffic.StreamMaritimeEventDetectionOperator;
import com.rapidminer.operator.IOMultiplier;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This is a container class holding the dictionary of operators available for optimization by the
 * INFORE optimizer. The method {@link #createFromNetwork(String, Network, List)} provides the functionality to create such a
 * default {@link OperatorDictionary} for the current (version 0.1.0) streaming operators.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class OperatorDictionary {

    private String dictionaryName = null;
    private List<DictOperator> operators = null;

    /**
     * Creates a new {@link OperatorDictionary} instance with the fields set to default values.
     */
    public OperatorDictionary() {
    }

    /**
     * Returns the name of this {@link OperatorDictionary} configuration.
     *
     * @return name of this {@link OperatorDictionary} configuration
     */
    public String getDictionaryName() {
        return dictionaryName;
    }

    /**
     * Sets name of this {@link OperatorDictionary} configuration to the provided one. Returns
     * itself, so that set methods can be chained.
     *
     * @param dictionaryName new name of this {@link OperatorDictionary} configuration
     * @return this {@link OperatorDictionary}
     */
    public OperatorDictionary setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
        return this;
    }

    /**
     * Returns the list of {@link DictOperator}s of this {@link OperatorDictionary} configuration.
     *
     * @return list of {@link DictOperator}s of this {@link OperatorDictionary} configuration
     */
    public List<DictOperator> getOperators() {
        return operators;
    }

    /**
     * Sets list of {@link DictOperator}s of this {@link OperatorDictionary} configuration to the
     * provided one. Returns itself, so that set methods can be chained.
     *
     * @param operators new list of {@link DictOperator}s of this {@link OperatorDictionary} configuration
     * @return this {@link OperatorDictionary}
     */
    public OperatorDictionary setOperators(List<DictOperator> operators) {
        this.operators = operators;
        return this;
    }


    public static OperatorDictionary createFromNetwork(String name, Network network, List<Operator> operators) throws
            OperatorCreationException {
        RandomGenerator random = RandomGenerator.getGlobalRandomGenerator();
        Set<String> allAvailableSites = new LinkedHashSet<>();
        network.getSites().forEach(site -> allAvailableSites.add(site.getSiteName()));
        Set<String> allAvailablePlatforms = new LinkedHashSet<>();
        network.getSites()
                .forEach(site -> site.getAvailablePlatforms()
                        .forEach(plat -> allAvailablePlatforms.add(
                                plat.getPlatformName())));

        Set<String> streamingOperatorsKeys = createAllStreamingOperators()
                .stream()
                .map(op -> op.getOperatorDescription().getKey())
                .collect(Collectors.toSet());

        List<DictOperator> dictOperators = new ArrayList<>();
        for (Operator operator : operators) {
            String classKey = operator.getOperatorDescription().getKey();
            int[] costCoefficients = new int[]{0, 0, 0, 0};
            costCoefficients[random.nextInt(4)] = 1;
            int inputRate = 100;

            DictOperator dictOperator = new DictOperator();
            Map<String, DictSite> sites = new LinkedHashMap<>();
            for (OptimizerSite site : network.getSites()) {
                String siteName = site.getSiteName();
                DictSite currentSite = new DictSite();

                Map<String, DictPlatform> platforms = new LinkedHashMap<>();
                for (OptimizerPlatform platform : site.getAvailablePlatforms()) {
                    String platformName = platform.getPlatformName();
                    DictPlatform currentPlatforms = new DictPlatform();
                    Map<String, Integer> migrationCosts = new LinkedHashMap<>();

                    boolean isSpecialOperator = streamingOperatorsKeys.contains(classKey) && !(operator instanceof StreamOperator);

                    if (streamingOperatorsKeys.contains(classKey) && platformName.equals("rtsa") && !isSpecialOperator)
                        continue;
                    if (!streamingOperatorsKeys.contains(classKey) && !platformName.equals("rtsa") && !isSpecialOperator)
                        continue;

                    allAvailablePlatforms.forEach(
                            plat -> migrationCosts.put(plat, plat.equals(platformName) ? 0 : 500));
                    currentPlatforms.setOperatorName(platformName + "_" + classKey)
                            .setStaticCost(5 * (1 + random.nextInt(5)))
                            .setMigrationCosts(migrationCosts);
                    platforms.put(platformName, currentPlatforms);
                }

                Map<String, Integer> siteMigrationCosts = new LinkedHashMap<>();
                allAvailableSites.forEach(
                        curSite -> siteMigrationCosts.put(curSite, curSite.equals(siteName) ? 0 : 500));

                currentSite.setStaticCost(50 + 10 * random.nextInt(5))
                        .setMigrationCosts(siteMigrationCosts)
                        .setPlatforms(Collections.singletonList(platforms));

                sites.put(siteName, currentSite);
            }

            // Translate the sites map to a list<map> to be translated to JSON array
            dictOperator.setClassKey(classKey)
                    .setCostCoefficients(costCoefficients)
                    .setInputRate(inputRate)
                    .setSites(Collections.singletonList(sites));
            dictOperators.add(dictOperator);
        }

        return new OperatorDictionary().setDictionaryName(name).setOperators(dictOperators);
    }

    /**
     * Returns the list of the current operators available for optimization.
     *
     * <ul>
     *     <li>{@link StreamAggregate}</li>
     *     <li>{@link StreamConnect}</li>
     *     <li>{@link StreamDuplicate}</li>
     *     <li>{@link StreamFilter}</li>
     *     <li>{@link StreamJoin}</li>
     *     <li>{@link StreamKafkaSink}</li>
     *     <li>{@link StreamKafkaSource}</li>
     *     <li>{@link StreamMap}</li>
     *     <li>{@link StreamParseField}</li>
     *     <li>{@link StreamSelect}</li>
     *     <li>{@link StreamStringifyField}</li>
     *     <li>{@link StreamAthenaOMLOperator}</li>
     *     <li>{@link StreamSDEOperator}</li>
     *     <li>{@link StreamCEFOperator}</li>
     *     <li>{@link StreamMaritimeEventDetectionOperator}</li>
     *     <li>{@link IOMultiplier}</li>
     *     <li>{@link RepositorySource}</li>
     * </ul>
     *
     * @return list of the current operators available for optimization
     * @throws OperatorCreationException
     */
    private static List<Operator> createAllStreamingOperators() throws OperatorCreationException {
        List<Operator> result = new ArrayList<>();

        result.add(OperatorService.createOperator(StreamAggregate.class));
        result.add(OperatorService.createOperator(StreamConnect.class));
        result.add(OperatorService.createOperator(StreamDuplicate.class));
        result.add(OperatorService.createOperator(StreamFilter.class));
        result.add(OperatorService.createOperator(StreamJoin.class));
        result.add(OperatorService.createOperator(StreamKafkaSink.class));
        result.add(OperatorService.createOperator(StreamKafkaSource.class));
        result.add(OperatorService.createOperator(StreamMap.class));
        result.add(OperatorService.createOperator(StreamParseField.class));
        result.add(OperatorService.createOperator(StreamSelect.class));
        result.add(OperatorService.createOperator(StreamStringifyField.class));

        result.add(OperatorService.createOperator(StreamingCEPOperator.class));

        result.add(OperatorService.createOperator(StreamAthenaOMLOperator.class));
        result.add(OperatorService.createOperator(StreamSDEOperator.class));
        result.add(OperatorService.createOperator(StreamCEFOperator.class));
        result.add(OperatorService.createOperator(StreamMaritimeEventDetectionOperator.class));

        result.add(OperatorService.createOperator(IOMultiplier.class));
        result.add(OperatorService.createOperator(RepositorySource.class));

        return result;
    }

}
