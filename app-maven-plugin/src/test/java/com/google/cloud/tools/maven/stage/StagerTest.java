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

package com.google.cloud.tools.maven.stage;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagerTest {

  @TempDir
  public File tempFolder;

  @Mock private AbstractStageMojo stageMojo;

  @Test
  public void testNewStager_noOpStager() throws MojoExecutionException {
    Mockito.when(stageMojo.isAppEngineCompatiblePackaging()).thenReturn(false);
    Stager stager = Stager.newStager(stageMojo);
    Assertions.assertEquals(NoOpStager.class, stager.getClass());
  }

  @Test
  public void testNewStager_xml() throws MojoExecutionException {
    Mockito.when(stageMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    Mockito.when(stageMojo.isAppEngineWebXmlBased()).thenReturn(true);
    Mockito.when(stageMojo.getArtifact()).thenReturn(tempFolder.toPath());

    Stager stager = Stager.newStager(stageMojo);
    Assertions.assertEquals(AppEngineWebXmlStager.class, stager.getClass());
  }

  @Test
  public void testNewStager_yaml() throws MojoExecutionException {
    Mockito.when(stageMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    Mockito.when(stageMojo.isAppEngineWebXmlBased()).thenReturn(false);
    Mockito.when(stageMojo.getArtifact()).thenReturn(tempFolder.toPath());

    Stager stager = Stager.newStager(stageMojo);
    Assertions.assertEquals(AppYamlStager.class, stager.getClass());
  }

  @Test
  public void testNewStager_noArtifact() {
    Mockito.when(stageMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    try {
      Stager.newStager(stageMojo);
      Assertions.fail();
    } catch (MojoExecutionException ex) {
      Assertions.assertEquals(
          """
          
          Could not determine appengine environment, did you package your application?
          Run 'mvn package appengine:stage'\
          """,
          ex.getMessage());
    }
  }
}
