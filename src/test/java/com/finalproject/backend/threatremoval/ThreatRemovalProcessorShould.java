package com.finalproject.backend.threatremoval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessName;
import com.finalproject.backend.model.ProcessResult;
import com.finalproject.backend.model.ProcessStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.FileUtils;
import org.apache.tika.mime.MediaType;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.tika.mime.MimeTypes.OCTET_STREAM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class ThreatRemovalProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationProperties applicationProperties;

  @InjectMocks
  private ThreatRemovalProcessor threatRemovalProcessor;

  private static WireMockServer deepSecureStub;

  private Exchange exchange;

  @BeforeClass
  public static void setUpStub() {
    org.eclipse.jetty.util.log.Log.setLog(null);
    deepSecureStub = new WireMockServer(new WireMockConfiguration().stubRequestLoggingDisabled(true));
    deepSecureStub.start();
  }

  @Before
  public void setUp() throws IOException {
    when(applicationProperties.getDeepSecureEndpoint()).thenReturn(URI.create(deepSecureStub.baseUrl()));
    when(applicationProperties.getDeepSecureApiKey()).thenReturn(randomAlphabetic(10));
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(
        ProcessJob.builder()
            .contentType(MediaType.OCTET_STREAM)
            .payloadLocation(temporaryFolder.newFile(randomAlphabetic(10)))
            .build()
    ).build();
    stubUploadFileResponse(randomAlphabetic(10).getBytes(), 200);
  }

  @AfterClass
  public static void stopStub() {
    deepSecureStub.stop();
  }

  @Test
  public void uploadsFileForSanitisation() {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    threatRemovalProcessor.process(exchange);

    WireMock.verify(postRequestedFor(urlEqualTo("/upload"))
        .withHeader("accept", WireMock.equalTo(format("%s,%s", processJob.getContentType(), APPLICATION_JSON)))
        .withHeader("Content-Type", WireMock.equalTo(processJob.getContentType().toString()))
        .withHeader("x-api-key", WireMock.equalTo(applicationProperties.getDeepSecureApiKey()))
        .withHeader("X-Accept-Preview-Mode-For-Content-Types", WireMock.equalTo(processJob.getContentType().toString())));
  }

  @Test
  public void overWriteFileWithSanitisedContent() throws IOException {
    byte[] sanitisedContent = randomAlphabetic(10).getBytes();
    stubUploadFileResponse(sanitisedContent, 200);

    threatRemovalProcessor.process(exchange);

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    assertThat(processJob.getPayloadLocation(), anExistingFile());
    assertThat(FileUtils.readFileToByteArray(processJob.getPayloadLocation()), Matchers.equalTo(sanitisedContent));
  }

  @Test
  public void addSuccessProcessingResult() {
    threatRemovalProcessor.process(exchange);

    List<ProcessResult> processResults = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(processResults, hasSize(1));
    assertThat(processResults.get(0).getProcessName(), Matchers.equalTo(ProcessName.THREAT_REMOVAL));
    assertThat(processResults.get(0).getProcessStatus(), Matchers.equalTo(ProcessStatus.SUCCESS));
  }

  @Test
  public void addFailedProcessingResultWhenSanitisationFails() {
    deepSecureStub.resetAll();

    threatRemovalProcessor.process(exchange);

    List<ProcessResult> processResults = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(processResults, hasSize(1));
    assertThat(processResults.get(0).getProcessName(), Matchers.equalTo(ProcessName.THREAT_REMOVAL));
    assertThat(processResults.get(0).getProcessStatus(), Matchers.equalTo(ProcessStatus.FAILED));
    assertThat(processResults.get(0).getFailureReason(), containsString("Unexpected failure"));
  }

  @Test
  public void extractFailureReasonFromApiResponseForTooManyRequests() {
    ObjectMapper objectMapper = new ObjectMapper();
    String failureReason = randomAlphabetic(10);
    ObjectNode response = objectMapper.createObjectNode()
        .set("error", objectMapper.createObjectNode().put("message", failureReason));
    stubUploadFileResponse(response.toPrettyString().getBytes(), TOO_MANY_REQUESTS.value());

    threatRemovalProcessor.process(exchange);

    List<ProcessResult> processResults = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(processResults, hasSize(1));
    assertThat(processResults.get(0).getProcessName(), Matchers.equalTo(ProcessName.THREAT_REMOVAL));
    assertThat(processResults.get(0).getProcessStatus(), Matchers.equalTo(ProcessStatus.FAILED));
    assertThat(processResults.get(0).getFailureReason(), containsString(failureReason));
  }

  @Test
  public void extractFailureReasonFromApiResponseForBadRequest() {
    ObjectMapper objectMapper = new ObjectMapper();
    String failureReason = randomAlphabetic(10);
    ObjectNode response = objectMapper.createObjectNode().put("message", failureReason);
    stubUploadFileResponse(response.toPrettyString().getBytes(), BAD_REQUEST.value());

    threatRemovalProcessor.process(exchange);

    List<ProcessResult> processResults = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(processResults, hasSize(1));
    assertThat(processResults.get(0).getProcessName(), Matchers.equalTo(ProcessName.THREAT_REMOVAL));
    assertThat(processResults.get(0).getProcessStatus(), Matchers.equalTo(ProcessStatus.FAILED));
    assertThat(processResults.get(0).getFailureReason(), containsString(failureReason));
  }

  private void stubUploadFileResponse(byte[] response, int status) {
    stubFor(
        WireMock.post(urlPathMatching("/upload"))
            .willReturn(
                aResponse()
                    .withStatus(status)
                    .withBody(response)
                    .withHeader("Content-Type", OCTET_STREAM)
                    .withHeader("Connection", "close")));
  }
}