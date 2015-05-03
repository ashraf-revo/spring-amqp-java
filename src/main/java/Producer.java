import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by shawnritchie on 23/04/15.
 */
public class Producer {

    public static void main(String[] args) throws Exception {
        new AnnotationConfigApplicationContext(ProducerConfiguration.class);
    }

}
