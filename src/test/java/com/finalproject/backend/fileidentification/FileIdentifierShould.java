package com.finalproject.backend.fileidentification;

import org.apache.tika.mime.MediaType;
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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FileIdentifierShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @InjectMocks
  private FileIdentifier fileIdentifier;

  private File testFile;

  @Before
  public void setUp() throws IOException {
    testFile = temporaryFolder.newFile(randomAlphabetic(10) + ".txt");
  }

  @Test
  public void successfulIdentifyFile() throws Exception {
    MediaType contentType = fileIdentifier.identifyFile(testFile);

    assertThat(contentType, equalTo(MediaType.TEXT_PLAIN));
  }

  @Test(expected = FileIdentificationException.class)
  public void throwExceptionWhenFileExtensionIsSpoofed() throws Exception {
    testFile = temporaryFolder.newFile(randomAlphabetic(10) + "." + randomAlphabetic(3));

    fileIdentifier.identifyFile(testFile);
  }

  @Test(expected = FileIdentificationException.class)
  public void throwExceptionWhenFileIdentificationFails() throws FileIdentificationException {
    testFile.delete();

    fileIdentifier.identifyFile(testFile);
  }

}