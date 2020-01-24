package com.finalproject.backend;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.function.Function;

import static org.apache.commons.codec.CharEncoding.UTF_8;

@Slf4j
@Component("awsLambdaS3Function")
public class HelloWorld implements Function<S3Event, S3Event> {

    @Autowired
    private AmazonS3 amazonS3;

    @Override
    public S3Event apply(S3Event s3Event) {

        log.info("S3 Event processing starts with record: {}", s3Event.toJson());

        // For each record.
        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {

            String s3Key = record.getS3().getObject().getKey();
            String s3Bucket = record.getS3().getBucket().getName();

            log.info("Received record with bucket: {}  and key:  {}", s3Bucket ,s3Key);

            try {
                S3Object object = amazonS3.getObject(s3Bucket, URLDecoder.decode(s3Key, UTF_8));
                log.info(StreamUtils.copyToString(object.getObjectContent(),  Charsets.UTF_8));
                log.info("Retrieved s3 object: {} ", object);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return s3Event;
    }
}