package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.User;
import net.lingala.zip4j.ZipFile;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;

public class UnZipProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @InjectMocks
  private UnZipProcessor unZipProcessor;

  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(
        ProcessJob.builder()
            .payloadLocation(createZipFile(2))
            .originalFileSize(new Random().nextLong())
            .originalFileHash(randomAlphabetic(10))
            .user(User.builder().build())
            .jobId(randomAlphabetic(10))
            .sourceBucket(randomAlphabetic(10))
            .sourceKey(randomAlphabetic(10))
            .build()
    ).build();
  }

  @Test
  public void extractFilesToDirectory() {
    Path zipFileLocation = exchange.getIn().getBody(ProcessJob.class).getPayloadLocation().toPath();

    unZipProcessor.process(exchange);

    Path extractedDirectory = zipFileLocation.getParent().resolve(format("%s_extracted", zipFileLocation.getFileName()));

    assertThat(extractedDirectory.toFile(), anExistingDirectory());
    assertThat(extractedDirectory.toFile().listFiles(), arrayWithSize(2));
  }

  @Test
  public void setBodyToListOfExtractedFiles() {
    ProcessJob originalProcessJob = exchange.getIn().getBody(ProcessJob.class);

    unZipProcessor.process(exchange);

    List<ProcessJob> extractedFiles = exchange.getIn().getBody(List.class);

    assertThat(extractedFiles, hasSize(2));
    extractedFiles.forEach(extractedFile -> {
      assertThat(extractedFile.getPayloadLocation(), notNullValue());
      assertThat(extractedFile.getProcessingResults(), empty());
      assertThat(extractedFile.getJobId(), equalTo(originalProcessJob.getJobId()));
      assertThat(extractedFile.getUser(), equalTo(originalProcessJob.getUser()));
      assertThat(extractedFile.getOriginalFileHash(), equalTo(originalProcessJob.getOriginalFileHash()));
      assertThat(extractedFile.getOriginalFileSize(), equalTo(originalProcessJob.getOriginalFileSize()));
      assertThat(extractedFile.getContentType(), nullValue());
      assertThat(extractedFile.getSourceKey(), equalTo(originalProcessJob.getSourceKey()));
      assertThat(extractedFile.getSourceBucket(), equalTo(originalProcessJob.getSourceBucket()));
    });
  }

  private File createZipFile(int zippedFiles) throws IOException {
    ZipFile zipFile = new ZipFile(temporaryFolder.getRoot().toPath().resolve(randomAlphabetic(10)).toFile());
    for (int i = 0; i < zippedFiles; i++) {
      zipFile.addFile(temporaryFolder.newFile(randomAlphabetic(10)));
    }
    return zipFile.getFile();
  }

}