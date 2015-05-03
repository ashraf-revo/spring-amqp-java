
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.amqp.core.Queue.*;

/**
 * Created by shawnritchie on 23/04/15.
 */
@Configuration
public class HelloWorldConfiguration {

    protected final String helloWorldExchangeName = "hello.world.exchange";

    protected final String helloWorldRoutingKey = "hello.world.routing.key";
    protected final String helloWorldReplyRoutingKey = "hello.world.reply.routing.key";

    protected final String helloWorldQueueName = "hello.world.queue";
    protected final String helloWorldResponseQueueName = "hello.world.response.queue";

    protected Queue createQueue(String name) {
        Queue queue = new Queue(name, false, false, false, new HashMap<String, Object>() {{
            put("x-message-ttl", 4000);
        }});
        return queue;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("user");
        connectionFactory.setPassword("user");
        connectionFactory.setPublisherConfirms(true);
        return connectionFactory;
    }

    @Bean
    public Exchange helloWorldExchange() {
        return new DirectExchange(this.helloWorldExchangeName, false, false);
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        //The routing key is set to the name of the queue by the broker for the default exchange.
        template.setRoutingKey(this.helloWorldQueueName);
        //Where we will synchronously receive messages from
        template.setQueue(this.helloWorldQueueName);

        return template;
    }

}
