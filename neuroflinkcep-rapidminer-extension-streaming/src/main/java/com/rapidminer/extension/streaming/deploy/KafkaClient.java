/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy;

import static java.lang.String.format;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.google.common.collect.Sets;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;


/**
 * Utility for communicating with a Kafka cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class KafkaClient {

	private static final Logger LOGGER = LogService.getRoot();

	private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MINUTES;

	private static final int TIMEOUT = 1;

	private final Properties clusterConfig;

	/**
	 * Constructs client with the cluster configuration provided as argument
	 *
	 * @param clusterConfig
	 */
	public KafkaClient(Properties clusterConfig) {
		this.clusterConfig = clusterConfig;
	}

	/**
	 * Sends the key-value record to the topic
	 *
	 * @param topic
	 * @param key
	 * @param value
	 * @throws OperatorException if something goes wrong
	 */
	public void send(String topic, String key, String value) throws OperatorException {
		String logDetails = format("(topic: %s, key: %s, value: %s)", topic, key, value);
		LOGGER.fine("Sending: " + logDetails);

		try (Producer<String, String> requestSender = buildProducer(clusterConfig)) {
			ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
			// Sending it
			requestSender
				.send(record)
				.get(TIMEOUT, TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			throw new OperatorException("Interrupted, while sending record: " + logDetails, e);
		} catch (ExecutionException e) {
			throw new OperatorException("Could not send record: " + logDetails, e);
		} catch (TimeoutException e) {
			throw new OperatorException("Sending request timed out: " + logDetails, e);
		}
	}

	/**
	 * Checks if the topics, that were given as parameter, exist.
	 *
	 * @param topics topic names that need to be checked if they already exist
	 * @return set of topics that already exist
	 */
	public Set<String> checkTopics(Set<String> topics) throws OperatorException {
		try (AdminClient client = AdminClient.create(clusterConfig)) {
			ListTopicsOptions options = new ListTopicsOptions();
			options.listInternal(true);

			// Get existing topic list into a set
			Set<String> currentTopics = client
				.listTopics(options)
				.names()
				.get(TIMEOUT, TIMEOUT_UNIT);

			// Intersection: topics that are already in the cluster and we were looking for them
			return Sets.intersection(topics, currentTopics);
		} catch (TimeoutException e) {
			throw new OperatorException("Topic list request timed out", e);
		} catch (Exception e) {
			throw new OperatorException("Could not read existing topic list", e);
		}
	}

	/**
	 * Creates topics using the administration tools provided by the Kafka library
	 *
	 * @param topics collection of topic name-partition-replication tuples
	 */
	public void createTopics(Collection<NewTopic> topics) throws OperatorException {
		try (AdminClient client = AdminClient.create(clusterConfig)) {
			// Do create topics
			client
				.createTopics(topics)
				.all()
				.get(TIMEOUT, TIMEOUT_UNIT);
		} catch (TimeoutException e) {
			throw new OperatorException("Topic list creation timed out", e);
		} catch (Exception e) {
			throw new OperatorException("Could not create topic list", e);
		}
	}

	/**
	 * Using the configuration (key-value store) this method creates a KafkaProducer
	 *
	 * @param clusterConfig
	 * @return configured Producer instance
	 */
	private Producer<String, String> buildProducer(Properties clusterConfig) {
		Properties props = new Properties();
		props.put(BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getProperty(BOOTSTRAP_SERVERS_CONFIG));
		props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return new KafkaProducer<>(props);
	}

}