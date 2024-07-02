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

package com.google.cloud.tools.maven.cloudsdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.managedcloudsdk.BadCloudSdkVersionException;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.Version;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.components.SdkUpdater;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.function.Function;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudSdkDownloaderTest {

  @Mock private Log log;
  @Mock private Function<String, ManagedCloudSdk> managedCloudSdkFactory;
  @Mock private ManagedCloudSdk managedCloudSdk;
  private String version = "123.123.123";

  @Mock private SdkInstaller installer;
  @Mock private SdkComponentInstaller componentInstaller;
  @Mock private SdkUpdater updater;

  @InjectMocks private CloudSdkDownloader downloader;

  @BeforeEach
  public void setup() {
    when(managedCloudSdkFactory.apply(version)).thenReturn(managedCloudSdk);
    when(managedCloudSdk.newInstaller()).thenReturn(installer);
    when(managedCloudSdk.newComponentInstaller()).thenReturn(componentInstaller);
    when(managedCloudSdk.newUpdater()).thenReturn(updater);
  }

  @Test
  public void testDownloadCloudSdk_install()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA), false);
    verify(managedCloudSdk).newInstaller();
  }

  @Test
  public void testDownloadCloudSdk_installSingeComponent()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA), false);
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk).newComponentInstaller();
  }

  @Test
  public void testDownloadCloudSdk_installMultipleComponents()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, CommandExitException, CommandExecutionException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
    when(managedCloudSdk.hasComponent(SdkComponent.BETA)).thenReturn(false);
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA, SdkComponent.BETA), false);
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk, times(2)).newComponentInstaller();
    verify(componentInstaller).installComponent(eq(SdkComponent.APP_ENGINE_JAVA), any(), any());
    verify(componentInstaller).installComponent(eq(SdkComponent.BETA), any(), any());
  }

  @Test
  public void testDownloadCloudSdk_installSomeOfMultipleComponents()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, CommandExitException, CommandExecutionException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
    when(managedCloudSdk.hasComponent(SdkComponent.BETA)).thenReturn(true);
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA, SdkComponent.BETA), false);
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk).newComponentInstaller();
    verify(componentInstaller).installComponent(eq(SdkComponent.APP_ENGINE_JAVA), any(), any());
  }

  @Test
  public void testDownloadCloudSdk_ignoreComponents()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    downloader.downloadIfNecessary(version, log, Collections.emptyList(), false);
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk, never()).newComponentInstaller();
  }

  @Test
  public void testDownloadCloudSdk_update()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(true);
    when(managedCloudSdk.isUpToDate()).thenReturn(false);
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA), false);
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk, never()).newComponentInstaller();
    verify(managedCloudSdk).newUpdater();
  }

  @Test
  public void testDownloadCloudSdk_offlineMode() {
    downloader.downloadIfNecessary(
        version, log, ImmutableList.of(SdkComponent.APP_ENGINE_JAVA), true);
    verify(managedCloudSdk).getSdkHome();
    verifyNoMoreInteractions(managedCloudSdk);
  }

  @Test
  public void testNewManagedSdk_null() throws UnsupportedOsException {
    // There's no way of testing for direct ManagedCloudSdk equality, so compare home paths
    ManagedCloudSdk sdk = CloudSdkDownloader.newManagedSdkFactory().apply(null);
    Assertions.assertEquals(ManagedCloudSdk.newManagedSdk().getSdkHome(), sdk.getSdkHome());
  }

  @Test
  public void testNewManagedSdk_specific()
      throws UnsupportedOsException, BadCloudSdkVersionException {
    ManagedCloudSdk sdk = CloudSdkDownloader.newManagedSdkFactory().apply("191.0.0");
    Assertions.assertEquals(
        ManagedCloudSdk.newManagedSdk(new Version("191.0.0")).getSdkHome(), sdk.getSdkHome());
  }
}
