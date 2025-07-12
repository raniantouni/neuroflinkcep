/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.connection;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.rapidminer.extension.streaming.deploy.management.JobSummary;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerMessage;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;


/**
 * @author Fabian Temme
 * @since 0.1.0
 */
public class OptimizerConnection {



	private StompSession stompSession;
	private final OptimizerStompSessionHandler sessionHandler = new OptimizerStompSessionHandler();

	private static final RestTemplate restTemplate = initRestTemplate();
	private static final WebSocketStompClient stompClient = initializeStompClient();
	private final HttpHeaders authHeaders;
	private final HttpHeaders headersMultipart;
	private final String httpUrl;
	private final String httpFileServerUrl;
	private final String webSocketUrl;

	/**
	 * Internal field for logging
	 */
	private final Logger logger;

	public OptimizerConnection(String host, String port, String fileServerHost, String fileServerPort, String username
		, String password, Logger logger) {
		this.logger = logger;
		authHeaders = initHeaders(MediaType.APPLICATION_JSON, username, password);
		headersMultipart = initHeaders(MediaType.MULTIPART_FORM_DATA, username, password);
		httpUrl = String.format("http://%s:%s", host, port);
		webSocketUrl = String.format("ws://%s:%s/optimizer", host, port);
		httpFileServerUrl = String.format("http://%s:%s", fileServerHost, fileServerPort);
	}

	public String sentOptimizationRequest(String networkJSON, String dictionaryJSON, String requestJSON,
										  long timeOutSessionConnect) {
		// If the stompSession was not yet connect, create the connection
		if (stompSession == null || !stompSession.isConnected()){
			try {
				connectStompSession(timeOutSessionConnect);
			} catch (ExecutionException | InterruptedException | TimeoutException e) {
				throw new CompletionException(e);
			}
		}

		// Upload network file
		restTemplate.exchange(
			httpUrl + "/optimizer/network",
			HttpMethod.PUT,
			new HttpEntity<>(networkJSON, authHeaders),
			String.class
		);

		// Upload dictionary
		restTemplate.exchange(
			httpUrl + "/optimizer/dictionary",
			HttpMethod.PUT,
			new HttpEntity<>(dictionaryJSON, authHeaders),
			String.class
		);

		// Submit the optimization request
		ResponseEntity<String> optimizationRequest = restTemplate.postForEntity(
			httpUrl + "/optimizer/submit", new HttpEntity<>(requestJSON, authHeaders),
			String.class);
		try {
			// return the response of the request which contains the id assigned to the request by the Optimizer
			String requestId = JsonUtil.fromString(optimizationRequest.getBody(), OptimizerMessage.class).getId();
			sessionHandler.setRequestID(requestId);
			return requestId;
		} catch (IOException e) {
			throw new CompletionException(e);
		}
	}

	public JobSummary cancelOptimizationRequest(String requestID, long timeOutSessionConnect) throws ExecutionException, InterruptedException, TimeoutException {
		if (stompSession == null || !stompSession.isConnected()){
			connectStompSession(timeOutSessionConnect);
		}
		ResponseEntity<String> optimizationCancelRequest = restTemplate.postForEntity(
			httpUrl + "/optimizer/cancel",
			new HttpEntity<>(JsonUtil.toJson(new OptimizerMessage(null, requestID)), authHeaders),
			String.class
		);
		return new JobSummary(requestID,"", optimizationCancelRequest.getStatusCodeValue() == 200 ?
			Status.Finished: Status.Failed);
	}

	public boolean newOptimizationResultsAvailable() {
		return sessionHandler.newOptimizationResultsAvailable();
	}

	public boolean isOptimizationFinished() {
		return sessionHandler.isOptimizationFinished();
	}

	public OptimizerResponse getLatestOptimizedPlan()  {
		return sessionHandler.getOptimizationResult();
	}

	public OptimizerResponse waitForOptimizerPlan(int pollingTimeMilliSeconds) throws InterruptedException {
		return sessionHandler.waitForOptimizationResult(pollingTimeMilliSeconds);
	}

	/**
	 * Method to serialize the provided {@code graph} instance and upload it to the file server of the Optimizer.
	 *
	 * @param graph
	 *    {@link StreamGraph} to be serialized and uploaded
	 * @return Response of the Optimizer Service
	 */
	public String submitGraphFile(StreamGraph graph, String jobName) throws IOException {
		// Serialize the graph to temp file
		String graphPrefix = "rapidminer_optimizer_graph_job_" + jobName.replaceAll("\\W+", "") + "_";
		File graphFile = ResourceManager.serializeGraph(graphPrefix, graph).toFile();

		// prepare the request content disposition
		MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
		ContentDisposition contentDisposition = ContentDisposition
			.builder("form-data")
			.name("file")
			.filename(graphFile.getName())
			.build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

		// prepare the bondy with the file entity
		HttpEntity<byte[]> fileEntity = new HttpEntity<>(Files.readAllBytes(graphFile.toPath()), fileMap);
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", fileEntity);

		// Create the final request entity
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headersMultipart);

		// send the request
		ResponseEntity<String> response =
			restTemplate.exchange(httpFileServerUrl + "/files", HttpMethod.POST, requestEntity, String.class);
		return response.getBody();
	}

	private static RestTemplate initRestTemplate() {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectTimeout(300_000);
		clientHttpRequestFactory.setReadTimeout(300_000);

		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			@Override
			public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
				return (httpResponse.getStatusCode()
					.series() == CLIENT_ERROR || httpResponse.getStatusCode()
					.series() == SERVER_ERROR);
			}

			@Override
			public void handleError(ClientHttpResponse httpResponse) throws IOException {
				if (httpResponse.getStatusCode().series() == SERVER_ERROR) {
					// handle SERVER_ERROR
				} else if (httpResponse.getStatusCode().series() == CLIENT_ERROR) {
					// handle CLIENT_ERROR
				}
			}
		});
		return restTemplate;
	}

	private static WebSocketStompClient initializeStompClient() {
		//Init the stomp client, make sure the client has sufficient resources
		final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(4);
		taskScheduler.afterPropertiesSet();
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
		container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
		WebSocketStompClient stompClient = new WebSocketStompClient(
			new StandardWebSocketClient(container));
		stompClient.setMessageConverter(new StringMessageConverter());
		stompClient.setTaskScheduler(taskScheduler);
		return stompClient;
	}

	private HttpHeaders initHeaders(MediaType mediaType, String username, String password) {
		HttpHeaders authHeaders = new HttpHeaders();
		authHeaders.setContentType(mediaType);
		authHeaders.setBasicAuth(username, password);
		return authHeaders;
	}

	private void connectStompSession(long timeOutSessionConnect) throws ExecutionException,
		InterruptedException, TimeoutException {
		StompHeaders connectionHeaders = new StompHeaders();
		connectionHeaders.add(StompHeaders.ACK,"client");
		WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders(authHeaders);

		stompSession = stompClient.connect(webSocketUrl, webSocketHttpHeaders,
				connectionHeaders, sessionHandler).get(timeOutSessionConnect, TimeUnit.SECONDS);  //Dont sync this
		stompSession.setAutoReceipt(true);
	}



}

