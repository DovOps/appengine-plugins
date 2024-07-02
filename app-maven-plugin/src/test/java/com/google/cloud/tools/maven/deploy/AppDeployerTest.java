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

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import com.google.cloud.tools.appengine.configuration.DeployProjectConfigurationConfiguration;
import com.google.cloud.tools.appengine.operations.Deployment;
import com.google.cloud.tools.maven.cloudsdk.CloudSdkAppEngineFactory;
import com.google.cloud.tools.maven.deploy.AppDeployer.ConfigBuilder;
import com.google.cloud.tools.maven.stage.Stager;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AppDeployerTest {

  @TempDir
  public File tempFolder;

  @Mock private ConfigBuilder configBuilder;
  @Mock private Stager stager;
  @Mock private AbstractDeployMojo deployMojo;

  private Path stagingDirectory;
  private Path appengineDirectory;
  @Mock private CloudSdkAppEngineFactory appEngineFactory;
  @Mock private Deployment appEngineDeployment;
  @Mock private DeployConfiguration deployConfiguration;
  @Mock private DeployProjectConfigurationConfiguration deployProjectConfigurationConfiguration;
  @Mock private Log mockLog;

  private AppDeployer testDeployer;

  @BeforeEach
  public void setup() throws IOException {
    stagingDirectory = newFolder(tempFolder, "staging").toPath();
    appengineDirectory = newFolder(tempFolder, "appengine").toPath();

    testDeployer = new AppDeployer(deployMojo, stager, configBuilder, appengineDirectory);

    Mockito.when(deployMojo.getStagingDirectory()).thenReturn(stagingDirectory);
    Mockito.when(deployMojo.getAppEngineFactory()).thenReturn(appEngineFactory);
    Mockito.when(appEngineFactory.deployment()).thenReturn(appEngineDeployment);
    Mockito.when(configBuilder.buildDeployProjectConfigurationConfiguration(appengineDirectory))
        .thenReturn(deployProjectConfigurationConfiguration);
    Mockito.when(deployMojo.getLog()).thenReturn(mockLog);
  }

  @Test
  public void testDeploy() throws MojoExecutionException, AppEngineException {
    Mockito.when(configBuilder.buildDeployConfiguration(ImmutableList.of(stagingDirectory)))
        .thenReturn(deployConfiguration);
    testDeployer.deploy();
    Mockito.verify(stager).stage();
    Mockito.verify(appEngineDeployment).deploy(deployConfiguration);
  }

  private List<Path> createStagedYamls(String... names) throws IOException {
    List<Path> createdFiles = new ArrayList<>();
    for (String name : names) {
      createdFiles.add(Files.createFile(appengineDirectory.resolve(name + ".yaml")));
    }
    return createdFiles;
  }

  private Path createAppYaml() throws IOException {
    return Files.createFile(stagingDirectory.resolve("app.yaml"));
  }

  @Test
  public void testDeployAll_allYamls()
      throws MojoExecutionException, AppEngineException, IOException {
    List<Path> files =
        ImmutableList.<Path>builder()
            .add(createAppYaml())
            .addAll(createStagedYamls("cron", "dispatch", "dos", "index", "queue"))
            .build();
    // also create a file we'll ignore
    Files.createFile(appengineDirectory.resolve("ignored"));

    Mockito.when(configBuilder.buildDeployConfiguration(Mockito.eq(files)))
        .thenReturn(deployConfiguration);

    testDeployer.deployAll();
    Mockito.verify(stager).stage();
    Mockito.verify(appEngineDeployment).deploy(deployConfiguration);
  }

  @Test
  public void testDeployAll_someYamls()
      throws MojoExecutionException, AppEngineException, IOException {
    List<Path> files =
        ImmutableList.<Path>builder()
            .add(createAppYaml())
            .addAll(createStagedYamls("dispatch", "dos", "queue"))
            .build();
    // also create a file we'll ignore
    Files.createFile(appengineDirectory.resolve("ignored"));

    Mockito.when(configBuilder.buildDeployConfiguration(Mockito.eq(files)))
        .thenReturn(deployConfiguration);

    testDeployer.deployAll();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployConfiguration(files);
    Mockito.verify(appEngineDeployment).deploy(deployConfiguration);
  }

  @Test
  public void testDeployAll_noAppYaml() throws IOException {
    createStagedYamls("dos", "cron");

    try {
      testDeployer.deployAll();
      fail();
    } catch (MojoExecutionException ex) {
      Assertions.assertEquals("Failed to deploy all: could not find app.yaml.", ex.getMessage());
    }
  }

  @Test
  public void testDeployCron() throws MojoExecutionException, AppEngineException {
    testDeployer.deployCron();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployProjectConfigurationConfiguration(appengineDirectory);
    Mockito.verify(appEngineDeployment).deployCron(deployProjectConfigurationConfiguration);
  }

  @Test
  public void testDeployDos() throws MojoExecutionException, AppEngineException {
    testDeployer.deployDos();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployProjectConfigurationConfiguration(appengineDirectory);
    Mockito.verify(appEngineDeployment).deployDos(deployProjectConfigurationConfiguration);
  }

  @Test
  public void testDeployDispatch() throws MojoExecutionException, AppEngineException {
    testDeployer.deployDispatch();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployProjectConfigurationConfiguration(appengineDirectory);
    Mockito.verify(appEngineDeployment).deployDispatch(deployProjectConfigurationConfiguration);
  }

  @Test
  public void testDeployIndex() throws MojoExecutionException, AppEngineException {
    testDeployer.deployIndex();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployProjectConfigurationConfiguration(appengineDirectory);
    Mockito.verify(appEngineDeployment).deployIndex(deployProjectConfigurationConfiguration);
  }

  @Test
  public void testDeployQueue() throws MojoExecutionException, AppEngineException {
    testDeployer.deployQueue();
    Mockito.verify(stager).stage();
    Mockito.verify(configBuilder).buildDeployProjectConfigurationConfiguration(appengineDirectory);
    Mockito.verify(appEngineDeployment).deployQueue(deployProjectConfigurationConfiguration);
  }

  @Test
  public void testBuildDeployConfiguration() {
    AbstractDeployMojo deployMojo = Mockito.mock(AbstractDeployMojo.class);
    Mockito.when(deployMojo.getBucket()).thenReturn("testBucket");
    Mockito.when(deployMojo.getGcloudMode()).thenReturn("beta");
    Mockito.when(deployMojo.getImageUrl()).thenReturn("testImageUrl");
    Mockito.when(deployMojo.getProjectId()).thenReturn("testProjectId");
    Mockito.when(deployMojo.getPromote()).thenReturn(false);
    Mockito.when(deployMojo.getStopPreviousVersion()).thenReturn(false);
    Mockito.when(deployMojo.getServer()).thenReturn("testServer");
    Mockito.when(deployMojo.getVersion()).thenReturn("testVersion");

    ConfigProcessor configProcessor = Mockito.mock(ConfigProcessor.class);
    Mockito.when(configProcessor.processProjectId("testProjectId"))
        .thenReturn("processedTestProjectId");
    Mockito.when(configProcessor.processVersion("testVersion")).thenReturn("processedTestVersion");

    List<Path> deployables = ImmutableList.of(Paths.get("some/path"), Paths.get("some/other/path"));
    DeployConfiguration deployConfig =
        new ConfigBuilder(deployMojo, configProcessor).buildDeployConfiguration(deployables);

    Assertions.assertEquals(deployables, deployConfig.getDeployables());
    Assertions.assertEquals("beta", deployConfig.getGcloudMode());
    Assertions.assertEquals("testBucket", deployConfig.getBucket());
    Assertions.assertEquals("testImageUrl", deployConfig.getImageUrl());
    Assertions.assertEquals("processedTestProjectId", deployConfig.getProjectId());
    Assertions.assertEquals(Boolean.FALSE, deployConfig.getPromote());
    Assertions.assertEquals(Boolean.FALSE, deployConfig.getStopPreviousVersion());
    Assertions.assertEquals("testServer", deployConfig.getServer());
    Assertions.assertEquals("processedTestVersion", deployConfig.getVersion());
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
