package web.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import core.parser.benchmarking.BenchmarkingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;

@Service
public class FileServerService {

    @Value("#{systemEnvironment['OPTIMIZER_USER']}")
    private String OPTIMIZER_USER;

    @Value("#{systemEnvironment['OPTIMIZER_PASS']}")
    private String OPTIMIZER_PASS;

    @Value("#{systemEnvironment['ATHENA_FS_URL']}")
    private String FS_URL;

    @Autowired
    private RestTemplate restTemplate;

    //Local vars
    private HttpHeaders jsonHeaders;
    private Gson gson;

    @PostConstruct
    private void postConstruct() {
        this.gson = new GsonBuilder().create();

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        jsonHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        jsonHeaders.setCacheControl(CacheControl.noCache());
        this.jsonHeaders = jsonHeaders;

        this.restTemplate = new RestTemplateBuilder()
                .basicAuthentication(OPTIMIZER_USER, OPTIMIZER_PASS)
                .errorHandler(new DefaultResponseErrorHandler())
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();

    }

    /**
     * Fetches the contents of a file.
     *
     * @param filename The file name.
     * @return The contents of the file or null if not found or an exception occured.
     */
    public String getFileContents(String filename) {
        try {
            return restTemplate.exchange(String.format("http://%s/files/%s", FS_URL, filename), HttpMethod.GET, new HttpEntity<>(jsonHeaders), String.class).getBody();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Checks if a file in the server is present (doesn't fetch the file).
     *
     * @param filename The file path.
     * @return A boolean value based on if the file is present in the remote server or not.
     */
    public boolean checkIfFileExists(String filename) {
        try {
            ResponseEntity<String> resp = restTemplate.exchange(String.format("http://%s/files/exists/%s", FS_URL, filename), HttpMethod.GET, new HttpEntity<>(jsonHeaders), String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                String body = resp.getBody();
                JsonObject json = gson.fromJson(body, JsonObject.class);
                return json.get("message").getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
