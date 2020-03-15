package com.finalproject.backend.handlers;

import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.User;
import com.finalproject.backend.userservice.UserRepository;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

import static com.finalproject.backend.constants.BackendApplicationConstants.AMAZON_REQUEST_ID;
import static com.finalproject.backend.constants.BackendApplicationConstants.AMZ_METADATA_USER_ID;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrepareJobProcessorShould {
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationProperties applicationProperties;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private PrepareJobProcessor prepareJobProcessor;

  private S3Object s3Object;
  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    when(applicationProperties.getDownloadDirectory()).thenReturn(temporaryFolder.newFolder(randomAlphabetic(10)).toPath());
    when(userRepository.findById(anyString())).thenReturn(Optional.of(User.builder().build()));
    s3Object = new S3Object();
    s3Object.setKey(randomAlphabetic(10));
    s3Object.setObjectContent(new FileInputStream(temporaryFolder.newFile(randomAlphabetic(10))));
    s3Object.setBucketName(randomAlphabetic(10));
    s3Object.getObjectMetadata().addUserMetadata(AMZ_METADATA_USER_ID, randomAlphabetic(10));

    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(s3Object).withHeader(AMAZON_REQUEST_ID, randomAlphabetic(10)).build();
  }

  @Test
  public void downloadS3ObjectContent() throws Exception {
    prepareJobProcessor.process(exchange);

    String jobId = exchange.getIn().getHeader(AMAZON_REQUEST_ID, String.class);

    File expectedFile = applicationProperties.getDownloadDirectory().resolve(format("%s/%s", jobId, s3Object.getKey())).toFile();

    assertThat(expectedFile, anExistingFile());
  }

  @Test
  public void retrieveUserFromUserRepository() throws Exception {
    prepareJobProcessor.process(exchange);

    String userId = s3Object.getObjectMetadata().getUserMetaDataOf(AMZ_METADATA_USER_ID);

    verify(userRepository).findById(userId);
  }

  @Test
  public void setExchangeBodyToProcessJobBuiltFromS3Object() throws Exception {
    prepareJobProcessor.process(exchange);

    ProcessJob builtProcessJob = exchange.getIn().getBody(ProcessJob.class);
    String jobId = exchange.getIn().getHeader(AMAZON_REQUEST_ID, String.class);
    File expectedFile = applicationProperties.getDownloadDirectory().resolve(format("%s/%s", jobId, s3Object.getKey())).toFile();

    assertThat(builtProcessJob, notNullValue());
    assertThat(builtProcessJob.getSourceBucket(), equalTo(s3Object.getBucketName()));
    assertThat(builtProcessJob.getSourceKey(), equalTo(s3Object.getKey()));
    assertThat(builtProcessJob.getContentType(), nullValue());
    assertThat(builtProcessJob.getOriginalFileSize(), equalTo(0L));
    assertThat(builtProcessJob.getOriginalFileHash(), notNullValue());
    assertThat(builtProcessJob.getUser(), notNullValue());
    assertThat(builtProcessJob.getJobId(), equalTo(jobId));
    assertThat(builtProcessJob.getPayloadLocation(), equalTo(expectedFile));
  }


}