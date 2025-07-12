package web.controller;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import web.configuration.WebSocketConfig;

import javax.annotation.PostConstruct;
import java.security.Principal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * STOMP endpoints
 * <p>
 * "/topic/broadcast"
 * "/user/queue/echo"
 * "/user/queue/info"
 * "/user/queue/optimization_results"
 * "/user/queue/errors"
 */
@Controller
@Log
public class WebsocketController {
    @Autowired
    private WebSocketConfig webSocketConfig;

        @MessageMapping("/echo")
    @SendToUser("/queue/echo")
    public String echo(String message, @Header("simpSessionId") String sessionId, Principal principal) {
        log.config(String.format("%s echos %s with sessionId %s.", principal.getName(), message, sessionId));
        return message;
    }

    @MessageMapping("/broadcast")
    public void broadcast(String message, @Header("simpSessionId") String sessionId) {
        log.config(String.format("Broadcast %s with sessionId %s.", message, sessionId));
    }

    @MessageMapping("/fs")
    public void broadcast_fs_event(String message, @Header("simpSessionId") String sessionId) {
        log.config(String.format("FS event %s with sessionId %s.", message, sessionId));
    }

    @MessageMapping("/benchmarking")
    public void benchmark(String message, @Header("simpSessionId") String sessionId) {
        log.config(String.format("Benchmark message [%s] with sessionId %s.", message, sessionId));
    }

    @MessageMapping("/bayesian_optimization")
    public void bayesian_optimization(String message, @Header("simpSessionId") String sessionId) {
        log.info(String.format("Bayesian optimization message [%s] with sessionId %s.", message, sessionId));
    }
}
