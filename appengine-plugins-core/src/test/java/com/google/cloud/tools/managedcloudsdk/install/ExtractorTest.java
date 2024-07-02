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

import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExtractorTest {

  @TempDir
  public File tmp;
  @Mock private ProgressListener mockProgressListener;
  @Mock private ExtractorProvider mockExtractorProvider;

  @Test
  public void testExtract_success() throws Exception {
    final Path extractionDestination = newFolder(tmp, "target").toPath();
    Path extractionSource = File.createTempFile("fake.archive", null, tmp).toPath();

    Mockito.doAnswer(
            invocation -> {
              Files.createDirectory(extractionDestination.resolve("some-dir"));
              Files.createFile(extractionDestination.resolve("some-file"));
              return null;
            })
        .when(mockExtractorProvider)
        .extract(extractionSource, extractionDestination, mockProgressListener);

    Extractor extractor =
        new Extractor(
            extractionSource, extractionDestination, mockExtractorProvider, mockProgressListener);

    extractor.extract();

    Assertions.assertTrue(Files.exists(extractionDestination));
    Mockito.verify(mockExtractorProvider)
        .extract(extractionSource, extractionDestination, mockProgressListener);
  }

  @Test
  public void testExtract_cleanupAfterException() throws Exception {
    final Path extractionDestination = newFolder(tmp, "target").toPath();
    Path extractionSource = File.createTempFile("fake.archive", null, tmp).toPath();

    Mockito.doAnswer(
            invocation -> {
              // pretend to extract by creating the expected final directory (for success!)
              Files.createDirectory(extractionDestination.resolve("some-dir"));
              Files.createFile(extractionDestination.resolve("some-file"));
              throw new IOException("Failed during extraction");
            })
        .when(mockExtractorProvider)
        .extract(extractionSource, extractionDestination, mockProgressListener);

    Extractor extractor =
        new Extractor(
            extractionSource, extractionDestination, mockExtractorProvider, mockProgressListener);

    try {
      extractor.extract();
      Assertions.fail("IOException expected but thrown - test infrastructure failure");
    } catch (IOException ex) {
      // ensure we are rethrowing after cleanup
      Assertions.assertEquals("Failed during extraction", ex.getMessage());
    }

    Assertions.assertFalse(Files.exists(extractionDestination));
    Mockito.verify(mockExtractorProvider)
        .extract(extractionSource, extractionDestination, mockProgressListener);
  }

  private static File newFolder(File root, String... subDirs) throws IOException {
    String subFolder = String.join("/", subDirs);
    File result = new File(root, subFolder);
    if (!result.mkdirs()) {
      throw new IOException("Couldn't create folders " + root);
    }
    return result;
  }
}
