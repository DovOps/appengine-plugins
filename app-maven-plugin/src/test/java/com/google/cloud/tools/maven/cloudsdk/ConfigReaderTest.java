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

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.appengine.operations.Gcloud;
import com.google.cloud.tools.appengine.operations.cloudsdk.serialization.CloudSdkConfig;
import java.io.File;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(JUnitParamsRunner.class)
public class ConfigReaderTest {

  @TempDir
  public File temporaryFolder;

  @Mock private Gcloud gcloud;
  @InjectMocks private ConfigReader testReader;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito.when(gcloud.getConfig()).thenReturn(mock(CloudSdkConfig.class));
  }

  @Test
  public void testGetProjectId_gcloudPass() throws Exception {
    Mockito.when(gcloud.getConfig().getProject()).thenReturn("some-project");

    Assertions.assertEquals("some-project", testReader.getProjectId());
  }

  @Test
  // JunitParamsRunnerToParameterized conversion not supported
  @Parameters({"null", ""})
  public void testGetProjectId_gcloudFail(@Nullable String gcloudProject) throws Exception {
    Mockito.when(gcloud.getConfig().getProject()).thenReturn(gcloudProject);

    try {
      testReader.getProjectId();
      fail();
    } catch (RuntimeException ex) {
      Assertions.assertEquals("Project was not found in gcloud config", ex.getMessage());
    }
  }
}
