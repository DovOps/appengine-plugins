/*
 * Copyright 2019 Google LLC.
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

package com.google.cloud.tools.appengine.configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RunConfigurationTest {

  private RunConfiguration configuration;
  private final List<String> jvmFlags = new ArrayList<>();
  private final List<Path> services = new ArrayList<>();
  private List<String> additionalArguments = new ArrayList<>();
  private Map<String, String> environment = new HashMap<>();

  @BeforeEach
  public void setUp() {
    jvmFlags.add("foo");
    jvmFlags.add("bar");
    additionalArguments.add("foo");
    additionalArguments.add("bar");
    environment.put("foo", "fooval");
    environment.put("bar", "barval");
    configuration =
        RunConfiguration.builder(services)
            .additionalArguments(additionalArguments)
            .automaticRestart(true)
            .defaultGcsBucketName("defaultGcsBucketName")
            .environment(environment)
            .host("host")
            .jvmFlags(jvmFlags)
            .port(999)
            .projectId("projectId")
            .build();
  }

  @Test
  public void testJvmFlags_unset() {
    List<String> flags = RunConfiguration.builder(services).build().getJvmFlags();
    Assertions.assertEquals(0, flags.size());
  }

  @Test
  public void testBuilder() {
    testConfigValues();
  }

  @Test
  public void testToBuilder() {

    RunConfiguration.Builder builder = configuration.toBuilder();
    configuration = builder.build();

    testConfigValues();
  }

  private void testConfigValues() {
    Assertions.assertEquals(jvmFlags, configuration.getJvmFlags());
    Assertions.assertEquals(Integer.valueOf(999), configuration.getPort());
    Assertions.assertEquals(additionalArguments, configuration.getAdditionalArguments());
    Assertions.assertEquals(environment, configuration.getEnvironment());
    Assertions.assertEquals("host", configuration.getHost());
    Assertions.assertEquals("defaultGcsBucketName", configuration.getDefaultGcsBucketName());
    Assertions.assertEquals("projectId", configuration.getProjectId());
    Assertions.assertEquals(Boolean.TRUE, configuration.getAutomaticRestart());
  }
}
