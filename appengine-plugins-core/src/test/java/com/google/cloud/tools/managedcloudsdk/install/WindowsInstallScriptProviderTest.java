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

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class WindowsInstallScriptProviderTest {

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
    Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));

    Path sdkRoot = Paths.get("C:\\path\\to\\sdk");
    List<String> commandLine =
        new WindowsInstallScriptProvider(Collections.emptyMap()).getScriptCommandLine(sdkRoot);

    Assertions.assertEquals(3, commandLine.size());
    Assertions.assertEquals("cmd.exe", commandLine.getFirst());
    Assertions.assertEquals("/c", commandLine.get(1));

    Path scriptPath = Paths.get(commandLine.get(2));
    Assertions.assertTrue(scriptPath.isAbsolute());
    Assertions.assertEquals(Paths.get("C:\\path\\to\\sdk\\install.bat"), scriptPath);
  }

  @Test
  public void testEnvironmentVariblesPickedUp() {
    Map<String, String> proxyVariable = ImmutableMap.of("http_proxy", "test-proxy:8080");
    Map<String, String> environment =
        new WindowsInstallScriptProvider(proxyVariable).getScriptEnvironment();

    Assertions.assertEquals(
        environment,
        ImmutableMap.of("CLOUDSDK_CORE_DISABLE_PROMPTS", "1", "http_proxy", "test-proxy:8080"));
  }
}
