package io.nexstudios.framework.core.util;

import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses human-readable duration strings into {@link Duration} objects.
 */
@NoArgsConstructor
public final class DurationParsing {

  // Token: number + unit, e.g. "10d", "20h", "34m", "150ms"
  private static final Pattern TOKEN = Pattern.compile("(\\d+)(ms|s|m|h|d|w)", Pattern.CASE_INSENSITIVE);

  /**
   * Parses a human-readable duration string into a {@link Duration} object.
   *
   * @param input the duration string (e.g., "1h30m", "5d", "10m30s").
   * @return the parsed {@link Duration}.
   * @throws IllegalArgumentException if the input is null, empty, or invalid.
   */
  public static Duration parse(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Invalid duration: null");
    }

    String s = input.trim();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("Invalid duration: empty");
    }

    // Allow "10d 20h 34m"
    s = s.replaceAll("\\s+", "");

    Matcher m = TOKEN.matcher(s);

    long totalMillis = 0L;
    int pos = 0;

    while (pos < s.length()) {
      if (!m.find(pos) || m.start() != pos) {
        throw new IllegalArgumentException(
            "Invalid duration format at index " + pos + " in \"" + input + "\" " +
                "(expected e.g. 10d20h34m, 5m, 1h30m)"
        );
      }

      long amount = parsePositiveLong(m.group(1));
      String unit = m.group(2).toLowerCase(Locale.ROOT);

      long millis = toMillis(amount, unit);

      totalMillis = Math.addExact(totalMillis, millis);
      pos = m.end();
    }

    return Duration.ofMillis(totalMillis);
  }

  /**
   * Parses the given string into a positive long value.
   *
   * @param raw the string to parse.
   * @return the parsed positive long value.
   * @throws IllegalArgumentException if the string is not a valid positive number.
   */
  private static long parsePositiveLong(String raw) {
    try {
      long v = Long.parseLong(raw);
      if (v < 0) {
        throw new IllegalArgumentException("Invalid " + "duration amount" + ": " + raw);
      }
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid " + "duration amount" + ": " + raw, e);
    }
  }

  /**
   * Converts a time amount and unit into milliseconds.
   *
   * @param amount the time value.
   * @param unit   the unit of time (e.g., "ms", "s", "m", "h", "d", "w").
   * @return the equivalent time in milliseconds.
   * @throws IllegalArgumentException if the unit is unknown.
   */
  private static long toMillis(long amount, String unit) {
    return switch (unit) {
      case "ms" -> amount;
      case "s" -> Math.multiplyExact(amount, 1_000L);
      case "m" -> Math.multiplyExact(amount, 60_000L);
      case "h" -> Math.multiplyExact(amount, 3_600_000L);
      case "d" -> Math.multiplyExact(amount, 86_400_000L);
      case "w" -> Math.multiplyExact(amount, 604_800_000L);
      default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
    };
  }
}