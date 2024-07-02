/*
 * Copyright 2017 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.managedcloudsdk.install;

import static org.hamcrest.MatcherAssert.assertThat;

import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TarGzExtractorProviderTest {

  @TempDir
  public File tmp;
  @Mock private ProgressListener mockProgressListener;

  private final TarGzExtractorProvider tarGzExtractorProvider = new TarGzExtractorProvider();

  @Test
  public void testCall() throws URISyntaxException, IOException {
    Path extractionRoot = tmp.toPath();
    Path testArchive = getResource("genericArchives/test.tar.gz");

    tarGzExtractorProvider.extract(testArchive, extractionRoot, mockProgressListener);

    GenericArchivesVerifier.assertArchiveExtraction(extractionRoot);
    // only check file permissions on non-windows
    if (!System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
      GenericArchivesVerifier.assertFilePermissions(extractionRoot);
    }

    ProgressVerifier.verifyUnknownProgress(
        mockProgressListener, "Extracting archive: " + testArchive.getFileName());
  }

  @Test
  public void testZipSlipVulnerability_windows() throws URISyntaxException {
    Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));

    Path extractionRoot = tmp.toPath();
    Path testArchive = getResource("zipSlipSamples/zip-slip-win.tar.gz");
    try {
      tarGzExtractorProvider.extract(testArchive, extractionRoot, mockProgressListener);
      Assertions.fail("IOException expected");
    } catch (IOException expected) {
      assertThat(
          expected.getMessage(),
          CoreMatchers.startsWith("Blocked unzipping files outside destination: "));
    }
  }

  @Test
  public void testZipSlipVulnerability_unix() throws URISyntaxException {
    Assumptions.assumeTrue(!System.getProperty("os.name").startsWith("Windows"));

    Path extractionRoot = tmp.toPath();
    Path testArchive = getResource("zipSlipSamples/zip-slip.tar.gz");
    try {
      tarGzExtractorProvider.extract(testArchive, extractionRoot, mockProgressListener);
      Assertions.fail("IOException expected");
    } catch (IOException expected) {
      assertThat(
          expected.getMessage(),
          CoreMatchers.startsWith("Blocked unzipping files outside destination: "));
    }
  }

  private Path getResource(String resourcePath) throws URISyntaxException {
    Path resource = Paths.get(getClass().getClassLoader().getResource(resourcePath).toURI());
    Assertions.assertTrue(Files.exists(resource));
    return resource;
  }
}
