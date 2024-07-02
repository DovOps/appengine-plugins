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

package com.google.cloud.tools.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceNameValidatorTest {

  @Test
  public void testDomain() {
    Assertions.assertFalse(ServiceNameValidator.validate("google.com:mystore"));
  }

  @Test
  public void testPartition() {
    Assertions.assertFalse(ServiceNameValidator.validate("s~google.com:mystore"));
  }

  @Test
  public void testOneWord() {
    Assertions.assertTrue(ServiceNameValidator.validate("word"));
  }

  @Test
  public void testUpperCase() {
    Assertions.assertTrue(ServiceNameValidator.validate("WORD"));
  }

  @Test
  public void testLongWord() {
    boolean validate =
        ServiceNameValidator.validate(
            """
            012345678901234567890123456789012345678901234567890123456789\
            012345678901234567890123456789012345678901234567890\
            """);
    Assertions.assertFalse(validate);
  }

  @Test
  public void testContainsSpace() {
    Assertions.assertFalse(ServiceNameValidator.validate("com google eclipse"));
  }

  @Test
  public void testEmptyString() {
    Assertions.assertFalse(ServiceNameValidator.validate(""));
  }

  @Test
  public void testNull() {
    Assertions.assertFalse(ServiceNameValidator.validate(null));
  }

  @Test
  public void testBeginsWithHyphen() {
    Assertions.assertFalse(ServiceNameValidator.validate("-foo"));
  }

  @Test
  public void testEndsWithHyphen() {
    Assertions.assertFalse(ServiceNameValidator.validate("-bar"));
  }

  @Test
  public void testContainsHyphen() {
    Assertions.assertTrue(ServiceNameValidator.validate("foo-bar"));
  }
}
