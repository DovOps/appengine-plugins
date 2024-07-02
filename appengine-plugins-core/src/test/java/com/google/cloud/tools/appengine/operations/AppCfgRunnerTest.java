/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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

import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.operations.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.operations.cloudsdk.InvalidJavaSdkException;
import com.google.cloud.tools.appengine.operations.cloudsdk.internal.process.ProcessBuilderFactory;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandler;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AppCfgRunnerTest {

  @TempDir
  public File testFolder;

  @Mock private CloudSdk sdk;
  @Mock private ProcessBuilderFactory processBuilderFactory;
  @Mock private ProcessBuilder processBuilder;
  @Mock private ProcessHandler processHandler;
  @Mock private Process process;
  private Path javaExecutablePath;
  private Path appengineToolsJar;
  private Path appengineJavaSdkPath;

  @BeforeEach
  public void setUp() throws IOException {
    javaExecutablePath = testFolder.toPath().resolve("java.fake");
    appengineToolsJar = testFolder.toPath().resolve("appengine.tools");
    appengineJavaSdkPath = testFolder.toPath().resolve("appengine-sdk-root");
    when(sdk.getJavaExecutablePath()).thenReturn(javaExecutablePath);
    when(sdk.getAppEngineToolsJar()).thenReturn(appengineToolsJar);
    when(sdk.getAppEngineSdkForJavaPath()).thenReturn(appengineJavaSdkPath);

    when(processBuilderFactory.newProcessBuilder()).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);
  }

  @Test
  public void testRun()
      throws InvalidJavaSdkException, ProcessHandlerException,
          AppEngineJavaComponentsNotInstalledException, IOException {
    AppCfgRunner appCfgRunner =
        new AppCfgRunner.Factory(processBuilderFactory).newRunner(sdk, processHandler);

    appCfgRunner.run(ImmutableList.of("some", "command"));

    Mockito.verify(processBuilder)
        .command(
            ImmutableList.of(
                javaExecutablePath.toString(),
                "-cp",
                appengineToolsJar.toString(),
                "com.google.appengine.tools.admin.AppCfg",
                "some",
                "command"));
    Mockito.verify(processBuilder).start();
    Mockito.verifyNoMoreInteractions(processBuilder);

    Mockito.verify(processHandler).handleProcess(process);
    Assertions.assertEquals(appengineJavaSdkPath.toString(), System.getProperty("appengine.sdk.root"));
  }
}
