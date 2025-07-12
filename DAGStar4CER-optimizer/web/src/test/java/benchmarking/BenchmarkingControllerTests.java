package benchmarking;

import com.google.gson.GsonBuilder;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;

/**
 * Benchmarking test suite.
 */
@Log
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BenchmarkingControllerTests {

    @Value("#{systemEnvironment['ATHENA_OPTIMIZER_BIND_ADR']}:#{systemEnvironment['ATHENA_OPTIMIZER_WEB_PORT']}")
    private String ATHENA_BENCHMARKING_URL;

    //ENV variables
    private static final String FS_ENDPOINT = System.getenv("ATHENA_FS_URL");
    private static final String OPTIMIZER_USER = System.getenv("OPTIMIZER_USER");
    private static final String OPTIMIZER_PASS = System.getenv("OPTIMIZER_PASS");

    // Local variables
    private static RestTemplate restTemplate;
    private static HttpHeaders fsHeaders;


    @BeforeAll
    static void beforeAll() {
        //Use this template to interact with the optimizer, contains correct auth headers by default
        restTemplate = new RestTemplateBuilder()
                .basicAuthentication(OPTIMIZER_USER, OPTIMIZER_PASS)
                .errorHandler(new DefaultResponseErrorHandler())
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                    f.setBufferRequestBody(false);  //Recommended for large files.
                    return f;
                })
                .build();

        //Headers
        fsHeaders = new HttpHeaders();
        fsHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        fsHeaders.setCacheControl(CacheControl.noCache());
    }

    /**
     * Upload the necessary file(s) to the remote File Server (FS).
     * A sample JSON file in the resources folder is used in this test.
     */
    @Test
    @Order(1)
    @DisplayName("Uploading file to FS")
    void upload_file_to_fs_test() throws IOException {
        FileSystemResource fsr = new FileSystemResource(new ClassPathResource("input/jar1.jar").getURL().getPath()); //Quick fix for path errors

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fsr);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, fsHeaders);

        ResponseEntity<String> response = restTemplate.exchange(String.format("http://%s/files", FS_ENDPOINT), HttpMethod.POST, requestEntity, String.class);
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(), "Request to FS failed.");
        System.out.printf("Request was successful and returned body: %s%n", response.getBody());
    }

    /**
     * Benchmarking request should return the UID of the submitted request.
     * Request should also be available at an Elasticsearch index.
     */
    @Test
    @Order(2)
    void send_benchmarking_request_to_service_test() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setCacheControl(CacheControl.noCache());

        String requestBody = new String(new ClassPathResource("sample_test_input/bm1.json").getInputStream().readAllBytes());
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(String.format("http://%s/benchmarking/submit", ATHENA_BENCHMARKING_URL), HttpMethod.POST, entity, String.class);
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(), "Request to Benchmarking service failed.");
        String body = response.getBody();   // { id : <bm_id> }
        Assertions.assertNotNull(body, "Benchmarking service response is null.");
        Assertions.assertTrue(body.contains("\"id\""), "Benchmarking service schema mismatch.");
        System.out.printf("Request was successful and returned body: %s%n", body);
    }
}
