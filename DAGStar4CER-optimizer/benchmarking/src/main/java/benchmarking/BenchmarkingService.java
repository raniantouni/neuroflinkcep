package benchmarking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.java.Log;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.nio.channels.UnresolvedAddressException;

@Service
@Log
public class BenchmarkingService {

    @Value("#{systemEnvironment['BENCHMARKING_STOMP_USERNAME']}")
    private String STOMP_USERNAME;

    @Value("#{systemEnvironment['BENCHMARKING_STOMP_PASSWORD']}")
    private String STOMP_PASSWORD;

    @Value("#{systemEnvironment['ATHENA_OPTIMIZER_URL']}")
    private String ATHENA_OPTIMIZER_URL;


    @Autowired
    private Gson gson;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RestHighLevelClient elkClient;

    @Autowired
    @Qualifier("getBenchmarkingWSClient")
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
    public Gson getGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    @Bean
    public void processRequests() throws ExecutionException, InterruptedException, TimeoutException {
        log.config("Started processing requests.");
        final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(1000);
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
                subDestinations.add(String.format("/topic/%s", System.getenv("BENCHMARKING_STOMP_TOPIC")));
                subDestinations.add(String.format("/topic/%s", System.getenv("BROADCAST_STOMP_TOPIC")));
                subDestinations.add(String.format("/topic/%s", System.getenv("BO_STOMP_TOPIC")));
                subDestinations.forEach(sub -> session.subscribe(sub, this));
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.severe(String.format("handleException [%s] with payload [%s]", exception.getMessage(), new String(payload)));
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.severe(String.format("handleTransportError [%s] with session [%s]", exception.getMessage(), session.getSessionId()));
            }
        };

        //Connect
        StompHeaders connectionHeaders = new StompHeaders();
        WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
        webSocketHttpHeaders.setBasicAuth(STOMP_USERNAME, STOMP_PASSWORD);
        StompSession stompSession = this.stompClient.connect(String.format("ws://%s/optimizer", ATHENA_OPTIMIZER_URL),
                webSocketHttpHeaders, connectionHeaders, sessionHandler).get(10, TimeUnit.SECONDS);
        stompSession.setAutoReceipt(true);

        String message = "123";
        StompSession.Receiptable receipt = stompSession.send("/app/echo", message);
        log.config("Receipt: " + receipt.getReceiptId());
        if (!message.equals(messages.poll(5, TimeUnit.SECONDS))) {
            throw new IllegalStateException("Mismatch");
        }
        log.config("Stopped processing requests.");
    }
}
