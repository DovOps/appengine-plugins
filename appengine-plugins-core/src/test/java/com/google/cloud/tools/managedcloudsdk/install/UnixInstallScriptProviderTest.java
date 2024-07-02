/*
 * Copyright 2018 Google LLC
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class UnixInstallScriptProviderTest {

  @Test
  public void testGetScriptCommandLine_nonAbsoluteSdkRoot() {
    try {
      new UnixInstallScriptProvider(Collections.emptyMap())
          .getScriptCommandLine(Paths.get("relative/path"));
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals("non-absolute SDK path", e.getMessage());
    }
  }

  @Test
  public void testGetScriptCommandLine() {
    Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows"));

    Path sdkRoot = Paths.get("/path/to/sdk");
    List<String> commandLine =
        new UnixInstallScriptProvider(Collections.emptyMap()).getScriptCommandLine(sdkRoot);

    Assertions.assertEquals(1, commandLine.size());
    Path scriptPath = Paths.get(commandLine.getFirst());
    Assertions.assertTrue(scriptPath.isAbsolute());
    Assertions.assertEquals(Paths.get("/path/to/sdk/install.sh"), scriptPath);
  }
}
