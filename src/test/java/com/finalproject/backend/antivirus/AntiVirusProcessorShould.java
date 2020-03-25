package com.finalproject.backend.antivirus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
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

import static com.finalproject.backend.model.ProcessName.ANTI_VIRUS_SCAN;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static com.github.tomakehurst.wiremock.client.CountMatchingStrategy.GREATER_THAN;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

public class AntiVirusProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static WireMockServer metaDefenderStub;

  @Mock
  private ApplicationProperties applicationProperties;

  @InjectMocks
  private AntiVirusProcessor antiVirusProcessor;

  private Exchange exchange;

  private String dataId;

  @BeforeClass
  public static void setUpStub() {
    org.eclipse.jetty.util.log.Log.setLog(null);
    metaDefenderStub = new WireMockServer(new WireMockConfiguration().stubRequestLoggingDisabled(true));
    metaDefenderStub.start();
  }

  @Before
  public void setUp() throws IOException {
    exchange = ExchangeBuilder.anExchange(
        new DefaultCamelContext()).withBody(
        ProcessJob.builder().payloadLocation(temporaryFolder.newFile(randomAlphabetic(10))).build()).build();

    when(applicationProperties.getMetaDefenderEndpoint()).thenReturn(URI.create(format("%s/file", metaDefenderStub.baseUrl())));
    when(applicationProperties.getMetaDefenderApiKey()).thenReturn(randomAlphabetic(10));
    when(applicationProperties.getMetaDefenderPollingTimeout()).thenReturn(1000);
    when(applicationProperties.getMetaDefenderPollingDelay()).thenReturn(100);
    dataId = randomAlphabetic(10);
    stubUploadFileResponse();
    stubScanResultResponse(buildMetaDefenderJsonNodeWithScanProgress(100, 0, ""));
  }

  @AfterClass
  public static void tearDownStub() {
    metaDefenderStub.stop();
  }

  @Test
  public void uploadFileForScan() {
    antiVirusProcessor.process(exchange);

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    WireMock.verify(postRequestedFor(urlEqualTo("/file"))
        .withHeader("apiKey", WireMock.equalTo(applicationProperties.getMetaDefenderApiKey()))
        .withHeader("fileName", WireMock.equalTo(processJob.getPayloadLocation().getName()))
        .withHeader("content-type", WireMock.equalTo(APPLICATION_OCTET_STREAM)));
  }

  @Test
  public void pollForResultsUntilComplete() {
    stubScanResultResponse(buildMetaDefenderJsonNodeWithScanProgress(0, 0, ""));

    antiVirusProcessor.process(exchange);

    WireMock.verify(new CountMatchingStrategy(GREATER_THAN, 1), getRequestedFor(urlEqualTo(format("/file/%s", dataId)))
        .withHeader("apiKey", WireMock.equalTo(applicationProperties.getMetaDefenderApiKey())));
  }

  @Test
  public void succeedProcessWhenNoThreatHasBeenDetected() {
    antiVirusProcessor.process(exchange);

    List<ProcessResult> results = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getProcessName(), Matchers.equalTo(ANTI_VIRUS_SCAN));
    assertThat(results.get(0).getProcessStatus(), Matchers.equalTo(SUCCESS));
  }

  @Test
  public void failProcessWhenThreatHasBeenDetected() {
    String threatFound = randomAlphabetic(10);
    stubScanResultResponse(buildMetaDefenderJsonNodeWithScanProgress(100, 1, threatFound));
    antiVirusProcessor.process(exchange);

    List<ProcessResult> results = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getProcessName(), Matchers.equalTo(ANTI_VIRUS_SCAN));
    assertThat(results.get(0).getProcessStatus(), Matchers.equalTo(FAILED));
    assertThat(results.get(0).getFailureReason(), Matchers.containsString(threatFound));
  }

  @Test
  public void setFoundThreatToUnknownWhenNoEngineProvidesIt() {
    stubScanResultResponse(buildMetaDefenderJsonNodeWithScanProgress(100, 1, ""));
    antiVirusProcessor.process(exchange);

    List<ProcessResult> results = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getProcessName(), Matchers.equalTo(ANTI_VIRUS_SCAN));
    assertThat(results.get(0).getProcessStatus(), Matchers.equalTo(FAILED));
    assertThat(results.get(0).getFailureReason(), Matchers.containsString("Unknown"));
  }

  @Test
  public void failProcessingWhenPollingTimesOut() {
    stubScanResultResponse(buildMetaDefenderJsonNodeWithScanProgress(99, 0, ""));
    antiVirusProcessor.process(exchange);

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    assertThat(processJob.getProcessingResults(), hasSize(1));
    assertThat(processJob.getProcessingResults().get(0).getProcessName(), Matchers.equalTo(ANTI_VIRUS_SCAN));
    assertThat(processJob.getProcessingResults().get(0).getProcessStatus(), Matchers.equalTo(FAILED));
    assertThat(processJob.getProcessingResults().get(0).getFailureReason(), Matchers.equalTo("AV scan was timed out before it completed."));
  }

  @Test
  public void uploadFileWithUnArchivingRule() {
    antiVirusProcessor.process(exchange);

    WireMock.verify(postRequestedFor(urlEqualTo("/file"))
        .withHeader("rule", WireMock.equalTo("multiscan,unarchive")));
  }

  private void stubUploadFileResponse() {
    stubFor(
        WireMock.post(urlPathMatching("/file"))
            .willReturn(
                aResponse()
                    .withBody(format("{ \"data_id\": \"%s\" }", dataId))
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Connection", "close")));
  }

  private void stubScanResultResponse(JsonNode response) {
    stubFor(
        WireMock.get(urlPathMatching(format("/file/%s", dataId)))
            .willReturn(
                aResponse()
                    .withBody(response.toPrettyString())
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Connection", "close")));
  }

  private JsonNode buildMetaDefenderJsonNodeWithScanProgress(int scanProgress, int scanResult, String threatFound) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode metaDefenderResultNode = objectMapper.createObjectNode();
    ObjectNode processInfoNode = objectMapper.createObjectNode();
    ObjectNode scanResultsNode = objectMapper.createObjectNode();
    ObjectNode scanDetailsNode = objectMapper.createObjectNode();
    scanDetailsNode.set("ClamAv", objectMapper.createObjectNode().put("threat_found", threatFound));
    scanResultsNode.set("scan_details", scanDetailsNode);
    processInfoNode.put("progress_percentage", scanProgress);
    scanResultsNode.put("scan_all_result_i", scanResult);
    metaDefenderResultNode.put("data_id", dataId);
    metaDefenderResultNode.set("process_info", processInfoNode);
    metaDefenderResultNode.set("scan_results", scanResultsNode);
    return metaDefenderResultNode;
  }

}