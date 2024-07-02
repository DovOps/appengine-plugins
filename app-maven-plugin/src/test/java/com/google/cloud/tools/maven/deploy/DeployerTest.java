package com.google.cloud.tools.maven.deploy;

import static org.mockito.Mockito.times;

import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import com.google.cloud.tools.appengine.configuration.DeployProjectConfigurationConfiguration;
import com.google.cloud.tools.appengine.operations.Deployment;
import com.google.cloud.tools.maven.cloudsdk.CloudSdkAppEngineFactory;
import com.google.cloud.tools.maven.deploy.AppDeployer.ConfigBuilder;
import com.google.cloud.tools.maven.stage.AppEngineWebXmlStager;
import com.google.cloud.tools.maven.stage.AppYamlStager;
import com.google.cloud.tools.maven.stage.Stager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeployerTest {

  @TempDir
  public File tempFolder;

  @Mock private ConfigProcessor configProcessor;

  @Mock private ConfigBuilder configBuilder;
  @Mock private Stager stager;
  @Mock private AbstractDeployMojo deployMojo;

  private Path stagingDirectory;
  @Mock private CloudSdkAppEngineFactory appEngineFactory;
  @Mock private Deployment appEngineDeployment;
  @Mock private Path appengineDirectory;
  @Mock private DeployConfiguration deployConfiguration;
  @Mock private DeployProjectConfigurationConfiguration deployProjectConfigurationConfiguration;

  @InjectMocks private AppDeployer testDeployer;

  @BeforeEach
  public void setup() throws IOException {
    stagingDirectory = newFolder(tempFolder, "staging").toPath();

    Mockito.when(deployMojo.getStagingDirectory()).thenReturn(stagingDirectory);
    Mockito.when(deployMojo.getAppEngineFactory()).thenReturn(appEngineFactory);
  }

  @Test
  public void testNewDeployer_appengineWebXml() throws MojoExecutionException {
    Mockito.when(deployMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    Mockito.when(deployMojo.isAppEngineWebXmlBased()).thenReturn(true);
    Mockito.when(deployMojo.getArtifact()).thenReturn(tempFolder.toPath());

    AppDeployer deployer = (AppDeployer) new Deployer.Factory().newDeployer(deployMojo);
    Assertions.assertEquals(
        deployMojo.getStagingDirectory().resolve("WEB-INF").resolve("appengine-generated"),
        deployer.appengineDirectory);
    Assertions.assertEquals(AppEngineWebXmlStager.class, deployer.stager.getClass());
  }

  @Test
  public void testNewDeployer_appYaml() throws MojoExecutionException, IOException {
    Path appengineDir = newFolder(tempFolder, "junit").toPath();
    Mockito.when(deployMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    Mockito.when(deployMojo.getArtifact()).thenReturn(tempFolder.toPath());
    Mockito.when(deployMojo.getAppEngineDirectory()).thenReturn(appengineDir);

    AppDeployer deployer = (AppDeployer) new Deployer.Factory().newDeployer(deployMojo);
    Mockito.verify(deployMojo, times(0)).getAppEngineWebXml();
    Assertions.assertEquals(appengineDir, deployer.appengineDirectory);
    Assertions.assertEquals(AppYamlStager.class, deployer.stager.getClass());
  }

  @Test
  public void testNewDeployer_noArtifact() {
    Mockito.when(deployMojo.isAppEngineCompatiblePackaging()).thenReturn(true);
    try {
      new Deployer.Factory().newDeployer(deployMojo);
      Assertions.fail();
    } catch (MojoExecutionException ex) {
      Assertions.assertEquals(
          """
          
          Could not determine appengine environment, did you package your application?
          Run 'mvn package appengine:deploy'\
          """,
          ex.getMessage());
    }
  }

  @Test
  public void testNewDeployer_noOpDeployer() throws MojoExecutionException {
    Mockito.when(deployMojo.isAppEngineCompatiblePackaging()).thenReturn(false);
    Assertions.assertEquals(
        NoOpDeployer.class, new Deployer.Factory().newDeployer(deployMojo).getClass());
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
