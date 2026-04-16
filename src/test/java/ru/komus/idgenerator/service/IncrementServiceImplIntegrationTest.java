package ru.komus.idgenerator.service;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.ClassRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.OracleContainer;
import ru.komus.idgenerator.data.dto.ErrorDto;
import ru.komus.idgenerator.data.dto.IdIncrementDto;
import ru.komus.idgenerator.data.dto.IdIncrementUpdateDto;
import ru.komus.idgenerator.data.dto.RangeDto;
import ru.komus.idgenerator.data.model.IdIncrement;
import ru.komus.idgenerator.repository.IncrementRepository;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { IncrementServiceImplIntegrationTest.Initializer.class})
public class IncrementServiceImplIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;
    private RestTemplate patchRestTemplate;

    @Autowired
    IncrementRepository incrementRepository;
    @Autowired
    IncrementService incrementService;

    @ClassRule
    public static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-xe:18.4.0-slim");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            oracleContainer.start();
            TestPropertyValues.of(
                    "spring.datasource.url=" + oracleContainer.getJdbcUrl(),
                    "spring.datasource.username=" + oracleContainer.getUsername(),
                    "spring.datasource.password=" + oracleContainer.getPassword(),
                    "spring.jpa.hibernate.ddl-auto=create-drop"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @BeforeEach
    public void insertIncrements() {
        this.patchRestTemplate = testRestTemplate.getRestTemplate();
        HttpClient httpClient = HttpClientBuilder.create().build();
        this.patchRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        incrementRepository.save(new IdIncrement("test_generator_1", 1000, 5000L));
        incrementRepository.save(new IdIncrement("test_generator_2", 500, 1000L));
        incrementRepository.save(new IdIncrement("test_generator_3", 1000, 2000L));
        incrementRepository.save(new IdIncrement("test_generator_4", 1000, 2000L));
        incrementRepository.save(new IdIncrement("test_generator_5", 1000, 2000L));
        incrementRepository.save(new IdIncrement("concurrent_generator", 10, 0L));
    }

    @Test
    public void checkNextIdTest(){
        RangeDto rangeDto = new RangeDto();
        rangeDto.setStart(6000L);
        rangeDto.setCount(1000);

        incrementService.getNextIds("test_generator_1");
        RangeDto test_generator = incrementService.getNextIds("test_generator_1");

        Assertions.assertEquals(rangeDto, test_generator);
    }

    @Test
    public void checkNotExistGeneratorTest(){
        Exception exception = assertThrows(NoSuchElementException.class,
            () -> incrementService.getNextIds("test_generator_007"));
        String expectedMessage = "generatorCode test_generator_007 does not exist.";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void checkSuccessSuccessCreateMethod(){
        HttpEntity<IdIncrementDto> request = createAdminRequest("new_key", 100, 1L);

        ResponseEntity<IdIncrementDto> result = this.testRestTemplate
            .postForEntity("/api/v1/admin/key", request, IdIncrementDto.class);

        Optional<IdIncrement> incrementOpt = incrementRepository.findByGeneratorCode("new_key");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertTrue(incrementOpt.isPresent());
        assertEquals("new_key", incrementOpt.get().getGeneratorCode());
        assertEquals(100, incrementOpt.get().getBatch());
        assertEquals(1L, incrementOpt.get().getIncrement());
    }

    @Test
    public void checkCreateMethodWithWrongData(){
        HttpEntity<IdIncrementDto> request = createAdminRequest("test_generator_1", 100, 1L);
        ResponseEntity<ErrorDto> result = this.testRestTemplate
            .postForEntity("/api/v1/admin/key", request, ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("test_generator_1 already exists.", result.getBody().getErrorMessage());
    }

    @Test
    public void checkCreateMethodWithWrongKeyData(){
        HttpEntity<IdIncrementDto> request = createAdminRequest("", 0, 0L);
        ResponseEntity<ErrorDto> result = this.testRestTemplate
            .postForEntity("/api/v1/admin/key", request, ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("generatorCode can't be empty", result.getBody().getErrorMessage());
    }


    @Test
    public void checkSuccessPatchMethod(){
        HttpEntity<IdIncrementUpdateDto> request = createAdminPatchRequest( 100, 500700L);
        this.patchRestTemplate.exchange("/api/v1/admin/key/test_generator_2", HttpMethod.PATCH, request, IdIncrementDto.class);

        Optional<IdIncrement> keyOpt = incrementRepository.findByGeneratorCode("test_generator_2");

        assertTrue(keyOpt.isPresent());
        assertEquals("test_generator_2", keyOpt.get().getGeneratorCode());
        assertEquals(100, keyOpt.get().getBatch());
        assertEquals(500700L, keyOpt.get().getIncrement());
    }

    @Test
    public void checkPatchMethodWithWrongKey(){
        HttpEntity<IdIncrementUpdateDto> request = createAdminPatchRequest( 0, 0L);
        ErrorDto result = this.patchRestTemplate
            .patchForObject("/api/v1/admin/key/wrong_key", request, ErrorDto.class);

        assertEquals("generatorCode wrong_key does not exist.", result.getErrorMessage());
    }

    @Test
    public void checkPatchMethodWithNotFullBody(){
        HttpEntity<IdIncrementUpdateDto> request = createAdminPatchRequest( null, 3000L);
        this.patchRestTemplate.patchForObject("/api/v1/admin/key/test_generator_3", request, IdIncrementDto.class);

        IdIncrement increment = incrementRepository.findByGeneratorCode("test_generator_3").get();

        assertEquals("test_generator_3", increment.getGeneratorCode());
        assertEquals(1000, increment.getBatch());
        assertEquals(3000L, increment.getIncrement());
    }

    @Test
    public void checkSuccessDeleteMethod(){
        HttpHeaders headers = getHttpAdminsHeaders();
        this.testRestTemplate.exchange("/api/v1/admin/key/test_generator_3", HttpMethod.DELETE, new HttpEntity<>(headers), ErrorDto.class);
        Optional<IdIncrement> keyOpt = incrementRepository.findByGeneratorCode("test_generator_3");
        assertTrue(keyOpt.isEmpty());
    }

    @Test
    public void checkDeleteMethodWithWrongKey(){
        HttpHeaders headers = getHttpAdminsHeaders();
        ResponseEntity<ErrorDto> result = this.testRestTemplate.exchange("/api/v1/admin/key/wrong_key", HttpMethod.DELETE, new HttpEntity<>(headers), ErrorDto.class);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("generatorCode wrong_key does not exist.", result.getBody().getErrorMessage());
    }

    @Test
    public void concurrentTest() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(100);
        for (int i = 1; i <= 100; i++) {
            new Thread(()-> {
                try {
                    start.countDown();
                    start.await();
                    incrementService.getNextIds("concurrent_generator");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(2000);
        List<IdIncrementDto> incrementDtos = incrementService.getAll();
        IdIncrementDto idIncrementDto = incrementDtos.stream().filter(dto -> "concurrent_generator".equals(dto.getGeneratorCode())).findFirst().get();
        assertEquals(1000, idIncrementDto.getIncrement());
    }

    private HttpEntity<IdIncrementDto> createAdminRequest(String generatorCode, Integer batch, Long increment){
        HttpHeaders headers = getHttpAdminsHeaders();
        IdIncrementDto body = new IdIncrementDto(generatorCode, batch, increment);

       return new HttpEntity<>(body,headers);
    }

    private HttpHeaders getHttpAdminsHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("admin", "admin");
        return headers;
    }

    private HttpEntity<IdIncrementUpdateDto> createAdminPatchRequest(Integer batch, Long increment){
        HttpHeaders headers = getHttpAdminsHeaders();
        IdIncrementUpdateDto body = new IdIncrementUpdateDto(batch, increment);

        return new HttpEntity<>(body,headers);
    }
}
