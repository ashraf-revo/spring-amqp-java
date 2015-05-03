
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.util.SerializationUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by shawnritchie on 23/04/15.
 */
@Configuration
public class ProducerConfiguration extends HelloWorldConfiguration{


    @Bean
    public ScheduledProducer scheduledProducer() {
        return new ScheduledProducer();
    }

    @Bean
    public BeanPostProcessor postProcessor() {
        return new ScheduledAnnotationBeanPostProcessor();
    }

    @Bean
    public Queue helloWorldResponseQueue() {
        return createQueue(this.helloWorldResponseQueueName);
    }

    @Bean
    @Autowired
    public Binding helloWorldBinding(Exchange helloWorldExchange, Queue helloWorldResponseQueue) {
        Binding binding =
                BindingBuilder
                        .bind(helloWorldResponseQueue)
                        .to(helloWorldExchange)
                        .with(this.helloWorldReplyRoutingKey)
                        .noargs();

        return binding;
    }

    @Bean
    @Autowired
    public SimpleMessageListenerContainer listenerContainer(Queue helloWorldResponseQueue) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueues(helloWorldResponseQueue);
        container.setMessageListener(new MessageListenerAdapter(new ResponseHandler()));
        return container;
    }

    static class ScheduledProducer {

        @Autowired
        private ConnectionFactory connectionFactory;

        @Autowired
        private volatile Exchange helloWorldExchange;

        private final AtomicInteger counter = new AtomicInteger();

        protected Message GenerateMessage()
        {
            Payload payload = new Payload("Hello World", counter.incrementAndGet(), true);
            Message message =
                    MessageBuilder
                            .withBody(
                                    SerializationUtils
                                            .serialize(payload)
                            )
                            .setHeader("uuidrequest", java.util.UUID.randomUUID().toString())
                            .setCorrelationId("hello.world.reply.routing.key".getBytes())
                            .build();
            System.out.println(payload.toString());
            return message;
        }

        @Scheduled(fixedRate = 60000)
        public void sendPayload() throws Exception{
            Message message = GenerateMessage();

            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            //template.setChannelTransacted(true);


            template.setReturnCallback(new RabbitTemplate.ReturnCallback() {
                @Override
                public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                    System.out.println("ReturnedMessage: ");
                }
            });


            template.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
                @Override
                public void confirm(CorrelationData correlationData, boolean b, String s) {
                    System.out.println("Confirmed: ");
                }
            });

            RetryTemplate retryTemplate = new RetryTemplate();
            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(500);
            backOffPolicy.setMultiplier(10.0);
            backOffPolicy.setMaxInterval(10000);
            retryTemplate.setBackOffPolicy(backOffPolicy);

            Map< Class<? extends Throwable>, Boolean> exceptionMap =
                    new HashMap< Class<? extends Throwable>, Boolean>();
            retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, exceptionMap));

            retryTemplate
                    .execute(
                            //Executed the following callback when a
                            //message being sent fails
                            new RetryCallback<Message, Exception>() {
                                @Override
                                public Message doWithRetry(RetryContext context) throws Exception {
                                    Payload payload = (Payload) SerializationUtils.deserialize(message.getBody());
                                    System.out.println("doWithRetry: " + payload.toString());
                                    context.setAttribute("message", message);
                                    template
                                            .convertAndSend
                                                    (
                                                            helloWorldExchange.getName(),
                                                            "hello.world.routing.key",
                                                            message
                                                    );
                                    System.out.println("doWithRetry: Sent");
                                    return message;
                                }
                            },
                            //Once all attempts to send the message have been
                            //exhausted run the following callback
                            new RecoveryCallback<Message>() {
                                @Override
                                public Message recover(RetryContext context) throws Exception {
                                    Payload payload = (Payload) SerializationUtils.deserialize(message.getBody());
                                    Object message = context.getAttribute("message");
                                    Throwable t = context.getLastThrowable();
                                    System.out.println("recover: " + t.toString());
                                    // Do something with message such as logging the error
                                    return null;
                                }
                        }
                );


            template.setRetryTemplate(retryTemplate);




            //rabbitTemplate.convertAndSend(message);
//            template.convertAndSend(helloWorldExchange.getName(), "hello.world.routing.key", message);
//            System.out.println("template: sent");
        }
    }

}
