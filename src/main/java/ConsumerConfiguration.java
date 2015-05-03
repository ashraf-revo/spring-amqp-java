import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shawnritchie on 23/04/15.
 */
@Configuration
public class ConsumerConfiguration extends HelloWorldConfiguration {

    @Bean
    public RabbitTransactionManager rabbitTransactionManager() {
        return new RabbitTransactionManager(connectionFactory());
    }

    @Bean
    public Queue helloWorldQueue() {
        return createQueue(this.helloWorldQueueName);
    }

    @Bean
    @Autowired
    public Binding helloWorldBinding(Exchange helloWorldExchange, Queue helloWorldQueue) {
        Binding binding =
                BindingBuilder
                        .bind(helloWorldQueue)
                        .to(helloWorldExchange)
                        .with(this.helloWorldRoutingKey)
                        .noargs();

        return binding;
    }

    @Bean
    @Autowired
    public SimpleMessageListenerContainer listenerContainer(Queue helloWorldQueue) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
//        container.setTransactionManager(rabbitTransactionManager() );
//        container.setChannelTransacted(true);
        container.setQueues(helloWorldQueue);
        container.setMessageListener(new MessageListenerAdapter(new HelloWorldHandler(connectionFactory(), helloWorldExchange())));
        return container;
    }

}
