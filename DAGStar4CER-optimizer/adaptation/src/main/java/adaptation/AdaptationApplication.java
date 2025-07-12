package adaptation;

import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@Log
@EnableAsync
@SpringBootApplication
public class AdaptationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AdaptationApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("AdaptationApplication started!");
    }
}
