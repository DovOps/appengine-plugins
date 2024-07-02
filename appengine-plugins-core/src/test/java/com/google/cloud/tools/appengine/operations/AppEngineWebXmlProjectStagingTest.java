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

package com.google.cloud.tools.appengine.operations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.configuration.AppEngineWebXmlProjectStageConfiguration;
import com.google.cloud.tools.appengine.operations.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.operations.cloudsdk.InvalidJavaSdkException;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AppEngineWebXmlProjectStaging}. */
@ExtendWith(MockitoExtension.class)
public class AppEngineWebXmlProjectStagingTest {

  @TempDir
  public File tmpDir;

  @Mock private AppCfgRunner appCfgRunner;

  private Path source;
  private Path destination;
  private Path dockerfile;
  private AppEngineWebXmlProjectStaging staging;
  private AppEngineWebXmlProjectStageConfiguration.Builder builder;

  @BeforeEach
  public void setUp()
      throws IOException, InvalidJavaSdkException, ProcessHandlerException,
          AppEngineJavaComponentsNotInstalledException {
    source = newFolder(tmpDir, "source").toPath();
    destination = newFolder(tmpDir, "destination").toPath();
    dockerfile = File.createTempFile("dockerfile", null, tmpDir).toPath();

    staging = new AppEngineWebXmlProjectStaging(appCfgRunner);

    builder =
        AppEngineWebXmlProjectStageConfiguration.builder()
            .sourceDirectory(source)
            .stagingDirectory(destination);

    // create an app.yaml in staging output when we run.
    Mockito.doAnswer(
            ignored -> {
              Files.createFile(destination.resolve("app.yaml"));
              return null;
            })
        .when(appCfgRunner)
        .run(Mockito.anyList());
  }

  @Test
  public void testSourceDirectoryRequired() {
    try {
      AppEngineWebXmlProjectStageConfiguration.builder().stagingDirectory(destination).build();
      Assertions.fail("allowed missing source directory");
    } catch (IllegalStateException ex) {
      Assertions.assertEquals("No source directory supplied", ex.getMessage());
    }
  }

  @Test
  public void testStagingDirectoryRequired() {
    try {
      AppEngineWebXmlProjectStageConfiguration.builder().sourceDirectory(destination).build();
      Assertions.fail("allowed missing staging directory");
    } catch (IllegalStateException ex) {
      Assertions.assertEquals("No staging directory supplied", ex.getMessage());
    }
  }

  @Test
  public void testCheckFlags_allFlags() throws Exception {
    builder
        .dockerfile(dockerfile)
        .enableQuickstart(true)
        .disableUpdateCheck(true)
        .enableJarSplitting(true)
        .jarSplittingExcludes("suffix1,suffix2")
        .compileEncoding("UTF8")
        .deleteJsps(true)
        .enableJarClasses(true)
        .disableJarJsps(true)
        .runtime("java");

    AppEngineWebXmlProjectStageConfiguration configuration = builder.build();

    List<String> expected =
        ImmutableList.of(
            "--enable_quickstart",
            "--disable_update_check",
            "--enable_jar_splitting",
            "--jar_splitting_excludes=suffix1,suffix2",
            "--compile_encoding=UTF8",
            "--delete_jsps",
            "--enable_jar_classes",
            "--disable_jar_jsps",
            "--allow_any_runtime",
            "--runtime=java",
            "stage",
            source.toString(),
            destination.toString());

    staging.stageStandard(configuration);

    verify(appCfgRunner, times(1)).run(eq(expected));
  }

  @Test
  public void testCheckFlags_booleanFlags()
      throws AppEngineException, ProcessHandlerException, IOException {
    builder.enableQuickstart(false);
    builder.disableUpdateCheck(false);
    builder.enableJarSplitting(false);
    builder.deleteJsps(false);
    builder.enableJarClasses(false);
    builder.disableJarJsps(false);

    AppEngineWebXmlProjectStageConfiguration configuration = builder.build();

    List<String> expected = ImmutableList.of("stage", source.toString(), destination.toString());

    staging.stageStandard(configuration);

    verify(appCfgRunner, times(1)).run(eq(expected));
  }

  @Test
  public void testCheckFlags_noFlags()
      throws AppEngineException, ProcessHandlerException, IOException {

    List<String> expected = ImmutableList.of("stage", source.toString(), destination.toString());

    staging.stageStandard(builder.build());

    verify(appCfgRunner, times(1)).run(eq(expected));
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
