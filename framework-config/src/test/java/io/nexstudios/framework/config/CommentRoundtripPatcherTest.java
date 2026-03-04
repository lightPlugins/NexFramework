package io.nexstudios.framework.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CommentRoundtripPatcherTest {

  @Test
  void patch_injectsHeaderFromOriginal_ifGeneratedHasNoHeader() {
    String original = """
        # original header
        # line2
        a: 1
        """;
    String defaults = """
        # defaults header
        a: 1
        """;
    String generated = """
        a: 1
        """;

    String patched = CommentRoundtripPatcher.patch(original, defaults, generated);

    assertTrue(patched.startsWith("# original header\n# line2\n"));
    assertTrue(patched.contains("\na: 1\n") || patched.endsWith("\na: 1"));
  }

  @Test
  void patch_prefersOriginalKeyComment_overDefaults() {
    String original = """
        root:
          # original comment
          key: 1
        """;
    String defaults = """
        root:
          # defaults comment
          key: 1
        """;
    String generated = """
        root:
          key: 1
        """;

    String patched = CommentRoundtripPatcher.patch(original, defaults, generated);

    assertTrue(patched.contains("  # original comment\n  key: 1"));
    assertFalse(patched.contains("defaults comment"));
  }

  @Test
  void patch_usesDefaultsComment_forNewKeys() {
    String original = """
        root:
          key: 1
        """;
    String defaults = """
        root:
          # defaults comment
          newKey: 2
        """;
    String generated = """
        root:
          key: 1
          newKey: 2
        """;

    String patched = CommentRoundtripPatcher.patch(original, defaults, generated);

    assertTrue(patched.contains("  # defaults comment\n  newKey: 2"));
  }
}