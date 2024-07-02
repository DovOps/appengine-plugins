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

import com.google.cloud.tools.managedcloudsdk.BadCloudSdkVersionException;
import com.google.cloud.tools.managedcloudsdk.OsInfo;
import com.google.cloud.tools.managedcloudsdk.Version;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FileResourceProviderFactoryTest {

  @TempDir
  public File testDir;

  private Path fakeSdkHome;
  private Path fakeDownloadsDir;

  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            new OsInfo(OsInfo.Name.WINDOWS, OsInfo.Architecture.X86),
            "google-cloud-sdk-windows-bundled-python.zip",
            "windows-x86-bundled-python.zip",
            "gcloud.cmd"
          },
          {
            new OsInfo(OsInfo.Name.WINDOWS, OsInfo.Architecture.X86_64),
            "google-cloud-sdk-windows-x86_64-bundled-python.zip",
            "windows-x86_64-bundled-python.zip",
            "gcloud.cmd"
          },
          {
            new OsInfo(OsInfo.Name.MAC, OsInfo.Architecture.X86),
            "google-cloud-sdk.tar.gz",
            "darwin-x86.tar.gz",
            "gcloud"
          },
          {
            new OsInfo(OsInfo.Name.MAC, OsInfo.Architecture.X86_64),
            "google-cloud-sdk.tar.gz",
            "darwin-x86_64.tar.gz",
            "gcloud"
          },
          {
            new OsInfo(OsInfo.Name.LINUX, OsInfo.Architecture.X86),
            "google-cloud-sdk.tar.gz",
            "linux-x86.tar.gz",
            "gcloud"
          },
          {
            new OsInfo(OsInfo.Name.LINUX, OsInfo.Architecture.X86_64),
            "google-cloud-sdk.tar.gz",
            "linux-x86_64.tar.gz",
            "gcloud"
          },
        });
  }
  public OsInfo osInfo;
  public String latestFilename;
  public String versionedFilenameTail;
  public String gcloudExecutable;

  @BeforeEach
  public void setUp() {
    fakeSdkHome = testDir.toPath();
    fakeDownloadsDir = fakeSdkHome.resolve("downloads");
  }

  @MethodSource("data")
  @ParameterizedTest
  public void testNewFileResourceProvider_latest(OsInfo osInfo, String latestFilename, String versionedFilenameTail, String gcloudExecutable) throws MalformedURLException {
    initFileResourceProviderFactoryTest(osInfo, latestFilename, versionedFilenameTail, gcloudExecutable);
    FileResourceProviderFactory factory =
        new FileResourceProviderFactory(Version.LATEST, osInfo, fakeSdkHome);
    FileResourceProvider provider = factory.newFileResourceProvider();

    Assertions.assertEquals(
        new URL(FileResourceProviderFactory.LATEST_BASE_URL + latestFilename),
        provider.getArchiveSource());
    Assertions.assertEquals(fakeDownloadsDir.resolve(latestFilename), provider.getArchiveDestination());
    Assertions.assertEquals(fakeSdkHome.resolve("LATEST"), provider.getArchiveExtractionDestination());
    Assertions.assertEquals(
        fakeSdkHome.resolve("LATEST").resolve("google-cloud-sdk"), provider.getExtractedSdkHome());
    Assertions.assertEquals(
        fakeSdkHome
            .resolve("LATEST")
            .resolve("google-cloud-sdk")
            .resolve("bin")
            .resolve(gcloudExecutable),
        provider.getExtractedGcloud());
  }

  @MethodSource("data")
  @ParameterizedTest
  public void testNewFileResourceProvider_versioned(OsInfo osInfo, String latestFilename, String versionedFilenameTail, String gcloudExecutable)
      throws MalformedURLException, BadCloudSdkVersionException {
    initFileResourceProviderFactoryTest(osInfo, latestFilename, versionedFilenameTail, gcloudExecutable);
    FileResourceProviderFactory factory =
        new FileResourceProviderFactory(new Version("123.123.123"), osInfo, fakeSdkHome);
    FileResourceProvider provider = factory.newFileResourceProvider();

    Assertions.assertEquals(
        new URL(
            FileResourceProviderFactory.VERSIONED_BASE_URL
                + "google-cloud-sdk-123.123.123-"
                + versionedFilenameTail),
        provider.getArchiveSource());
    Assertions.assertEquals(
        fakeDownloadsDir.resolve("google-cloud-sdk-123.123.123-" + versionedFilenameTail),
        provider.getArchiveDestination());
    Assertions.assertEquals(
        fakeSdkHome.resolve("123.123.123"), provider.getArchiveExtractionDestination());
    Assertions.assertEquals(
        fakeSdkHome.resolve("123.123.123").resolve("google-cloud-sdk"),
        provider.getExtractedSdkHome());
    Assertions.assertEquals(
        fakeSdkHome
            .resolve("123.123.123")
            .resolve("google-cloud-sdk")
            .resolve("bin")
            .resolve(gcloudExecutable),
        provider.getExtractedGcloud());
  }

  public void initFileResourceProviderFactoryTest(OsInfo osInfo, String latestFilename, String versionedFilenameTail, String gcloudExecutable) {
    this.osInfo = osInfo;
    this.latestFilename = latestFilename;
    this.versionedFilenameTail = versionedFilenameTail;
    this.gcloudExecutable = gcloudExecutable;
  }
}
