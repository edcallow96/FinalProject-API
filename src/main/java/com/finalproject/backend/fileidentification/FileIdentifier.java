package com.finalproject.backend.fileidentification;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FileIdentifier {

  private Tika tika;

  public FileIdentifier() throws Exception {
    tika = new Tika(new TikaConfig(this.getClass().getResourceAsStream("/tikaConfig.xml")));
  }

  public MediaType identifyFile(InputStream inputStream) throws IOException {
    return MediaType.parse(this.tika.detect(inputStream));
  }
}
