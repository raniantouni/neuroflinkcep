package optimizer;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.parser.workflow.OptimizationRequest;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Optimization request test suite.
 */
@Log
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OptimizationTests {
    private static final String ATHENA_OPTIMIZER_BIND_ADR = System.getenv("ATHENA_OPTIMIZER_BIND_ADR");
    private static final String ATHENA_OPTIMIZER_WEB_PORT = System.getenv("ATHENA_OPTIMIZER_WEB_PORT");

    //Set by Xenia
    //private static final String ATHENA_OPTIMIZER_BIND_ADR = "192.168.1.9";
    //private static final String ATHENA_OPTIMIZER_WEB_PORT = "8080";

    //ENV variables
    private static final String OPTIMIZER_USER = System.getenv("OPTIMIZER_USER");
    private static final String OPTIMIZER_PASS = System.getenv("OPTIMIZER_PASS");

    //Set by Xenia
    //private static final String OPTIMIZER_USER = "infore_user";
    //private static final String OPTIMIZER_PASS = "infore_pass";

    // Local variables
    private static Gson gson;
    private static Random random;
    private static RestTemplate restTemplate;
    private static StompSession stompSession;
    private static Map<String, BlockingQueue<String>> receivedMessages;
    private static HttpHeaders jsonHeaders;


    @BeforeAll
    static void beforeAll() {
        restTemplate = new RestTemplateBuilder()
                .basicAuthentication(OPTIMIZER_USER, OPTIMIZER_PASS)
                .errorHandler(new DefaultResponseErrorHandler())
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();

        //Headers
        jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        jsonHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        jsonHeaders.setCacheControl(CacheControl.noCache());

        //JSON parser
        gson = new GsonBuilder().create();

        //Set random seed
        random = new Random(0);

        //Init the stomp client, make sure the client has sufficient resources
        final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.afterPropertiesSet();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient(container));
        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.setTaskScheduler(taskScheduler);

        //Handler
        StompSessionHandler sessionHandler = new StompSessionHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object msg) {
                String topic = headers.getDestination();
                String payload = (String) msg;
                if (headers.containsKey("message-id")) {
                    receivedMessages.get(topic).add(payload);
                }
                System.out.printf("RECEIVED: %s -> %s%n", topic, payload);
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                //Subscribe to topics
                List<String> subDestinations = new ArrayList<>();
                subDestinations.add("/topic/broadcast");
                subDestinations.add("/user/queue/echo");
                subDestinations.add("/user/queue/optimization_results");
                subDestinations.add("/user/queue/info");
                subDestinations.add("/user/queue/errors");

                receivedMessages = new HashMap<>();
                subDestinations.forEach(sub -> {
                    receivedMessages.put(sub, new ArrayBlockingQueue<>(1000));
                    synchronized (session) {
                        session.subscribe(sub, this);
                    }
                });
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                Assertions.fail(String.format("handleException [%s] with payload [%s]", exception.getMessage(), new String(payload)));
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                Assertions.fail(String.format("handleTransportError [%s] with session [%s]", exception.toString(), session.getSessionId()));
            }
        };

        //Connect to the optimizer endpoint
        try {
            StompHeaders connectionHeaders = new StompHeaders();
            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.setBasicAuth(OPTIMIZER_USER, OPTIMIZER_PASS);
            stompSession = stompClient.connect(String.format("ws://%s:%s/optimizer", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT),
                    webSocketHttpHeaders, connectionHeaders, sessionHandler).get(30, TimeUnit.SECONDS);  //Dont sync this
            stompSession.setAutoReceipt(true);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    /**
     * Disconnects from the stomp session gracefully.
     */
    @AfterAll
    static void tearDown() {
        if (stompSession != null) {
            synchronized (stompSession) {
                stompSession.disconnect();
            }
        }
    }

    /**
     * Ensure that no messages carry over between tests by draining and printing leftover messages.
     */
    @BeforeEach
    @AfterEach
    void beforeAndAfterEachTest() {
        for (String topic : receivedMessages.keySet()) {
            BlockingQueue<String> topicQueue = receivedMessages.get(topic);
            if (!topicQueue.isEmpty()) {
                System.out.println("Draining: " + String.join(",", topicQueue));
            }
        }
    }

    /**
     * Ensures that the echo STOMP endpoint is up and running.
     * Sends a random message to the Optimizer service and expects back a message with the same contents.
     *
     * @throws InterruptedException if message topic key is not present in the messages mapping.
     */
    @Test
    @Order(1)
    @DisplayName("Echo")
    public void echo_test() throws InterruptedException {
        String message = String.valueOf(random.nextFloat());
        System.out.printf("Echoing message [%s].%n", message);
        synchronized (stompSession) {
            stompSession.send("/app/echo", message);
        }
        Assertions.assertEquals(receivedMessages.get("/user/queue/echo").poll(5, TimeUnit.SECONDS), message);
    }

    /**
     * Ensures that the Optimization service can receive Dictionary and Network resources.
     * Contents uploaded to the Optimization service are not deleted afterwards.
     *
     * @throws IOException if message topic key is not present in the messages mapping.
     */
    @Test
    @Order(2)
    @DisplayName("Upload network and dictionary files.")
    public void uploadNetworkAndDictionaryFiles() throws IOException {
        //Parse the contents of these 2 files
        String dictionaryContents = new String(new ClassPathResource("sample_test_input/dictionary.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String networkContents = new String(new ClassPathResource("sample_test_input/network.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        //Send
        ResponseEntity<String> dr1 = restTemplate.exchange(String.format("http://%s:%s/optimizer/dictionary", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT),
                HttpMethod.PUT, new HttpEntity<>(dictionaryContents, jsonHeaders), String.class);
        System.out.println("Dictionary response: " + dr1);

        ResponseEntity<String> nr1 = restTemplate.exchange(String.format("http://%s:%s/optimizer/network", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT),
                HttpMethod.PUT, new HttpEntity<>(networkContents, jsonHeaders), String.class);
        System.out.println("Network response: " + nr1);
    }

    /**
     * Ensures that the Optimizer service can correctly receive and process an Optimization request.
     * An optimized workflow is expected to be present at the '/user/queue/optimization_results' STOMP topic.
     * An attempt to cancel the submitted request is made *AFTER* it has been completed which should fail.
     *
     * @throws IOException          if message topic key is not present in the messages mapping.
     * @throws InterruptedException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read.
     */
    @Test
    @Order(3)
    @DisplayName("Submit workflow for optimization")
    public void optimizationRequestTest() throws IOException, InterruptedException {
        //Parse workflow contents
        OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/life_science_no_bayesian_opt.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        //OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/workflow_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        request.getOptimizationParameters().setContinuous(false);
        String requestContents = gson.toJson(request);

        //Send
        ResponseEntity<String> wr = restTemplate.exchange(String.format("http://%s:%s/optimizer/submit", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestContents, jsonHeaders), String.class);
        String requestIdJSON = Objects.requireNonNull(wr.getBody());    // requestIdJSON format: {"id":"XXX"}
        System.out.println("Request submission result: " + wr);

        //Handle WS messages
        String optimization_response = receivedMessages.get("/user/queue/optimization_results").poll(120_000, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(optimization_response, "Failed to retrieve plan and timed out...");
        System.out.printf("Plan:     %s%n", optimization_response);

        //Cancel
        System.out.println("Sending a cancel request for request with payload " + requestIdJSON);
        String cancelWR = restTemplate.exchange(String.format("http://%s:%s/optimizer/cancel", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestIdJSON, jsonHeaders), String.class).getBody();
        Assertions.assertFalse(Boolean.parseBoolean(cancelWR), "Should not be able to cancel a completed request.");
        System.out.printf("Cancel request result: [%s]%n", cancelWR);
    }

    /**
     * Ensures that the Optimizer service can correctly receive and process an Optimization request.
     * An attempt to cancel the submitted request is made right after it was sent which is expected to succeed.
     * An optimized workflow should *NOT* be present at the '/user/queue/optimization_results' STOMP topic.
     * Note: Due to the asynchronous nature of this message passing interface the cancel requests may be delayed enough
     * that an easy-to-optimize workflow could be completed before the cancel request is processed.
     *
     * @throws IOException          if message topic key is not present in the messages mapping.
     * @throws InterruptedException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read.
     */
    @Test
    @Order(4)
    @DisplayName("Submit workflow for optimization and cancel before completion")
    public void optimizationRequestTestWithCancel() throws IOException, InterruptedException {
        //Parse workflow contents
        OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/life_science_no_bayesian_opt.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        //OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/workflow_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        request.getOptimizationParameters().setContinuous(false);
        request.getOptimizationParameters().setAlgorithm("op-ES");
        String requestContents = gson.toJson(request);

        //Send
        ResponseEntity<String> wr = restTemplate.exchange(String.format("http://%s:%s/optimizer/submit", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestContents, jsonHeaders), String.class);
        String requestIdJSON = Objects.requireNonNull(wr.getBody());    // requestIdJSON format: {"id":"XXX"}
        System.out.println("Request submission result: " + wr);

        //Cancel
        System.out.println("Sending a cancel request for request with payload " + requestIdJSON);
        String cancelWR = restTemplate.exchange(String.format("http://%s:%s/optimizer/cancel", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestIdJSON, jsonHeaders), String.class).getBody();
        System.out.printf("Response for 'cancel' request with id=[%s] returned [%s]%n", requestIdJSON, cancelWR);
        Assertions.assertTrue(Boolean.parseBoolean(cancelWR), "Should be able to cancel a completed request.");
        String polledMessage = receivedMessages.get("/user/queue/info").poll(1000, TimeUnit.MILLISECONDS);
        System.out.printf("Polled message [%s] after 'cancel' request with workflow name=[%s]%n", polledMessage, request.getWorkflowName());


        //Handle WS messages
        String plan = receivedMessages.get("/user/queue/optimization_results").poll(1000, TimeUnit.MILLISECONDS);
        Assertions.assertNull(plan, "Should not have been able to get results from the Optimizer service.");
    }

    @Test
    @Order(5)
    void continuous_optimization_test_1() throws InterruptedException, IOException {

        //Parse workflow contents
        OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/life_science_no_bayesian_opt.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        request.getOptimizationParameters().setContinuous(true);
        request.getOptimizationParameters().setAlgorithm("auto");
        String requestContents = gson.toJson(request);

        //Send
        ResponseEntity<String> wr = restTemplate.exchange(String.format("http://%s:%s/optimizer/submit", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestContents, jsonHeaders), String.class);
        String requestIdJSON = Objects.requireNonNull(wr.getBody());    // requestIdJSON format: {"id":"XXX"}
        System.out.println("Request submission result: " + wr);

        //The optimizer service will keep publishing optimized workflows until a cancel request is sent or a hard limit is reached.
        //Wait for 10s or until a message arrives, tolerate up to 5 delayed messages.
        //Wait for 5 optimized workflows and then cancel.
        int opt_workflows_left = 5;
        int delays_left = 5;
        while (opt_workflows_left > 0 && delays_left > 0) {
            //Block and wait for a new message
            String plan = receivedMessages.get("/user/queue/optimization_results").poll(10_000, TimeUnit.MILLISECONDS);
            if (plan == null) {
                delays_left--;
            } else {
                System.out.println("New plan: " + plan);
                opt_workflows_left--;
            }
        }

        //Cancel the optimization query
        String cancelWR = restTemplate.exchange(String.format("http://%s:%s/optimizer/cancel", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestIdJSON, jsonHeaders), String.class).getBody();
        System.out.printf("Response for 'cancel' request with id=[%s] returned [%s]%n", requestIdJSON, cancelWR);
    }

    // ----- Tests added by Xenia ----

    /**
     * Ensures that the Optimizer service can correctly receive and process an Optimization request.
     * An optimized workflow is expected to be present at the '/user/queue/optimization_results' STOMP topic.
     *
     *
     * @throws IOException          if message topic key is not present in the messages mapping.
     * @throws InterruptedException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read.
     */
    @Test
    @Order(6)
    @DisplayName("Submit workflow for optimization")
    public void optimizationRequestTest2() throws IOException, InterruptedException {

        //UPLOAD DICTIONARY AND NETWORK FILES

        //Parse the contents of the dictionary and network files
        String dictionaryContents = new String(new ClassPathResource("sample_test_input_generated_with_RMStudio/dictionary_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String networkContents = new String(new ClassPathResource("sample_test_input_generated_with_RMStudio/network_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        //Send
        ResponseEntity<String> dr1 = restTemplate.exchange(String.format("http://%s:%s/optimizer/dictionary", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT),
                HttpMethod.PUT, new HttpEntity<>(dictionaryContents, jsonHeaders), String.class);
        System.out.println("Dictionary response: " + dr1);

        ResponseEntity<String> nr1 = restTemplate.exchange(String.format("http://%s:%s/optimizer/network", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT),
                HttpMethod.PUT, new HttpEntity<>(networkContents, jsonHeaders), String.class);
        System.out.println("Network response: " + nr1);

        //---------

        // SUBMIT REQUEST

        //Parse workflow contents
        OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input_generated_with_RMStudio/request_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        //OptimizationRequest request = gson.fromJson(new String(new ClassPathResource("sample_test_input/workflow_write.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8), OptimizationRequest.class);
        request.getOptimizationParameters().setContinuous(false);
        String requestContents = gson.toJson(request);

        //Send
        ResponseEntity<String> wr = restTemplate.exchange(String.format("http://%s:%s/optimizer/submit", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
                new HttpEntity<>(requestContents, jsonHeaders), String.class);
        //String requestIdJSON = Objects.requireNonNull(wr.getBody());    // requestIdJSON format: {"id":"XXX"}
        System.out.println("Request submission result: " + wr);

        //-----

        // GET OPTIMIZATION RESULTS

        //Handle WS messages
        String optimization_response = receivedMessages.get("/user/queue/optimization_results").poll(120_000, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(optimization_response, "Failed to retrieve plan and timed out...");
        //System.out.printf("Plan:     %s%n", optimization_response);

        //Cancel
//        System.out.println("Sending a cancel request for request with payload " + requestIdJSON);
//        String cancelWR = restTemplate.exchange(String.format("http://%s:%s/optimizer/cancel", ATHENA_OPTIMIZER_BIND_ADR, ATHENA_OPTIMIZER_WEB_PORT), HttpMethod.POST,
//                new HttpEntity<>(requestIdJSON, jsonHeaders), String.class).getBody();
//        Assertions.assertFalse(Boolean.parseBoolean(cancelWR), "Should not be able to cancel a completed request.");
//        System.out.printf("Cancel request result: [%s]%n", cancelWR);
    }
}