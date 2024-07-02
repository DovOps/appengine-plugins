/*
 * Copyright 2016 Google LLC. All Rights Reserved.
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
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.project.MavenProject;
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
public class AbstractStageMojoTest {

  @TempDir
  public File testDirectory;

  @Mock private MavenProject mavenProject;
  @Mock private File sourceDirectory;

  @InjectMocks
  public AbstractStageMojo testMojo =
      new AbstractStageMojo() {
        @Override
        public void execute() {
          // do nothing
        }
      };

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  // JunitParamsRunnerToParameterized conversion not supported
  @Parameters({"pom", "ear", "rar", "par", "ejb", "maven-plugin", "eclipse-plugin"})
  public void testIsAppEngineCompatiblePackaging_false(String packaging) {
    Mockito.when(mavenProject.getPackaging()).thenReturn(packaging);

    Assertions.assertFalse(testMojo.isAppEngineCompatiblePackaging());
  }

  @Test
  // JunitParamsRunnerToParameterized conversion not supported
  @Parameters({"jar", "war"})
  public void testIsAppEngineCompatiblePackaging_true(String packaging) {
    Mockito.when(mavenProject.getPackaging()).thenReturn(packaging);

    Assertions.assertTrue(testMojo.isAppEngineCompatiblePackaging());
  }

  @Test
  public void testIsAppEngineWebXmlBased_true() throws IOException {
    Mockito.when(sourceDirectory.toPath()).thenReturn(testDirectory.toPath());
    newFolder(testDirectory, "WEB-INF");
    File.createTempFile("WEB-INF/appengine-web.xml", null, testDirectory);

    Assertions.assertTrue(testMojo.isAppEngineWebXmlBased());
  }

  @Test
  public void testIsAppEngineWebXmlBased_false() throws IOException {
    Mockito.when(sourceDirectory.toPath()).thenReturn(testDirectory.toPath());
    newFolder(testDirectory, "WEB-INF");

    Assertions.assertFalse(testMojo.isAppEngineWebXmlBased());
  }

  private static File newFolder(File root, String... subDirs) throws IOException {
    String subFolder = String.join("/", subDirs);
    File result = new File(root, subFolder);
    if (!result.mkdirs()) {
      throw new IOException("Couldn't create folders " + root);
    }
    return result;
  }
}
