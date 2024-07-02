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

package com.google.cloud.tools.libraries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LibrariesTest {

  private JsonObject[] apis;
  private CookieHandler oldCookieHandler;

  @BeforeEach
  public void parseJson() {
    oldCookieHandler = CookieHandler.getDefault();
    // https://github.com/GoogleCloudPlatform/appengine-plugins-core/issues/822
    CookieHandler.setDefault(new CookieManager());

    JsonReaderFactory factory = Json.createReaderFactory(null);
    InputStream in = LibrariesTest.class.getResourceAsStream("libraries.json");
    JsonReader reader = factory.createReader(in);
    apis = reader.readArray().toArray(new JsonObject[0]);
  }

  @AfterEach
  public void tearDown() {
    CookieHandler.setDefault(oldCookieHandler);
  }

  @Test
  public void testJson() throws IOException {
    Assertions.assertTrue(apis.length > 0);
    for (int i = 0; i < apis.length; i++) {
      assertApi(apis[i]);
    }
  }

  private static final String[] statuses = {
    "early access", "alpha", "beta", "stable", "deprecated"
  };

  private static void assertApi(JsonObject api) throws IOException {
    String id = api.getString("id");
    Assertions.assertTrue(id.matches("[a-z]+"));
    Assertions.assertFalse(api.getString("serviceName").isEmpty());
    Assertions.assertFalse(api.getString("name").isEmpty());
    Assertions.assertFalse(api.getString("name").contains("Google"));
    Assertions.assertFalse(api.getString("description").isEmpty());
    String transports = api.getJsonArray("transports").getString(0);
    Assertions.assertTrue(
        "http".equals(transports) || "grpc".equals(transports),
        transports + " is not a recognized transport");
    assertReachable(api.getString("documentation"));
    try {
      assertReachable(api.getString("icon"));
    } catch (NullPointerException ex) {
      // no icon element to test
    }
    JsonArray clients = api.getJsonArray("clients");
    Assertions.assertFalse(clients.isEmpty());
    for (int i = 0; i < clients.size(); i++) {
      JsonObject client = (JsonObject) clients.get(i);
      String launchStage = client.getString("launchStage");
      assertThat(statuses, hasItemInArray(launchStage));
      try {
        assertReachable(client.getString("site"));
      } catch (NullPointerException ex) {
        // no site element to test
      }
      assertReachable(client.getString("apireference"));
      Assertions.assertTrue(client.getString("languageLevel").matches("1\\.\\d+\\.\\d+"));
      Assertions.assertFalse(client.getString("name").isEmpty());
      JsonString language = client.getJsonString("language");
      Assertions.assertNotNull(language, "Missing language in " + client.getString("name"));
      Assertions.assertEquals("java", language.getString());
      JsonObject mavenCoordinates = client.getJsonObject("mavenCoordinates");
      String version = mavenCoordinates.getString("version");
      Assertions.assertFalse(version.isEmpty());
      if (!version.endsWith(launchStage)) {
        Assertions.assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
      }
      Assertions.assertFalse(mavenCoordinates.getString("artifactId").isEmpty());
      Assertions.assertFalse(mavenCoordinates.getString("groupId").isEmpty());
      if (client.getString("source") != null) {
        assertReachable(client.getString("source"));
      }
    }
  }

  private static void assertReachable(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout(10000);
    connection.setRequestMethod("HEAD");
    try {
      Assertions.assertEquals(200, connection.getResponseCode(), "Could not reach " + url);
    } catch (SocketTimeoutException e) {
      Assertions.fail("Connection to '" + url + "' timed out.");
    }
  }

  @Test
  public void testDuplicates() {
    Map<String, String> apiCoordinates = Maps.newHashMap();
    Set<String> serviceNames = Sets.newHashSet();
    for (JsonObject api : apis) {
      String name = api.getString("name");
      String serviceName = api.getString("serviceName");
      if (apiCoordinates.containsKey(name)) {
        Assertions.fail("name: " + name + " is defined twice");
      }
      if (serviceNames.contains(serviceName)) {
        Assertions.fail("service name: " + serviceName + " is defined twice");
      }
      JsonObject coordinates =
          ((JsonObject) api.getJsonArray("clients").getFirst()).getJsonObject("mavenCoordinates");
      String mavenCoordinates =
          coordinates.getString("groupId") + ":" + coordinates.getString("artifactId");
      if (apiCoordinates.containsValue(mavenCoordinates)) {
        Assertions.fail(mavenCoordinates + " is defined twice");
      }
      apiCoordinates.put(name, mavenCoordinates);
      serviceNames.add(serviceName);
    }
  }

  @Test
  public void testServiceRoleMapping_hasNoDuplicateRoles() {
    for (JsonObject api : apis) {
      JsonArray serviceRoles = api.getJsonArray("serviceRoles");
      if (serviceRoles != null) {
        Set<String> roles = Sets.newHashSet();
        for (int i = 0; i < serviceRoles.size(); i++) {
          String role = serviceRoles.getString(i);
          if (roles.contains(role)) {
            Assertions.fail("Role: " + role + " is defined multiple times");
          }
          roles.add(role);
        }
      }
    }
  }

  @Test
  public void testVersionExists() throws IOException {
    for (JsonObject api : apis) {
      JsonObject coordinates =
          ((JsonObject) api.getJsonArray("clients").getFirst()).getJsonObject("mavenCoordinates");
      String repo =
          "https://repo1.maven.org/maven2/"
              + coordinates.getString("groupId").replace('.', '/')
              + "/"
              + coordinates.getString("artifactId")
              + "/"
              + coordinates.getString("version")
              + "/";
      assertReachable(repo);
    }
  }

  @Test
  public void testOrder() {
    List<String> names = new ArrayList<>();
    for (JsonObject api : apis) {
      names.add(api.getString("name"));
    }
    for (int i = 1; i < names.size(); i++) {
      String previous = names.get(i - 1).toLowerCase(Locale.US);
      String current = names.get(i).toLowerCase(Locale.US);
      Assertions.assertTrue(current.compareTo(previous) > 0, current + " < " + previous);
    }
  }
}
