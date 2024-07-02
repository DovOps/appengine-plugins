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

package com.google.cloud.tools.managedcloudsdk.install;

import com.google.cloud.tools.managedcloudsdk.OsInfo;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for {@link InstallerFactory}. * */
public class InstallerFactoryTest {

  @TempDir
  public File tmp;

  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {new OsInfo(OsInfo.Name.LINUX, null), UnixInstallScriptProvider.class},
          {new OsInfo(OsInfo.Name.MAC, null), UnixInstallScriptProvider.class},
          {new OsInfo(OsInfo.Name.WINDOWS, null), WindowsInstallScriptProvider.class}
        });
  }
  public OsInfo os;
  public Class<InstallScriptProvider> expectedInstallScriptProviderClass;

  @MethodSource("data")
  @ParameterizedTest
  public void testNewInstaller_latestVersion(OsInfo os, Class<InstallScriptProvider> expectedInstallScriptProviderClass) {
    initInstallerFactoryTest(os, expectedInstallScriptProviderClass);
    Installer installer = new InstallerFactory(os, false).newInstaller(null, null, null);
    Assertions.assertEquals(
        expectedInstallScriptProviderClass, installer.getInstallScriptProvider().getClass());
  }

  public void initInstallerFactoryTest(OsInfo os, Class<InstallScriptProvider> expectedInstallScriptProviderClass) {
    this.os = os;
    this.expectedInstallScriptProviderClass = expectedInstallScriptProviderClass;
  }
}
