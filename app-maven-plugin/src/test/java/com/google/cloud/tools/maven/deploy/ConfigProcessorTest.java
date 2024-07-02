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

package com.google.cloud.tools.maven.deploy;

import static org.junit.jupiter.api.Assertions.fail;

import com.google.cloud.tools.maven.cloudsdk.ConfigReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfigProcessorTest {

  private static final String PROJECT_BUILD = "project-build";
  private static final String VERSION_BUILD = "version-build";

  @Mock private ConfigReader configReader;

  @InjectMocks private ConfigProcessor testProcessor;

  @Test
  public void testProcessProjectId_fromBuildConfig() {
    Assertions.assertEquals(PROJECT_BUILD, testProcessor.processProjectId(PROJECT_BUILD));
  }

  @Test
  public void testProcessProjectId_fromGcloud() {
    Mockito.when(configReader.getProjectId()).thenReturn("test-from-gcloud");
    Assertions.assertEquals(
        "test-from-gcloud", testProcessor.processProjectId(ConfigReader.GCLOUD_CONFIG));
  }

  @Test
  public void testProcessProjectId_fromAppengineWebXml() {
    try {
      testProcessor.processProjectId(ConfigReader.APPENGINE_CONFIG);
      fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ConfigProcessor.PROJECT_ERROR, ex.getMessage());
    }
  }

  @Test
  public void testProcessProjectId_unset() {
    try {
      testProcessor.processProjectId(null);
      fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ConfigProcessor.PROJECT_ERROR, ex.getMessage());
    }
  }

  @Test
  public void testProcessVersion_fromBuildConfig() {
    Assertions.assertEquals(VERSION_BUILD, testProcessor.processProjectId(VERSION_BUILD));
  }

  @Test
  public void testProcessVersion_fromGcloud() {
    Assertions.assertNull(testProcessor.processVersion(ConfigReader.GCLOUD_CONFIG));
  }

  @Test
  public void testProcessVersion_fromAppengineWebXml() {
    try {
      testProcessor.processVersion(ConfigReader.APPENGINE_CONFIG);
      fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ConfigProcessor.VERSION_ERROR, ex.getMessage());
    }
  }

  @Test
  public void testProcessVersion_unset() {
    try {
      testProcessor.processVersion(null);
      fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ConfigProcessor.VERSION_ERROR, ex.getMessage());
    }
  }
}
