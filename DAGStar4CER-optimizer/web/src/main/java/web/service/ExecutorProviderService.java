package web.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Service
public class ExecutorProviderService {
    private final int maxCores = Runtime.getRuntime().availableProcessors();

    /**
     * Create an executor service for each request.
     * //TODO can create micro-tasks for each algo in a single executor service
     */
    @Bean("optimizationTaskExecutor1")
    @Scope(SCOPE_PROTOTYPE)
    public ExecutorService createOptimizationTaskExecutor1() {
        return new ThreadPoolExecutor(maxCores, maxCores, 1L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    //Should only handle optimization *REQUESTS*, not tasks.
    @Bean("optimizationRequestHandlerExecutor1")
    public ExecutorService optimizationRequestHandlerExecutor1() {
        return new ThreadPoolExecutor(maxCores, maxCores, 1L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    //For async task submitRequestWithId
    @Bean("submitRequestWithIdAsyncTaskExecutor1")
    public ExecutorService submitRequestWithIdAsyncTaskExecutor1() {
        return new ThreadPoolExecutor(maxCores, maxCores, 1L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }
}
