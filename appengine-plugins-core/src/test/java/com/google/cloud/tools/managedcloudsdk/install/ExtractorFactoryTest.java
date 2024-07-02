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

import com.google.cloud.tools.managedcloudsdk.NullProgressListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExtractorFactoryTest {

  @TempDir
  public File tmp;

  private NullProgressListener listener = new NullProgressListener();

  @Test
  public void testNewExtractor_isZip() throws IOException, UnknownArchiveTypeException {
    Path archive = File.createTempFile("test-zip.zip", null, tmp).toPath();
    Path dest = File.createTempFile("dest", null, tmp).toPath();
    Extractor testExtractor = new ExtractorFactory().newExtractor(archive, dest, listener);
    Assertions.assertTrue(testExtractor.getExtractorProvider() instanceof ZipExtractorProvider);
  }

  @Test
  public void testNewExtractor_isTarGz() throws IOException, UnknownArchiveTypeException {
    Path archive = File.createTempFile("test-tar-gz.tar.gz", null, tmp).toPath();
    Path dest = File.createTempFile("dest", null, tmp).toPath();
    Extractor testExtractor = new ExtractorFactory().newExtractor(archive, dest, listener);
    Assertions.assertTrue(testExtractor.getExtractorProvider() instanceof TarGzExtractorProvider);
  }

  @Test
  public void testNewExtractor_unknownArchiveType() throws IOException {
    // make sure out check starts from end of filename
    Path archive = File.createTempFile("test-bad.tar.gz.zip.bad", null, tmp).toPath();
    Path dest = File.createTempFile("dest", null, tmp).toPath();

    try {
      new ExtractorFactory().newExtractor(archive, dest, listener);
      Assertions.fail("UnknownArchiveTypeException expected but not thrown");
    } catch (UnknownArchiveTypeException ex) {
      Assertions.assertEquals("Unknown archive: " + archive, ex.getMessage());
    }
  }
}
