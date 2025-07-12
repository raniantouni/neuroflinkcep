/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.connection;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import com.rapidminer.extension.streaming.optimizer.settings.OptimizerMessage;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;
import com.rapidminer.extension.streaming.utility.JsonUtil;


/**
 * @author Fabian Temme
 * @since 0.4.0
 */
public class OptimizerStompSessionHandler extends StompSessionHandlerAdapter {
	
	private static Map<String,BlockingQueue<String>> receivedMessages;
	
	public static String BROADCAST_TOPIC = "/topic/broadcast";
	public static String ECHO_TOPIC = "/user/queue/echo";
	public static String INFO_TOPIC = "/user/queue/info";
	public static String ERRORS_TOPIC = "/user/queue/errors";
	public static String OPTIMIZATION_RESULT_TOPIC = "/user/queue/optimization_results";

	private StompSession session;
	private String requestID;
	private OptimizerResponse latestResponse = null;
	private boolean optimizationFinished = false;
	
	public OptimizerStompSessionHandler() {}
	
	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		this.session = session;

		//Subscribe to topics
		List<String> subDestinations = new ArrayList<>();
		subDestinations.add(BROADCAST_TOPIC);
		subDestinations.add(ECHO_TOPIC);
		subDestinations.add(INFO_TOPIC);
		subDestinations.add(ERRORS_TOPIC);
		subDestinations.add(OPTIMIZATION_RESULT_TOPIC);
		
		receivedMessages = new LinkedHashMap<>();
		subDestinations.forEach(sub -> {
			receivedMessages.put(sub, new ArrayBlockingQueue<>(1000));
			synchronized (session) {
				session.subscribe(sub, this);
			}
		});
	}
	
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}
	
	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		String topic = headers.getDestination();
		String msg = (String) payload;
		if (headers.getMessageId() != null){
			boolean acknowledge = true;
			try {
				if (OPTIMIZATION_RESULT_TOPIC.equals(topic)) {
					OptimizerResponse response = JsonUtil.fromString(msg, OptimizerResponse.class);
					if (requestID.equals(response.getOptimizationRequestId())) {
						latestResponse = response;
						notifyAll();
					} else {
						acknowledge = false;
					}
				} else {
					if (INFO_TOPIC.equals(topic)){
						// check for completion message
						OptimizerMessage optimizerMessage = JsonUtil.fromString(msg, OptimizerMessage.class);
						if (OptimizerMessage.OPTIMIZATION_COMPLETED_ACTION.equals(optimizerMessage.getAction()) &&
							requestID.equals(optimizerMessage.getId())) {
							optimizationFinished = true;
						}
					}
					receivedMessages.get(topic).add(msg);
				}
			} catch (IOException e) {
				acknowledge = false;
				e.printStackTrace();
			}
			session.acknowledge(headers.getMessageId(), acknowledge);
		}
	}
	
	public String getMessages(String topic, int pollingTimeMilliSeconds) throws
			InterruptedException {
		if (!receivedMessages.containsKey(topic)){
			return null;
		}
		return receivedMessages.get(topic).poll(pollingTimeMilliSeconds, TimeUnit.MILLISECONDS);
	}
	
	public OptimizerResponse getOptimizationResult() {
		return latestResponse;
	}

	public boolean isOptimizationFinished() {
		return optimizationFinished;
	}

	public synchronized OptimizerResponse waitForOptimizationResult(int pollingTimeMilliSeconds) throws InterruptedException {
		if(latestResponse == null){
			wait(pollingTimeMilliSeconds);
		}
		return latestResponse;
	}

	public boolean newOptimizationResultsAvailable(){
		return latestResponse != null;
	}

	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

}
