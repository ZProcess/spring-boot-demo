package demo.cwd.gatling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Created by chenweida on 2018/1/18.
 */
@SpringBootApplication
public class GatlingApplication {
    public static ApplicationContext ctx = null;

    public static void main(String[] args) {
        ctx = SpringApplication.run(GatlingApplication.class, args);
    }


}
