package com.finalproject.backend.antivirus;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.tika.mime.MediaType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@Ignore //TODO Mock DeepSecure endpoint
public class AntiVirusProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @InjectMocks
  private AntiVirusProcessor antiVirusProcessor;

  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    File tempFile = new File(temporaryFolder.getRoot(), randomAlphabetic(10));
    Files.copy(Paths.get("src/test/resources/march.PNG"), tempFile.toPath());
    ProcessJob processJob = ProcessJob.builder().contentType(MediaType.parse("image/png")).payloadLocation(tempFile).build();
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(processJob).build();
  }

  @Test
  public void testAv() {
    antiVirusProcessor.process(exchange);
  }

}