package io.nexstudios.framework.core.key;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a namespaced key consisting of a namespace and a key.
 *
 * A {@code NexKey} instance is immutable and ensures that the namespace and key conform
 * to specific formatting rules:
 * - The namespace allows characters matching the regular expression `[a-z0-9._-]+`.
 * - The key allows characters matching the regular expression `[a-z0-9/._-]+`.
 *
 * The namespace and key values are normalized to lower case and trimmed of any leading
 * or trailing whitespace. If the provided values do not meet the formatting requirements,
 * an {@code IllegalArgumentException} is thrown.
 */
public record NexKey(String namespace, String key) {

  private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9._-]+$");
  private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9/._-]+$");

  public NexKey {
    namespace = normalizeNamespace(namespace);
    key = normalizeKey(key);

    if (!NAMESPACE_PATTERN.matcher(namespace).matches()) {
      throw new IllegalArgumentException(
          "Invalid namespace: '" + namespace + "' (allowed: [a-z0-9._-]+)"
      );
    }
    if (!KEY_PATTERN.matcher(key).matches()) {
      throw new IllegalArgumentException(
          "Invalid key: '" + key + "' (allowed: [a-z0-9/._-]+)"
      );
    }
  }

  public static NexKey of(String namespace, String key) {
    return new NexKey(namespace, key);
  }

  /**
   * Creates a new {@code NexKey} instance using the provided owner name and key.
   * The owner name is normalized before being used as the namespace.
   *
   * @param ownerName the name of the owner to derive the namespace from, must not be null
   * @param key the key associated with the namespace, must not be null
   * @return a new {@code NexKey} instance with the normalized owner name as the namespace
   *         and the provided key.
   * @throws NullPointerException if {@code ownerName} or {@code key} is null
   * @throws IllegalArgumentException if the normalized {@code ownerName} or {@code key} is empty
   */
  public static NexKey ofOwnerName(String ownerName, String key) {
    Objects.requireNonNull(ownerName, "ownerName");
    return new NexKey(normalizeNamespace(ownerName), key);
  }

  public static NexKey fromString(String input) {
    Objects.requireNonNull(input, "input");

    String trimmedInput = input.trim();
    if (trimmedInput.isEmpty()) {
      throw new IllegalArgumentException("Invalid namespaced key: input is empty");
    }

    int colonIndex = trimmedInput.indexOf(':');
    if (colonIndex <= 0 || colonIndex == trimmedInput.length() - 1) {
      throw new IllegalArgumentException(
          "Invalid namespaced key: '" + input + "' (expected format: namespace:key)"
      );
    }

    String parsedNamespace = trimmedInput.substring(0, colonIndex);
    String parsedKey = trimmedInput.substring(colonIndex + 1);

    return new NexKey(parsedNamespace, parsedKey);
  }

  public static String normalizeNamespace(String namespace) {
    Objects.requireNonNull(namespace, "namespace");

    String normalizedNamespace = namespace.trim();
    if (normalizedNamespace.isEmpty()) {
      throw new IllegalArgumentException("Namespace must not be empty");
    }

    return normalizedNamespace.toLowerCase(Locale.ROOT);
  }

  public static String normalizeKey(String key) {
    Objects.requireNonNull(key, "key");

    String normalizedKey = key.trim();
    if (normalizedKey.isEmpty()) {
      throw new IllegalArgumentException("Key must not be empty");
    }

    return normalizedKey.toLowerCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return namespace + ":" + key;
  }
}