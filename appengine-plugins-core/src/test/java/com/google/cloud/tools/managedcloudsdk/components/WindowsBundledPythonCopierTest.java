/*
 * Copyright 2018 Google LLC.
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

package com.google.cloud.tools.managedcloudsdk.components;

import com.google.cloud.tools.managedcloudsdk.command.CommandCaller;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WindowsBundledPythonCopierTest {

  @TempDir
  public File temporaryFolder;

  @Mock private CommandCaller mockCommandCaller;
  private Path fakeGcloud;

  @BeforeEach
  public void setUp() throws InterruptedException, CommandExitException, CommandExecutionException {
    fakeGcloud = Paths.get("my/path/to/fake-gcloud");
    Mockito.when(
            mockCommandCaller.call(
                Arrays.asList(fakeGcloud.toString(), "components", "copy-bundled-python"),
                null,
                null))
        .thenReturn("copied-python");
  }

  @Test
  public void testCopyPython()
      throws InterruptedException, CommandExitException, CommandExecutionException {
    WindowsBundledPythonCopier testCopier =
        new WindowsBundledPythonCopier(fakeGcloud, mockCommandCaller);
    Map<String, String> testEnv = testCopier.copyPython();

    Assertions.assertEquals(ImmutableMap.of("CLOUDSDK_PYTHON", "copied-python"), testEnv);
  }

  @Test
  public void testDeleteCopiedPython() throws IOException {
    Path pythonHome = newFolder(temporaryFolder, "python").toPath();
    Path executable = File.createTempFile("python/python.exe", null, temporaryFolder).toPath();
    Assertions.assertTrue(Files.exists(executable));

    WindowsBundledPythonCopier.deleteCopiedPython(executable.toString());
    Assertions.assertFalse(Files.exists(executable));
    Assertions.assertFalse(Files.exists(pythonHome));
  }

  @Test
  public void testDeleteCopiedPython_caseInsensitivity() throws IOException {
    Path pythonHome = newFolder(temporaryFolder, "python").toPath();
    Path executable = File.createTempFile("python/PyThOn.EXE", null, temporaryFolder).toPath();
    Assertions.assertTrue(Files.exists(executable));
    Assertions.assertTrue(executable.toString().endsWith("PyThOn.EXE"));

    WindowsBundledPythonCopier.deleteCopiedPython(executable.toString());
    Assertions.assertFalse(Files.exists(executable));
    Assertions.assertFalse(Files.exists(pythonHome));
  }

  @Test
  public void testDeleteCopiedPython_unexpectedLocation() throws IOException {
    newFolder(temporaryFolder, "unexpected");
    Path unexpected = File.createTempFile("unexpected/file.ext", null, temporaryFolder).toPath();
    Assertions.assertTrue(Files.exists(unexpected));

    WindowsBundledPythonCopier.deleteCopiedPython(unexpected.toString());
    Assertions.assertTrue(Files.exists(unexpected));
  }

  @Test
  public void testDeleteCopiedPython_nonExistingDirectory() {
    Path executable = temporaryFolder.toPath().resolve("python/python.exe");
    Assertions.assertFalse(Files.exists(executable));

    WindowsBundledPythonCopier.deleteCopiedPython("python/python.exe");
    // Ensure no runtime exception is thrown.
  }

  @Test
  public void testIsUnderTempDirectory_variableTemp() {
    Assertions.assertTrue(
        WindowsBundledPythonCopier.isUnderTempDirectory(
            "/temp/prefix/some/file.ext", ImmutableMap.of("TEMP", "/temp/prefix")));
  }

  @Test
  public void testIsUnderTempDirectory_variableTmp() {
    Assertions.assertTrue(
        WindowsBundledPythonCopier.isUnderTempDirectory(
            "/tmp/prefix/some/file.ext", ImmutableMap.of("TMP", "/tmp/prefix")));
  }

  @Test
  public void testIsUnderTempDirectory_noTempVariables() {
    Assertions.assertFalse(
        WindowsBundledPythonCopier.isUnderTempDirectory(
            "/tmp/prefix/some/file.ext", ImmutableMap.of()));
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
