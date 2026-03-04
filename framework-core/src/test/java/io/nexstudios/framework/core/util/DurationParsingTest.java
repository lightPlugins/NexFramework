package io.nexstudios.framework.core.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

final class DurationParsingTest {

  @Test
  void parse_singleUnit() {
    assertEquals(Duration.ofMillis(150), DurationParsing.parse("150ms"));
    assertEquals(Duration.ofSeconds(10), DurationParsing.parse("10s"));
    assertEquals(Duration.ofMinutes(5), DurationParsing.parse("5m"));
    assertEquals(Duration.ofHours(2), DurationParsing.parse("2h"));
    assertEquals(Duration.ofDays(3), DurationParsing.parse("3d"));
    assertEquals(Duration.ofDays(7), DurationParsing.parse("1w"));
  }

  @Test
  void parse_multipleUnits_concatenated_and_whitespace() {
    assertEquals(Duration.ofMinutes(90), DurationParsing.parse("1h30m"));
    assertEquals(Duration.ofMinutes(90), DurationParsing.parse("  1h 30m  "));
    assertEquals(Duration.ofMillis(10 * 60_000L + 30_000L), DurationParsing.parse("10m30s"));
    assertEquals(Duration.ofMillis(1_000L + 250L), DurationParsing.parse("1s250ms"));
  }

  @Test
  void parse_isCaseInsensitiveForUnits() {
    assertEquals(Duration.ofMinutes(5), DurationParsing.parse("5M"));
    assertEquals(Duration.ofMillis(1_000L + 2L), DurationParsing.parse("1S2mS"));
  }

  @Test
  void parse_rejectsNullAndEmpty() {
    var exNull = assertThrows(IllegalArgumentException.class, () -> DurationParsing.parse(null));
    assertTrue(exNull.getMessage().contains("null"));

    var exEmpty = assertThrows(IllegalArgumentException.class, () -> DurationParsing.parse("   "));
    assertTrue(exEmpty.getMessage().contains("empty"));
  }

  @Test
  void parse_rejectsInvalidFormat_andReportsIndex() {
    var ex1 = assertThrows(IllegalArgumentException.class, () -> DurationParsing.parse("h10"));
    assertTrue(ex1.getMessage().contains("index 0"));

    var ex2 = assertThrows(IllegalArgumentException.class, () -> DurationParsing.parse("10m-5s"));
    assertTrue(ex2.getMessage().contains("index 3"));
  }

  @Test
  void parse_throwsOnOverflow() {
    assertThrows(ArithmeticException.class, () -> DurationParsing.parse("9223372036854776s"));
  }
}