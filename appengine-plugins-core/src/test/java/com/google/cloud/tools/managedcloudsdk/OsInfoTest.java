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

package com.google.cloud.tools.managedcloudsdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link OsInfo}. */
public class OsInfoTest {

  @Test
  public void testGetSystemOs_windows() throws UnsupportedOsException {
    Assertions.assertEquals(OsInfo.Name.WINDOWS, OsInfo.getSystemOs("Windows"));
    Assertions.assertEquals(OsInfo.Name.WINDOWS, OsInfo.getSystemOs("windows"));
    Assertions.assertEquals(OsInfo.Name.WINDOWS, OsInfo.getSystemOs("windows 300"));
  }

  @Test
  public void testGetSystemOs_linux() throws UnsupportedOsException {
    Assertions.assertEquals(OsInfo.Name.LINUX, OsInfo.getSystemOs("Linux"));
    Assertions.assertEquals(OsInfo.Name.LINUX, OsInfo.getSystemOs("linux"));
    Assertions.assertEquals(OsInfo.Name.LINUX, OsInfo.getSystemOs("linux 300"));
  }

  @Test
  public void testGetSystemOs_mac() throws UnsupportedOsException {
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("Mac OS X"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("mac os x"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("mac os x 300"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("macOS"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("macos"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("macos 300"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("Darwin"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("darwin"));
    Assertions.assertEquals(OsInfo.Name.MAC, OsInfo.getSystemOs("darwin 300"));
  }

  @Test
  public void testGetSystemOs_unsupported() {
    try {
      OsInfo.getSystemOs("BadOs 10.3");
      Assertions.fail("UnsupportedOsException expected but not thrown.");
    } catch (UnsupportedOsException ex) {
      Assertions.assertEquals("Unknown OS: BadOs 10.3", ex.getMessage());
    }
  }

  @Test
  public void testGetSystemArchitecture_is64() {
    Assertions.assertEquals(OsInfo.Architecture.X86_64, OsInfo.getSystemArchitecture("64"));
    Assertions.assertEquals(OsInfo.Architecture.X86_64, OsInfo.getSystemArchitecture("universal"));
    Assertions.assertEquals(OsInfo.Architecture.X86_64, OsInfo.getSystemArchitecture("junk64Junk"));
    Assertions.assertEquals(
        OsInfo.Architecture.X86_64, OsInfo.getSystemArchitecture("junkUniversaljunk"));
  }

  @Test
  public void testGetSystemArchitecture_defaultIs32() {
    Assertions.assertEquals(OsInfo.Architecture.X86, OsInfo.getSystemArchitecture("32"));
    Assertions.assertEquals(OsInfo.Architecture.X86, OsInfo.getSystemArchitecture("junk32junk"));
    Assertions.assertEquals(OsInfo.Architecture.X86, OsInfo.getSystemArchitecture("junk"));
  }
}
