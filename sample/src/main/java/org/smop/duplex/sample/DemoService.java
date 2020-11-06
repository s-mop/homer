package org.smop.duplex.sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DemoService {
    
    @RabbitListener(queuesToDeclare = @Queue("mytestqueue"))
    public void checkSome(List<String> tagTuple) {
        log.warn("check here {}", tagTuple);
    }
}
