/*
 * Copyright 2016 Google LLC.
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

package com.google.cloud.tools.appengine.operations.cloudsdk;

import com.google.cloud.tools.test.utils.LogStoringHandler;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

@SuppressWarnings("NullAway")
public class PathResolverTest {

  @TempDir
  public static File symlinkTestArea;
  @Nullable private static Exception symlinkException = null;

  @TempDir
  public File temporaryFolder;

  private PathResolver resolver = new PathResolver();

  @BeforeAll
  public static void generateSymlinkException() throws IOException {
    Path dest = File.createTempFile("junit", null, symlinkTestArea).toPath();
    Path link =
        symlinkTestArea.toPath().resolve("test-link" + System.currentTimeMillis());
    try {
      Files.createSymbolicLink(link, dest);
    } catch (Exception e) {
      symlinkException = e;
    }
  }

  @Test
  public void testResolve() {
    Assertions.assertNotNull(resolver.getCloudSdkPath(), "Could not locate Cloud SDK");
  }

  @Test
  public void testGetRank() {
    Assertions.assertTrue(resolver.getRank() > 10000);
  }

  @Test
  public void testGetLocationsFromPath() {
    List<String> paths =
        PathResolver.getLocationsFromPath(
            "\\my music & videos" + "google-cloud-sdk" + File.separator + "bin");
    Assertions.assertEquals(1, paths.size());
    Assertions.assertEquals("\\my music & videosgoogle-cloud-sdk", paths.getFirst());
  }

  @Test
  public void testUnquote() {
    String actual = PathResolver.unquote("\"only remove \"\" end quotes\"");
    Assertions.assertEquals("only remove \"\" end quotes", actual);
  }

  @Test
  public void testGetLocationFromLink_valid() throws IOException {
    Assumptions.assumeNoException(symlinkException);
    Path sdkHome = newFolder(temporaryFolder, "junit").toPath().toRealPath();
    Path bin = Files.createDirectory(sdkHome.resolve("bin"));
    Path gcloud = Files.createFile(bin.resolve("gcloud"));
    Files.createSymbolicLink(temporaryFolder.toPath().resolve("gcloud"), gcloud);

    List<String> possiblePaths = new ArrayList<>();
    PathResolver.getLocationsFromLink(possiblePaths, gcloud);

    Assertions.assertEquals(1, possiblePaths.size());
    Assertions.assertEquals(gcloud.getParent().getParent().toString(), possiblePaths.getFirst());
  }

  @Test
  public void testGetLocationFromLink_notValid() throws IOException {
    Assumptions.assumeNoException(symlinkException);
    Path invalidPath = newFolder(temporaryFolder, "junit").toPath();
    Files.createSymbolicLink(temporaryFolder.toPath().resolve("gcloud"), invalidPath);

    List<String> possiblePaths = new ArrayList<>();

    PathResolver.getLocationsFromLink(possiblePaths, invalidPath);

    Assertions.assertEquals(0, possiblePaths.size());
  }

  @Test
  public void testGetLocationFromLink_triggerException() throws IOException {
    Assumptions.assumeNoException(symlinkException);
    LogStoringHandler testHandler = LogStoringHandler.getForLogger(PathResolver.class.getName());

    Path exceptionForcingPath = Mockito.mock(Path.class);
    IOException exception = Mockito.mock(IOException.class);
    Mockito.when(exceptionForcingPath.toRealPath()).thenThrow(exception);

    List<String> possiblePaths = new ArrayList<>();
    PathResolver.getLocationsFromLink(possiblePaths, exceptionForcingPath);

    Assertions.assertEquals(1, testHandler.getLogs().size());
    LogRecord logRecord = testHandler.getLogs().getFirst();

    Assertions.assertEquals(
        "Non-critical exception when searching for cloud-sdk", logRecord.getMessage());
    Assertions.assertEquals(exception, logRecord.getThrown());
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
