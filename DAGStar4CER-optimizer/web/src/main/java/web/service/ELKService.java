package web.service;

import lombok.extern.java.Log;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import web.document.ELKStompLogDocument;
import web.repository.ELKStompLogMessageRepository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
@Log
public class ELKService {

    @Autowired
    private ELKStompLogMessageRepository elkStompLogMessageRepository;


    //Message Queue for STOMP messages awaiting ELK upload
    private BlockingQueue<ELKStompLogDocument> stompMsgQueue;

    @PostConstruct
    public void setup() {
        //Local resources
        this.stompMsgQueue = new ArrayBlockingQueue<>(1000);
    }

    public void appendLogSTOMPMsg(Message<?> message, String source) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return;
        }
        this.stompMsgQueue.offer(new ELKStompLogDocument(accessor, message, source));
    }

    @Scheduled(fixedRate = 10000)
    private void drainSTOMPMessageQueue() {
        List<ELKStompLogDocument> drainedMessages = new ArrayList<>();
        this.stompMsgQueue.drainTo(drainedMessages);
        this.elkStompLogMessageRepository.saveAll(drainedMessages);
    }
}
