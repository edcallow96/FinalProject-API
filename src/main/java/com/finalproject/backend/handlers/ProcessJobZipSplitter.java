package com.finalproject.backend.handlers;

import com.finalproject.backend.model.ProcessJob;
import lombok.SneakyThrows;
import org.apache.camel.Exchange;
import org.apache.camel.dataformat.zipfile.ZipIterator;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class ProcessJobZipSplitter extends ZipSplitter {

  @SneakyThrows
  @Override
  public Object evaluate(Exchange exchange) {
    InputStream inputStream = new FileInputStream(exchange.getIn().getBody(ProcessJob.class).getPayloadLocation());
    return new ZipIterator(exchange, inputStream);
  }
}
