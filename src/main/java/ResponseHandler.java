import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.util.SerializationUtils;

/**
 * Created by shawnritchie on 27/04/15.
 */
public class ResponseHandler implements MessageListener {

    @Override
    public void onMessage(Message message) {
        Payload payload = (Payload) SerializationUtils.deserialize(message.getBody());

        System.out.println("Response: " + payload.toString());
    }
}
