package adaptation;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Log
public class AdaptationService {
    @Value("#{systemEnvironment['ADAPTATION_STOMP_USERNAME']}")
    private String STOMP_USERNAME;

    @Value("#{systemEnvironment['ADAPTATION_STOMP_PASSWORD']}")
    private String STOMP_PASSWORD;

    @Value("#{systemEnvironment['ATHENA_OPTIMIZER_URL']}")
    private String ATHENA_OPTIMIZER_URL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("getAdaptationWSClient")
    private WebSocketStompClient stompClient;


    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder()
                .basicAuthentication(STOMP_USERNAME, STOMP_PASSWORD)
                .errorHandler(new DefaultResponseErrorHandler())
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }

    @Bean
    public void processRequests() throws InterruptedException, ExecutionException, TimeoutException {
        log.config("Started processing requests.");
        final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(1000); //All messages
        final ArrayBlockingQueue<String> optimizer_responses = new ArrayBlockingQueue<>(1000);  //Optimizer plans only
        StompSessionHandler sessionHandler = new StompSessionHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(final StompHeaders headers, final Object payload) {
                String destination = headers.getDestination();
                String msg = (String) payload;
                if (headers.containsKey("message-id")) {
                    messages.add(msg);
                    if (destination != null && destination.equals("/user/queue/optimization_results")) {
                        optimizer_responses.add(msg);
                    }
                }
                if (destination != null || msg != null) {
                    log.config(String.format("RECEIVED: %s -> %s%n", destination, msg));
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                //Subscriptions
                List<String> subDestinations = new ArrayList<>();
                subDestinations.add("/user/queue/echo");
                subDestinations.add("/user/queue/optimization_results");
                subDestinations.add("/user/queue/errors");
                subDestinations.forEach(sub -> session.subscribe(sub, this));
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.severe(String.format("handleException [%s] with payload [%s]", exception.getMessage(), new String(payload)));
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.severe(String.format("handleTransportError [%s] with session [%s]", exception.toString(), session.getSessionId()));
            }
        };

        //Connect
        StompHeaders connectionHeaders = new StompHeaders();
        WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
        webSocketHttpHeaders.setBasicAuth(STOMP_USERNAME, STOMP_PASSWORD);
        StompSession stompSession = null;

        //Try a few times before giving up
        for (int i = 0; i < 10; i++) {
            try {
                stompSession = this.stompClient.connect(String.format("ws://%s/optimizer", ATHENA_OPTIMIZER_URL),
                        webSocketHttpHeaders, connectionHeaders, sessionHandler).get(10, TimeUnit.SECONDS);
                stompSession.setAutoReceipt(true);
                log.info("Successfully connected to Optimizer WS.");
                break;
            } catch (Exception e) {
                log.severe(String.format("Failed to contact the Optimizer service at %s with an error of %s", ATHENA_OPTIMIZER_URL, e.getMessage()));
                Thread.sleep(5000);
            }
        }
        if (stompSession == null) {
            log.severe("Giving up and exiting since the Optimizer service was not reachable.");
            return;
        }

        //Echo test
        String message = "123";
        StompSession.Receiptable receipt = stompSession.send("/app/echo", message);
        log.config("Receipt: " + receipt.getReceiptId());
        if (message.equals(messages.poll(5, TimeUnit.SECONDS))) {
            log.info("Echo success");
        } else {
            throw new IllegalStateException("Mismatch");
        }

        //Main loop
        try {
            while (true) {
                log.info("Waiting for an optimization result..");
                String msg = optimizer_responses.take();
                log.info("Received optimization result: " + msg);
                String result = processAdaptationMessage(msg);
                log.info("Produced adaptation result: " + result);
            }
        } catch (Exception e) {
            stompSession.disconnect();
            e.printStackTrace();
            log.severe(e.getMessage());
        }
        log.info("Exiting!");
    }

    private String processAdaptationMessage(String msg) {
        //TODO implement
        return "NOPE";
    }
}
