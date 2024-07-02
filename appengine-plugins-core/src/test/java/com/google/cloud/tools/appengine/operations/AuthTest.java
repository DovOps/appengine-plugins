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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthTest {
  @Mock private GcloudRunner gcloudRunner;
  @TempDir
  public File tmpDir;

  @Test
  public void testNullSdk() {
    try {
      new Auth(null);
      Assertions.fail("allowed null runner");
    } catch (NullPointerException expected) {
      // pass
    }
  }

  @Test
  public void testLogin_withUser() throws AppEngineException, ProcessHandlerException, IOException {
    String testUsername = "potato@potato.com";
    new Auth(gcloudRunner).login(testUsername);
    Mockito.verify(gcloudRunner).run(eq(Arrays.asList("auth", "login", testUsername)), isNull());
  }

  @Test
  public void testLogin_withBadUser() {
    String testUsername = "potato@pota@to.com";
    try {
      new Auth(gcloudRunner).login(testUsername);
      Assertions.fail("Should have failed with bad user.");
    } catch (AppEngineException expected) {
      assertThat(
          expected.getMessage(),
          CoreMatchers.containsString("Invalid email address: " + testUsername));
    }
  }

  @Test
  public void testLogin_withNullUser() throws AppEngineException {
    try {
      new Auth(gcloudRunner).login(null);
      Assertions.fail("Should have failed with bad user.");
    } catch (NullPointerException npe) {
      // pass
    }
  }

  @Test
  public void testLogin_noUser() throws ProcessHandlerException, AppEngineException, IOException {
    new Auth(gcloudRunner).login();
    Mockito.verify(gcloudRunner).run(eq(Arrays.asList("auth", "login")), isNull());
  }

  @Test
  public void testActivateServiceAccount()
      throws ProcessHandlerException, IOException, AppEngineException {
    Path jsonKeyFile = File.createTempFile("json-keys", null, tmpDir).toPath();
    new Auth(gcloudRunner).activateServiceAccount(jsonKeyFile);
    Mockito.verify(gcloudRunner)
        .run(
            eq(
                Arrays.asList(
                    "auth",
                    "activate-service-account",
                    "--key-file",
                    jsonKeyFile.toAbsolutePath().toString())),
            isNull());
  }

  @Test
  public void testActivateServiceAccount_badKeyFile() throws AppEngineException {
    Path jsonKeyFile = tmpDir.toPath().resolve("non-existant-file");
    try {
      new Auth(gcloudRunner).activateServiceAccount(jsonKeyFile);
      Assertions.fail("Should have failed with bad keyfile.");
    } catch (IllegalArgumentException expected) {
      assertThat(
          expected.getMessage(),
          CoreMatchers.containsString("File does not exist: " + jsonKeyFile));
    }
  }
}
