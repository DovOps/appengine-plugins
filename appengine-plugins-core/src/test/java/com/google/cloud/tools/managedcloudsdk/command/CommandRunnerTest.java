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

package com.google.cloud.tools.managedcloudsdk.command;

import com.google.cloud.tools.managedcloudsdk.ConsoleListener;
import com.google.cloud.tools.managedcloudsdk.process.AsyncStreamHandler;
import com.google.cloud.tools.managedcloudsdk.process.ProcessExecutor;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link CommandRunner} */
@ExtendWith(MockitoExtension.class)
public class CommandRunnerTest {

  @TempDir
  public File testDir;

  @Mock private ProcessExecutor mockProcessExecutor;
  @Mock private ConsoleListener mockConsoleListener;
  @Mock private AsyncStreamHandler mockStreamHandler;
  @Mock private AsyncStreamHandlerFactory mockStreamHandlerFactory;

  private List<String> fakeCommand;
  private Path fakeWorkingDirectory;
  private Map<String, String> fakeEnvironment;

  private CommandRunner testCommandRunner;

  @BeforeEach
  public void setUp() throws IOException, InterruptedException {
    fakeCommand = Arrays.asList("gcloud", "test", "--option");
    fakeWorkingDirectory = testDir.toPath();
    fakeEnvironment = ImmutableMap.of("testKey", "testValue");

    Mockito.when(mockStreamHandlerFactory.newHandler(mockConsoleListener))
        .thenReturn(mockStreamHandler);
    Mockito.when(
            mockProcessExecutor.run(
                fakeCommand,
                fakeWorkingDirectory,
                fakeEnvironment,
                mockStreamHandler,
                mockStreamHandler))
        .thenReturn(0);

    testCommandRunner = new CommandRunner(() -> mockProcessExecutor, mockStreamHandlerFactory);
  }

  private void verifyCommandExecution() throws IOException, InterruptedException {
    Mockito.verify(mockProcessExecutor)
        .run(
            fakeCommand,
            fakeWorkingDirectory,
            fakeEnvironment,
            mockStreamHandler,
            mockStreamHandler);
    Mockito.verifyNoMoreInteractions(mockProcessExecutor);
  }

  @Test
  public void testRun()
      throws InterruptedException, CommandExitException, CommandExecutionException, IOException {
    testCommandRunner.run(fakeCommand, fakeWorkingDirectory, fakeEnvironment, mockConsoleListener);
    verifyCommandExecution();
  }

  @Test
  public void testCall_nonZeroExit() throws Exception {
    Mockito.when(
            mockProcessExecutor.run(
                fakeCommand,
                fakeWorkingDirectory,
                fakeEnvironment,
                mockStreamHandler,
                mockStreamHandler))
        .thenReturn(10);

    try {
      testCommandRunner.run(
          fakeCommand, fakeWorkingDirectory, fakeEnvironment, mockConsoleListener);
      Assertions.fail("CommandExitException expected but not found.");
    } catch (CommandExitException ex) {
      Assertions.assertEquals("Process failed with exit code: 10", ex.getMessage());
      Assertions.assertEquals(10, ex.getExitCode());
      Assertions.assertEquals(null, ex.getErrorLog());
    }
    verifyCommandExecution();
  }
}
