import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by shawnritchie on 23/04/15.
 */
public class Consumer {

    public static void main(String[] args) {
        new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
    }

}
