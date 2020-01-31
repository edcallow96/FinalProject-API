package com.finalproject.backend.handlers;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLDecoder;
import java.util.function.Function;

import static com.finalproject.backend.constants.BackendApplicationConstants.ENTRY_POINT_ROUTE;
import static org.apache.commons.codec.CharEncoding.UTF_8;

@Slf4j
@Component("lambdaFunctionHandler")
public class FinalProjectLambdaFunction implements Function<S3Event, S3Event> {

  private AmazonS3 amazonS3;
  private ProducerTemplate producerTemplate;
  private CamelContext camelContext;
  private ApplicationProperties applicationProperties;

  public FinalProjectLambdaFunction(
      final AmazonS3 amazonS3,
      final ProducerTemplate producerTemplate,
      final CamelContext camelContext,
      final ApplicationProperties applicationProperties) {
    this.amazonS3 = amazonS3;
    this.producerTemplate = producerTemplate;
    this.camelContext = camelContext;
    this.applicationProperties = applicationProperties;
  }

  @Override
  public S3Event apply(S3Event s3Event) {

    log.info("S3 Event processing starts with record: {}", s3Event.toJson());

    // For each record.
    for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {

      String s3Key = record.getS3().getObject().getKey();
      String s3Bucket = record.getS3().getBucket().getName();

      log.info("Received record with bucket: {}  and key:  {}", s3Bucket, s3Key);

      try {
        //TODO - refactor download into separate component
        S3Object s3Object = amazonS3.getObject(s3Bucket, URLDecoder.decode(s3Key, UTF_8));
        File downloadedFile = applicationProperties.getDownloadDirectory().resolve(s3Object.getKey()).toFile();
        FileUtils.copyInputStreamToFile(s3Object.getObjectContent(), downloadedFile);
        producerTemplate.send(ENTRY_POINT_ROUTE, ExchangeBuilder.anExchange(camelContext).withBody(downloadedFile).build());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return s3Event;
  }
}
