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

package com.google.cloud.tools.io;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test for {@link FileUtil} */
public class FileUtilTest {

  @TempDir
  public File testDir;

  @Test
  public void testCopyDirectory_nested() throws IOException {
    Path src = newFolder(testDir, "src").toPath();
    Path dest = newFolder(testDir, "dest").toPath();

    Path rootFile = Files.createFile(src.resolve("root.file"));
    Path subDir = Files.createDirectory(src.resolve("sub"));
    Path subFile = Files.createFile(subDir.resolve("sub.file"));

    FileUtil.copyDirectory(src, dest);

    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(rootFile))));
    Assertions.assertTrue(Files.isDirectory(dest.resolve(src.relativize(subDir))));
    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(subFile))));
  }

  @Test
  public void testCopyDirectory_posixPermissions() throws IOException {
    assumeTrue(!System.getProperty("os.name").startsWith("Windows"));

    Set<PosixFilePermission> permission = Sets.newHashSet();
    permission.add(PosixFilePermission.OWNER_READ);
    permission.add(PosixFilePermission.GROUP_READ);
    permission.add(PosixFilePermission.OTHERS_READ);
    permission.add(PosixFilePermission.OTHERS_EXECUTE);
    permission.add(PosixFilePermission.OTHERS_WRITE);

    Path src = newFolder(testDir, "src").toPath();
    Path dest = newFolder(testDir, "dest").toPath();

    Path rootFile = Files.createFile(src.resolve("root1.file"));
    Assertions.assertNotEquals(
        Files.getPosixFilePermissions(rootFile),
        permission,
        "This test is useless - modified permissions are default permissions");
    Files.setPosixFilePermissions(rootFile, permission);

    FileUtil.copyDirectory(src, dest);

    Assertions.assertEquals(
        permission, Files.getPosixFilePermissions(dest.resolve(src.relativize(rootFile))));
  }

  @Test
  public void testCopyDirectory_aclPermissions() {
    assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    // TODO : write windows tests
  }

  @Test
  public void testCopyDirectory_badArgs() throws IOException {
    Path dir = newFolder(testDir, "junit").toPath();
    Path file = File.createTempFile("junit", null, testDir).toPath();

    try {
      FileUtil.copyDirectory(dir, file);
      Assertions.fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertNotNull(ex.getMessage());
    }

    try {
      FileUtil.copyDirectory(file, dir);
      Assertions.fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertNotNull(ex.getMessage());
    }

    try {
      FileUtil.copyDirectory(dir, dir);
      Assertions.fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testCopyDirectory_childPath() throws IOException {
    Path src = newFolder(testDir, "junit").toPath();
    Path dest = Files.createDirectory(src.resolve("subdir"));

    try {
      FileUtil.copyDirectory(src, dest);
      Assertions.fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals("destination is child of source", ex.getMessage());
    }
  }

  @Test
  public void testCopyDirectory_sameFile() throws IOException {
    Path src = newFolder(testDir, "junit").toPath();
    Path dest = Paths.get(src.toString(), "..", src.getFileName().toString());

    try {
      FileUtil.copyDirectory(src, dest);
      Assertions.fail();
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals("Source and destination are the same", ex.getMessage());
    }
  }

  @Test
  public void testWeirdNames() throws IOException {
    Path src = newFolder(testDir, "funny").toPath();
    Path dest = newFolder(testDir, "funny2").toPath();
    FileUtil.copyDirectory(src, dest);
  }

  @Test
  public void testCopyDirectory_excludes() throws IOException {
    Path src = newFolder(testDir, "src").toPath();
    Path dest = newFolder(testDir, "dest").toPath();
    Path destExcludes = newFolder(testDir, "dest-with-excludes").toPath();

    Path rootFile = Files.createFile(src.resolve("root.file"));
    Path subDir = Files.createDirectory(src.resolve("sub"));
    Path subFile = Files.createFile(subDir.resolve("sub.file"));
    Path excludedFile = Files.createFile(src.resolve("excluded.file"));
    Path excludedSubDir = Files.createDirectory(src.resolve("excluded"));
    Path autoExcludedSubFile = Files.createFile(excludedSubDir.resolve("auto.excluded.file"));

    // control group
    FileUtil.copyDirectory(src, dest);
    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(rootFile))));
    Assertions.assertTrue(Files.isDirectory(dest.resolve(src.relativize(subDir))));
    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(subFile))));
    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(excludedFile))));
    Assertions.assertTrue(Files.isDirectory(dest.resolve(src.relativize(excludedSubDir))));
    Assertions.assertTrue(Files.isRegularFile(dest.resolve(src.relativize(autoExcludedSubFile))));

    // test group
    FileUtil.copyDirectory(src, destExcludes, ImmutableList.of(excludedSubDir, excludedFile));
    Assertions.assertTrue(Files.isRegularFile(destExcludes.resolve(src.relativize(rootFile))));
    Assertions.assertTrue(Files.isDirectory(destExcludes.resolve(src.relativize(subDir))));
    Assertions.assertTrue(Files.isRegularFile(destExcludes.resolve(src.relativize(subFile))));
    Assertions.assertFalse(Files.exists(destExcludes.resolve(src.relativize(excludedFile))));
    Assertions.assertFalse(Files.exists(destExcludes.resolve(src.relativize(excludedSubDir))));
    Assertions.assertFalse(Files.exists(destExcludes.resolve(src.relativize(autoExcludedSubFile))));
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
