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

import com.google.cloud.tools.managedcloudsdk.process.ProcessExecutor;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.SettableFuture;
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

/** Tests for {@link CommandCaller} */
@ExtendWith(MockitoExtension.class)
public class CommandCallerTest {

  @TempDir
  public File testDir;

  @Mock private ProcessExecutor mockProcessExecutor;
  @Mock private AsyncStreamSaver mockStdoutSaver;
  @Mock private AsyncStreamSaver mockStderrSaver;
  @Mock private AsyncStreamSaverFactory mockStreamSaverFactory;

  private final SettableFuture<String> mockStdout = SettableFuture.create();
  private final SettableFuture<String> mockStderr = SettableFuture.create();
  private List<String> fakeCommand;
  private Path fakeWorkingDirectory;
  private Map<String, String> fakeEnvironment;

  private CommandCaller testCommandCaller;

  @BeforeEach
  public void setUp() throws IOException, InterruptedException {
    fakeCommand = Arrays.asList("gcloud", "test", "--option");
    fakeWorkingDirectory = testDir.toPath();
    fakeEnvironment = ImmutableMap.of("testKey", "testValue");

    Mockito.when(mockStreamSaverFactory.newSaver())
        .thenReturn(mockStdoutSaver)
        .thenReturn(mockStderrSaver);
    Mockito.when(
            mockProcessExecutor.run(
                fakeCommand,
                fakeWorkingDirectory,
                fakeEnvironment,
                mockStdoutSaver,
                mockStderrSaver))
        .thenReturn(0);
    Mockito.when(mockStdoutSaver.getResult()).thenReturn(mockStdout);
    Mockito.when(mockStderrSaver.getResult()).thenReturn(mockStderr);

    mockStdout.set("stdout");
    mockStderr.set("stderr");

    testCommandCaller = new CommandCaller(() -> mockProcessExecutor, mockStreamSaverFactory);
  }

  private void verifyCommandExecution() throws IOException, InterruptedException {
    Mockito.verify(mockProcessExecutor)
        .run(fakeCommand, fakeWorkingDirectory, fakeEnvironment, mockStdoutSaver, mockStderrSaver);
    Mockito.verifyNoMoreInteractions(mockProcessExecutor);
  }

  @Test
  public void testCall()
      throws IOException, InterruptedException, CommandExecutionException, CommandExitException {
    Assertions.assertEquals(
        "stdout", testCommandCaller.call(fakeCommand, fakeWorkingDirectory, fakeEnvironment));
    verifyCommandExecution();
  }

  @Test
  public void testCall_nonZeroExit()
      throws IOException, InterruptedException, CommandExecutionException {
    Mockito.when(
            mockProcessExecutor.run(
                fakeCommand,
                fakeWorkingDirectory,
                fakeEnvironment,
                mockStdoutSaver,
                mockStderrSaver))
        .thenReturn(10);

    try {
      testCommandCaller.call(fakeCommand, fakeWorkingDirectory, fakeEnvironment);
      Assertions.fail("CommandExitException expected but not found.");
    } catch (CommandExitException ex) {
      Assertions.assertEquals("Process failed with exit code: 10\nstdout\nstderr", ex.getMessage());
      Assertions.assertEquals(10, ex.getExitCode());
      Assertions.assertEquals("stdout\nstderr", ex.getErrorLog());
    }
    verifyCommandExecution();
  }

  @Test
  public void testCall_ioException()
      throws CommandExitException, InterruptedException, IOException {
    Throwable cause = new IOException("oops");
    Mockito.when(
            mockProcessExecutor.run(
                fakeCommand,
                fakeWorkingDirectory,
                fakeEnvironment,
                mockStdoutSaver,
                mockStderrSaver))
        .thenThrow(cause);

    try {
      testCommandCaller.call(fakeCommand, fakeWorkingDirectory, fakeEnvironment);
      Assertions.fail("CommandExecutionException expected but not found.");
    } catch (CommandExecutionException ex) {
      Assertions.assertEquals("stdout\nstderr", ex.getMessage());
    }

    verifyCommandExecution();
  }

  @Test
  public void testCall_interruptedExceptionPassthrough()
      throws CommandExecutionException, CommandExitException, InterruptedException, IOException {

    AbstractFuture<String> future =
        new AbstractFuture<String>() {
          @Override
          public String get() throws InterruptedException {
            throw new InterruptedException();
          }
        };
    Mockito.when(mockStdoutSaver.getResult()).thenReturn(future);

    try {
      testCommandCaller.call(fakeCommand, fakeWorkingDirectory, fakeEnvironment);
      Assertions.fail("InterruptedException expected but not found.");
    } catch (InterruptedException ex) {
      // pass
    }

    verifyCommandExecution();
  }
}
