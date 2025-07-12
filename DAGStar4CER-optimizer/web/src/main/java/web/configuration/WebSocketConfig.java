package web.configuration;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import web.service.ELKService;

import javax.annotation.PostConstruct;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@Log
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private ELKService elkService;

    private BlockingQueue<String> BOQueue;

    @PostConstruct
    private void init() {
        this.BOQueue = new ArrayBlockingQueue<>(10);
    }

    @Bean
    public WebSocketStompClient getInternalWSClient() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.afterPropertiesSet();

        List<Transport> transports = new ArrayList<>();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
        transports.add(new WebSocketTransport(new StandardWebSocketClient(container)));
        WebSocketClient webSocketClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setInboundMessageSizeLimit(1024 * 1024);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setTaskScheduler(taskScheduler);
        return stompClient;
    }

    //Server side
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        return container;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");   //Prepend this when sending a message to an endpoint
        config.enableSimpleBroker("/queue", "/topic");      //Prepend this when subscribing to get result from a @SendTo
        config.setUserDestinationPrefix("/user");   //Prefix to client-specific subs (e.g /user/clientid/queue/mapping1)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //Use this when creating a socket
        registry.addEndpoint("/optimizer").setAllowedOrigins("*");
        registry.addEndpoint("/optimizer").setAllowedOrigins("*").withSockJS();
    }

    @EventListener
    public void handleConnectEvent(SessionConnectEvent event) {
        if (event.getUser() != null) {
            log.info("===> handleConnectEvent: username=" + event.getUser().getName() + ", event=" + event);
        } else {
            log.info("===> handleConnectEvent with event=" + event);
        }
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        if (event.getUser() != null) {
            log.config("<==> handleSubscribeEvent: username=" + event.getUser().getName() + ", event=" + event);
        } else {
            log.config("<==> handleSubscribeEvent with event=" + event);
        }
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        if (event.getUser() != null) {
            log.info("<=== handleDisconnectEvent: username=" + event.getUser().getName() + ", event=" + event);
        } else {
            log.info("<=== handleDisconnectEvent with event=" + event);
        }
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return null;
                }
                if (StompCommand.CONNECT == accessor.getCommand()) {
                    UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) accessor.getUser();
                    if (token != null && !token.isAuthenticated()) {
                        return null;
                    }
                }
                elkService.appendLogSTOMPMsg(message, "preSend");
                return message;
            }

            @Override
            public Message<?> postReceive(Message<?> message, MessageChannel channel) {
                final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    elkService.appendLogSTOMPMsg(message, "postReceive");
                }
                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {

    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        return false;
    }
}
