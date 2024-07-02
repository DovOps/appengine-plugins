/*
 * Copyright 2016 Google LLC.
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

package com.google.cloud.tools.appengine.operations.cloudsdk.internal.args;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link GcloudArgs} */
public class GcloudArgsTest {

  @Test
  public void testGet_string() {
    assertEquals(Arrays.asList("--name1", "value1"), GcloudArgs.get("name1", "value1"));
    assertEquals(Arrays.asList("--name2", "value2"), GcloudArgs.get("name2", "value2"));
  }

  @Test
  public void testGet_boolean() {
    assertEquals(Collections.singletonList("--name1"), GcloudArgs.get("name1", true));
    assertEquals(Collections.singletonList("--name2"), GcloudArgs.get("name2", true));

    assertEquals(Collections.singletonList("--no-name1"), GcloudArgs.get("name1", false));
    assertEquals(Collections.singletonList("--no-name2"), GcloudArgs.get("name2", false));
  }

  @Test
  public void testGet_integer() {
    assertEquals(Arrays.asList("--name1", "1"), GcloudArgs.get("name1", 1));
    assertEquals(Arrays.asList("--name2", "2"), GcloudArgs.get("name2", 2));
  }

  @TempDir
  public File tmpDir;

  @Test
  public void testGet_path() throws IOException {
    Path file1 = File.createTempFile("file1", null, tmpDir).toPath();
    Path file2 = File.createTempFile("file2", null, tmpDir).toPath();
    assertEquals(
        Arrays.asList("--name1", file1.toAbsolutePath().toString()),
        GcloudArgs.get("name1", file1));
    assertEquals(
        Arrays.asList("--name2", file2.toAbsolutePath().toString()),
        GcloudArgs.get("name2", file2));
  }

  @Test
  public void testKeyValues() {
    Map<String, Double> versionToTrafficSplitMapping =
        new LinkedHashMap<>(); // Preserve the order for the assertion
    versionToTrafficSplitMapping.put("v1", 0.2);
    versionToTrafficSplitMapping.put("v2", 0.3);
    versionToTrafficSplitMapping.put("v3", 0.5);

    assertEquals(
        Collections.singletonList("v1=0.2,v2=0.3,v3=0.5"),
        GcloudArgs.get(versionToTrafficSplitMapping));

    assertEquals(Collections.emptyList(), GcloudArgs.get(Collections.emptyMap()));
  }
}
