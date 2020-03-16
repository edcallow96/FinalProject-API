package com.finalproject.backend.fileidentification;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getExtension;

@Component
@Slf4j
public class FileIdentifier {

  private Tika tika;
  private TikaConfig tikaConfig;

  public FileIdentifier() {
    try {
      tikaConfig = new TikaConfig(this.getClass().getResourceAsStream("/tikaConfig.xml"));
      tika = new Tika(tikaConfig);
    } catch (Exception e) {
      log.error("Failed to load Tika Configuration", e);
      tika = new Tika();
    }
  }

  public MediaType identifyFile(File file) throws FileIdentificationException {
    try {
      MediaType contentType = MediaType.parse(this.tika.detect(file));
      checkForFileSpoofing(file, contentType);
      return contentType;
    } catch (IOException e) {
      throw new FileIdentificationException(format("file identification of %s failed.", file), e);
    }
  }

  private void checkForFileSpoofing(File file, MediaType contentType) throws FileIdentificationException {
    try {
      List<String> expectedExtensions = tikaConfig.getMimeRepository().forName(contentType.toString()).getExtensions();
      String fileExtension = "." + getExtension(file.getName());
      if (!expectedExtensions.contains(fileExtension.toLowerCase())) {
        throw new FileIdentificationException(format("file %s has spoofed file extension for type %s", file.getName(), contentType));
      }
    } catch (MimeTypeException e) {
      throw new FileIdentificationException(format("file identification of %s failed.", file), e);
    }
  }
}
