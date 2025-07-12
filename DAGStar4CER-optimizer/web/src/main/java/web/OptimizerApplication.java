package web;

import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.logging.Logger;


@EnableWebSecurity
@Log
@EnableAsync
@SpringBootApplication
public class OptimizerApplication implements CommandLineRunner {
    public static void main(String[] args) {
        log.info("Optimizer STARTUP sequence START");
        // Print environment variables ATHENA_ES_URL and ATHENA_KIBANA_URL
        log.info(String.format("ATHENA_ES_URL: %s", System.getenv("ATHENA_ES_URL")));
        log.info(String.format("ATHENA_KIBANA_URL: %s", System.getenv("ATHENA_KIBANA_URL")));
        SpringApplication.run(OptimizerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("dimitris");
        int a = 1;
        int b = a + 1;
        System.out.println(b);

        log.info("Optimizer READY!");

    }

    @PreDestroy
    private void shutdown() {
        log.info("Optimizer shutdown sequence START");
//        log.info(String.format("ENVIRONMENT: %s", getEnvAsString()));
        log.info("Optimizer shutdown sequence FINISH");
    }

    private String getEnvAsString() {
        final String[] env = {""};
        System.getenv()
                .forEach((k, v) -> env[0] += k + ":" + v + ",");
        return env[0];
    }
}
