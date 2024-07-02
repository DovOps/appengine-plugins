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

package com.google.cloud.tools.appengine.operations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.configuration.AppYamlProjectStageConfiguration;
import com.google.cloud.tools.test.utils.LogStoringHandler;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Test the {@link AppYamlProjectStaging} functionality. */
@ExtendWith(MockitoExtension.class)
public class AppYamlProjectStagingTest {

  @TempDir
  public File temporaryFolder;

  private AppYamlProjectStageConfiguration config;
  @Mock private AppYamlProjectStaging.CopyService copyService;

  private LogStoringHandler handler;
  private Path stagingDirectory;
  private Path dockerDirectory;
  private List<Path> extraFilesDirectories;
  private Path appEngineDirectory;
  private Path dockerFile;
  private Path artifact;

  @BeforeEach
  public void setUp() throws IOException {
    handler = LogStoringHandler.getForLogger(AppYamlProjectStaging.class.getName());
    appEngineDirectory = newFolder(temporaryFolder, "junit").toPath();
    dockerDirectory = newFolder(temporaryFolder, "junit").toPath();
    extraFilesDirectories =
        ImmutableList.of(
            newFolder(temporaryFolder, "junit").toPath(), newFolder(temporaryFolder, "junit").toPath());
    stagingDirectory = newFolder(temporaryFolder, "junit").toPath();
    artifact = File.createTempFile("artifact.jar", null, temporaryFolder).toPath();

    dockerFile = dockerDirectory.resolve("Dockerfile");
    Files.createFile(dockerFile);

    config =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .dockerDirectory(dockerDirectory)
            .extraFilesDirectories(extraFilesDirectories)
            .build();
  }

  @Test
  public void testStageArchive_flexPath() throws IOException, AppEngineException {
    Files.write(
        appEngineDirectory.resolve("app.yaml"),
        "env: flex\nruntime: test_runtime\n".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    // mock to watch internal calls
    AppYamlProjectStaging mock = mock(AppYamlProjectStaging.class);
    doCallRealMethod().when(mock).stageArchive(config);

    mock.stageArchive(config);
    verify(mock).stageFlexibleArchive(config, "test_runtime");
  }

  @Test
  public void testStageArchive_java11StandardPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardPath("java11");
  }

  @Test
  public void testStageArchive_java17StandardPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardPath("java17");
  }

  @Test
  public void testStageArchive_java21StandardPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardPath("java21");
  }

  private void stageArchive_gen2StandardPath(String runtime)
      throws IOException, AppEngineException {
    Files.write(
        appEngineDirectory.resolve("app.yaml"),
        ("runtime: " + runtime + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    // mock to watch internal calls
    AppYamlProjectStaging mock = mock(AppYamlProjectStaging.class);
    doCallRealMethod().when(mock).stageArchive(config);

    mock.stageArchive(config);
    verify(mock).stageStandardArchive(config);
  }

  @Test
  public void testStageArchive_java11StandardBinaryPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardBinaryPath("java11");
  }

  @Test
  public void testStageArchive_java17StandardBinaryPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardBinaryPath("java17");
  }

  @Test
  public void testStageArchive_java21StandardBinaryPath() throws IOException, AppEngineException {
    stageArchive_gen2StandardBinaryPath("java21");
  }

  private void stageArchive_gen2StandardBinaryPath(String runtime)
      throws IOException, AppEngineException {
    config =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(File.createTempFile("myscript.sh", null, temporaryFolder).toPath())
            .stagingDirectory(stagingDirectory)
            .extraFilesDirectories(extraFilesDirectories)
            .build();

    Files.write(
        appEngineDirectory.resolve("app.yaml"),
        ("runtime: " + runtime + "\nentrypoint: anything\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    // mock to watch internal calls
    AppYamlProjectStaging mock = mock(AppYamlProjectStaging.class);
    doCallRealMethod().when(mock).stageArchive(config);

    mock.stageArchive(config);
    verify(mock).stageStandardBinary(config);
  }

  @Test
  public void testStageArchive_java11BinaryWithoutEntrypoint() throws IOException {
    stageArchive_gen2BinaryWithoutEntrypoint("java11");
  }

  @Test
  public void testStageArchive_java17BinaryWithoutEntrypoint() throws IOException {
    stageArchive_gen2BinaryWithoutEntrypoint("java17");
  }

  @Test
  public void testStageArchive_java21BinaryWithoutEntrypoint() throws IOException {
    stageArchive_gen2BinaryWithoutEntrypoint("java21");
  }

  private void stageArchive_gen2BinaryWithoutEntrypoint(String runtime) throws IOException {
    Path nonJarArtifact = File.createTempFile("myscript.sh", null, temporaryFolder).toPath();
    config =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(nonJarArtifact)
            .stagingDirectory(stagingDirectory)
            .extraFilesDirectories(extraFilesDirectories)
            .build();

    Files.write(
        appEngineDirectory.resolve("app.yaml"),
        ("runtime: " + runtime + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    AppYamlProjectStaging testStaging = new AppYamlProjectStaging();

    try {
      testStaging.stageArchive(config);
      fail();
    } catch (AppEngineException ex) {
      assertEquals(
          "Cannot process application with runtime: "
              + runtime
              + ". A custom entrypoint must be defined in your app.yaml for non-jar artifact: "
              + config.getArtifact().toString(),
          ex.getMessage());
    }
  }

  @Test
  public void testStageArchive_unknown() throws IOException {
    Files.write(
        appEngineDirectory.resolve("app.yaml"),
        "runtime: moose\n".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    AppYamlProjectStaging testStaging = new AppYamlProjectStaging();

    try {
      testStaging.stageArchive(config);
      fail();
    } catch (AppEngineException ex) {
      assertEquals("Cannot process application with runtime: moose", ex.getMessage());
    }
  }

  @Test
  public void testCopyDockerContext_runtimeJavaNoWarning() throws AppEngineException, IOException {
    dockerDirectory = temporaryFolder.toPath().resolve("hopefully-made-up-dir");
    AppYamlProjectStageConfiguration invalidDockerDirConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .dockerDirectory(dockerDirectory)
            .build();
    assertFalse(Files.exists(dockerDirectory));

    AppYamlProjectStaging.copyDockerContext(invalidDockerDirConfig, copyService, "java");

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());

    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyDockerContext_noDocker() throws AppEngineException, IOException {
    AppYamlProjectStageConfiguration noDockerDirConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .build();
    AppYamlProjectStaging.copyDockerContext(noDockerDirConfig, copyService, "java");

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());

    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyDockerContext_runtimeJavaWithWarning()
      throws AppEngineException, IOException {

    AppYamlProjectStaging.copyDockerContext(config, copyService, "java");

    List<LogRecord> logs = handler.getLogs();
    assertEquals(1, logs.size());
    assertEquals(
        logs.getFirst().getMessage(),
        "WARNING: runtime 'java' detected, any docker configuration in "
            + config.getDockerDirectory()
            + " will be ignored. If you wish to specify "
            + "a docker configuration, please use 'runtime: custom'.");

    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyDockerContext_runtimeNotJavaNoDockerfile() throws IOException {

    Files.delete(dockerFile);
    try {
      AppYamlProjectStaging.copyDockerContext(config, copyService, "custom");
      fail();
    } catch (AppEngineException ex) {
      assertEquals(
          "Docker directory " + config.getDockerDirectory() + " does not contain Dockerfile.",
          ex.getMessage());
    }

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());

    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyDockerContext_runtimeNotJavaWithDockerfile()
      throws AppEngineException, IOException {
    AppYamlProjectStaging.copyDockerContext(config, copyService, "custom");

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());
    verify(copyService).copyDirectory(dockerDirectory, stagingDirectory);
  }

  @Test
  public void testCopyDockerContext_runtimeNullNoDockerfile() throws IOException {

    Files.delete(dockerFile);
    try {
      AppYamlProjectStaging.copyDockerContext(config, copyService, null);
      fail();
    } catch (AppEngineException ex) {
      assertEquals(
          "Docker directory " + config.getDockerDirectory() + " does not contain Dockerfile.",
          ex.getMessage());
    }

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());

    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyDockerContext_runtimeNull() throws AppEngineException, IOException {

    AppYamlProjectStaging.copyDockerContext(config, copyService, null);

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());

    verify(copyService).copyDirectory(dockerDirectory, stagingDirectory);
  }

  @Test
  public void testCopyExtraFiles_nullConfig() throws AppEngineException, IOException {
    AppYamlProjectStageConfiguration nullExtraFilesConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .extraFilesDirectories(null)
            .build();

    AppYamlProjectStaging.copyExtraFiles(nullExtraFilesConfig, copyService);
    verifyNoMoreInteractions(copyService);
  }

  @Test
  public void testCopyExtraFiles_nonExistantDirectory() throws IOException {
    Path extraFilesDirectory = temporaryFolder.toPath().resolve("non-existant-directory");
    assertFalse(Files.exists(extraFilesDirectory));

    AppYamlProjectStageConfiguration badExtraFilesConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .extraFilesDirectories(ImmutableList.of(extraFilesDirectory))
            .build();

    try {
      AppYamlProjectStaging.copyExtraFiles(badExtraFilesConfig, copyService);
      fail();
    } catch (AppEngineException ex) {
      assertEquals(
          "Extra files directory does not exist. Location: " + extraFilesDirectory,
          ex.getMessage());
    }
  }

  @Test
  public void testCopyExtraFiles_directoryIsActuallyAFile() throws IOException {
    Path extraFilesDirectory = File.createTempFile("junit", null, temporaryFolder).toPath();
    assertTrue(Files.isRegularFile(extraFilesDirectory));

    AppYamlProjectStageConfiguration badExtraFilesConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .extraFilesDirectories(ImmutableList.of(extraFilesDirectory))
            .build();

    try {
      AppYamlProjectStaging.copyExtraFiles(badExtraFilesConfig, copyService);
      fail();
    } catch (AppEngineException ex) {
      assertEquals(
          "Extra files location is not a directory. Location: " + extraFilesDirectory,
          ex.getMessage());
    }
  }

  @Test
  public void testCopyExtraFiles_doCopy() throws IOException, AppEngineException {
    AppYamlProjectStaging.copyExtraFiles(config, copyService);
    verify(copyService).copyDirectory(extraFilesDirectories.getFirst(), stagingDirectory);
    verify(copyService).copyDirectory(extraFilesDirectories.get(1), stagingDirectory);
    verifyNoMoreInteractions(copyService);
  }

  @Test
  public void testCopyAppEngineContext_nonExistentAppEngineDirectory() throws IOException {
    appEngineDirectory = temporaryFolder.toPath().resolve("non-existent-directory");
    assertFalse(Files.exists(appEngineDirectory));

    AppYamlProjectStageConfiguration noAppYamlConfig =
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(artifact)
            .stagingDirectory(stagingDirectory)
            .build();
    try {
      AppYamlProjectStaging.copyAppEngineContext(noAppYamlConfig, copyService);
      fail();
    } catch (AppEngineException ex) {
      assertEquals("app.yaml not found in the App Engine directory.", ex.getMessage());
    }
    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());
    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyAppEngineContext_emptyAppEngineDirectory() throws IOException {
    try {
      AppYamlProjectStaging.copyAppEngineContext(config, copyService);
      fail();
    } catch (AppEngineException ex) {
      assertEquals("app.yaml not found in the App Engine directory.", ex.getMessage());
    }
    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());
    verifyNoInteractions(copyService);
  }

  @Test
  public void testCopyAppEngineContext_appYamlInAppEngineDirectory()
      throws AppEngineException, IOException {
    Path file = appEngineDirectory.resolve("app.yaml");
    Files.createFile(file);

    AppYamlProjectStaging.copyAppEngineContext(config, copyService);

    List<LogRecord> logs = handler.getLogs();
    assertEquals(0, logs.size());
    verify(copyService)
        .copyFileAndReplace(
            appEngineDirectory.resolve("app.yaml"), stagingDirectory.resolve("app.yaml"));
  }

  @Test
  public void testFindRuntime_malformedAppYaml() throws IOException {

    Path file = appEngineDirectory.resolve("app.yaml");
    Files.write(
        file,
        ": m a l f o r m e d !".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    try {
      AppYamlProjectStaging.findRuntime(config);
      fail();
    } catch (AppEngineException ex) {
      assertEquals("Malformed 'app.yaml'.", ex.getMessage());
    }
  }

  @Test
  public void testHasCustomEntrypoint_true() throws IOException, AppEngineException {
    Path file = appEngineDirectory.resolve("app.yaml");
    Files.write(
        file,
        "entrypoint: custom custom".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW);

    assertTrue(AppYamlProjectStaging.hasCustomEntrypoint(config));
  }

  @Test
  public void testHasCustomEntrypoint_false() throws IOException, AppEngineException {
    Path file = appEngineDirectory.resolve("app.yaml");
    Files.write(
        file, "runtime: java".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    Assertions.assertFalse(AppYamlProjectStaging.hasCustomEntrypoint(config));
  }

  @Test
  public void testCopyArtifactJarClasspath_noClasspath() throws IOException {
    AppYamlProjectStaging.copyArtifactJarClasspath(
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(Paths.get("src/test/resources/jars/libs/simpleLib.jar"))
            .stagingDirectory(stagingDirectory)
            .build(),
        copyService);

    verifyNoInteractions(copyService);

    assertEquals(0, handler.getLogs().size());
  }

  @Test
  public void testCopyArtifactJarClasspath_withClasspathEntries() throws IOException {
    AppYamlProjectStaging.copyArtifactJarClasspath(
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(Paths.get("src/test/resources/jars/complexLib.jar"))
            .stagingDirectory(stagingDirectory)
            .build(),
        copyService);

    verify(copyService)
        .copyFileAndReplace(
            Paths.get("src/test/resources/jars/libs/simpleLib.jar"),
            stagingDirectory.resolve("libs/simpleLib.jar"));
    verifyNoMoreInteractions(copyService);

    assertEquals(0, handler.getLogs().size());
  }

  @Test
  public void testCopyArtifactJarClasspath_withMissingClasspathEntries() throws IOException {
    AppYamlProjectStaging.copyArtifactJarClasspath(
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(Paths.get("src/test/resources/jars/complexLibMissingEntryManifest.jar"))
            .stagingDirectory(stagingDirectory)
            .build(),
        copyService);

    verify(copyService)
        .copyFileAndReplace(
            Paths.get("src/test/resources/jars/libs/simpleLib.jar"),
            stagingDirectory.resolve("libs/simpleLib.jar"));
    verifyNoMoreInteractions(copyService);

    // check for warning about missing jars
    List<LogRecord> logs = handler.getLogs();
    assertEquals(1, logs.size());
    assertEquals(Level.WARNING, logs.getFirst().getLevel());
    assertEquals(
        "Could not copy 'Class-Path' jar: "
            + Paths.get("src/test/resources/jars/libs/missing.jar")
            + " referenced in MANIFEST.MF",
        logs.getFirst().getMessage());
  }

  @Test
  public void testCopyArtifactJarClasspath_targetAlreadyExists() throws IOException {

    Path simpleLib = Paths.get("src/test/resources/jars/libs/simpleLib.jar");
    Path simpleLibTarget = stagingDirectory.resolve("libs/simpleLib.jar");
    Files.createDirectories(simpleLibTarget.getParent());
    Files.createFile(simpleLibTarget);

    AppYamlProjectStaging.copyArtifactJarClasspath(
        AppYamlProjectStageConfiguration.builder()
            .appEngineDirectory(appEngineDirectory)
            .artifact(Paths.get("src/test/resources/jars/complexLib.jar"))
            .stagingDirectory(stagingDirectory)
            .build(),
        copyService);

    verify(copyService).copyFileAndReplace(simpleLib, simpleLibTarget);
    verifyNoMoreInteractions(copyService);

    // check for warning about overwriting jars
    List<LogRecord> logs = handler.getLogs();
    assertEquals(1, logs.size());
    assertEquals(Level.FINE, logs.getFirst().getLevel());
    assertEquals(
        "Overwriting 'Class-Path' jar: "
            + simpleLibTarget
            + " with "
            + simpleLib
            + " referenced in MANIFEST.MF",
        logs.getFirst().getMessage());
  }

  @Test
  public void testCopyService_copiesToExistingFile() throws IOException {
    AppYamlProjectStaging.CopyService copier = new AppYamlProjectStaging.CopyService();
    Path root = temporaryFolder.toPath();

    Path srcDir = root.resolve("srcDir");
    Files.createDirectory(srcDir);
    Path srcFile = srcDir.resolve("srcFile");
    Files.write(
        srcFile, "some content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    Path destDir = root.resolve("destDir");
    Files.createDirectory(destDir);
    Path destFile = destDir.resolve("destFile");
    Files.createFile(destFile);

    assertTrue(Files.exists(destDir));
    assertTrue(Files.exists(destFile));

    copier.copyFileAndReplace(srcFile, destFile);

    assertArrayEquals(Files.readAllBytes(srcFile), Files.readAllBytes(destFile));
  }

  @Test
  public void testCopyService_createsParentsWhenNecessary() throws IOException {
    AppYamlProjectStaging.CopyService copier = new AppYamlProjectStaging.CopyService();
    Path root = temporaryFolder.toPath();

    Path srcDir = root.resolve("srcDir");
    Files.createDirectory(srcDir);
    Path srcFile = srcDir.resolve("srcFile");
    Files.write(
        srcFile, "some content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    Path destDir = root.resolve("destDir");
    Path destFile = destDir.resolve("destFile");

    Assertions.assertFalse(Files.exists(destDir));
    Assertions.assertFalse(Files.exists(destFile));

    copier.copyFileAndReplace(srcFile, destFile);

    assertArrayEquals(Files.readAllBytes(srcFile), Files.readAllBytes(destFile));
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
