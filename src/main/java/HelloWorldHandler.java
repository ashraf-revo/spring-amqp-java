import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.SerializationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shawnritchie on 23/04/15.
 */
public class HelloWorldHandler implements MessageListener {

    private ConnectionFactory connectionFactory;

    private volatile Exchange helloWorldExchange;

    public HelloWorldHandler(ConnectionFactory connectionFactory, Exchange helloWorldExchange)
    {
        this.connectionFactory = connectionFactory;
        this.helloWorldExchange = helloWorldExchange;
    }

    protected Message generateMessage()
    {
        Payload payload = new Payload("Producer: Reply", 1, true);
        Message message =
                MessageBuilder
                        .withBody(
                                SerializationUtils
                                        .serialize(payload)
                        )
                        .build();
        return message;
    }

    protected void sendMessage(String correlationId, Message replyMessage)
            throws Exception
    {
        if (connectionFactory == null)
            System.out.println("is null");

        RabbitTemplate template = new RabbitTemplate(connectionFactory);

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
                        new RetryCallback<Message, Exception>() {
                            @Override
                            public Message doWithRetry(RetryContext context) throws Exception {
                                System.out.println("Reply Sent");
                                context.setAttribute("message", replyMessage);
                                template
                                        .convertAndSend
                                                (
                                                        helloWorldExchange.getName(),
                                                        correlationId,
                                                        replyMessage
                                                );
                                System.out.println("doWithRetry: Sent");
                                return replyMessage;
                            }
                        },
                        new RecoveryCallback<Message>() {
                            @Override
                            public Message recover(RetryContext context) throws Exception {
                                Object message = context.getAttribute("message");
                                Throwable t = context.getLastThrowable();
                                System.out.println("recover: " + t.toString());
                                // Do something with message such as logging the error
                                return null;
                            }
                        }
                );

        template.setRetryTemplate(retryTemplate);
    }

    @Override
    public void onMessage(Message message)
    {
        try {
            System.out.println("Recieved");

            //String correlationId = (String) SerializationUtils.deserialize();
            String correlationId = new String(message.getMessageProperties().getCorrelationId());
            Payload payload = (Payload) SerializationUtils.deserialize(message.getBody());

            System.out.println("Payload: " + payload.toString());
            System.out.println("uuidrequest: " + message.getMessageProperties().getHeaders().get("uuidrequest"));
            System.out.println("correlationId: " + correlationId);

            sendMessage(correlationId, generateMessage());
        }
        catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
