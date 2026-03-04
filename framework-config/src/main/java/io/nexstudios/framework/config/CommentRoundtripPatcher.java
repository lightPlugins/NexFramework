package io.nexstudios.framework.config;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort YAML comment roundtrip patcher.
 *
 * It re-injects:
 * - file header comments (leading comment block at top of file)
 * - comment blocks directly above "key:" lines
 *
 * Priority:
 * 1) original file comments
 * 2) defaults file comments (for newly added keys)
 *
 * Notes:
 * - This is a heuristic line-based patcher, not a full YAML roundtrip parser.
 * - It intentionally ignores list item comments and complex inline cases.
 */
public final class CommentRoundtripPatcher {

  private CommentRoundtripPatcher() {}

  private static final Pattern KEY_LINE = Pattern.compile("^(\\s*)([^\\s:#][^:#]*?)\\s*:(.*)$");

  public static String patch(String originalYaml, String defaultsYaml, String generatedYaml) {
    Objects.requireNonNull(originalYaml, "originalYaml");
    Objects.requireNonNull(defaultsYaml, "defaultsYaml");
    Objects.requireNonNull(generatedYaml, "generatedYaml");

    CommentIndex original = CommentIndex.parse(originalYaml);
    CommentIndex defaults = CommentIndex.parse(defaultsYaml);

    // Merge comment sources: original wins, defaults fills gaps
    Map<String, List<String>> commentsByPath = new LinkedHashMap<>(defaults.commentsByPath);
    commentsByPath.putAll(original.commentsByPath);

    List<String> out = new ArrayList<>();

    List<String> genLines = splitLinesPreserve(generatedYaml);

    // Header patch
    List<String> headerToInject = !original.headerComments.isEmpty()
        ? original.headerComments
        : defaults.headerComments;

    int firstContentIndex = firstNonEmptyIndex(genLines);
    if (!headerToInject.isEmpty()) {
      boolean generatedAlreadyHasHeader = hasHeaderComments(genLines);
      if (!generatedAlreadyHasHeader) {
        out.addAll(headerToInject);
        // Ensure a blank line after header if the generated file starts with content immediately
        if (firstContentIndex == 0 && (out.isEmpty() || !out.getLast().isBlank())) {
          out.add("");
        }
      }
    }

    // Key-by-key patch
    Deque<Frame> stack = new ArrayDeque<>();
    int lastIndent = -1;

    for (String line : genLines) {
      Matcher m = KEY_LINE.matcher(line);

      if (!m.matches() || isListItemKeyLine(line)) {
        out.add(line);
        continue;
      }

      int indent = m.group(1).length();
      String key = normalizeKey(m.group(2));

      // Maintain indentation stack
      if (indent <= lastIndent) {
        while (!stack.isEmpty() && stack.peekLast().indent >= indent) {
          stack.removeLast();
        }
      }
      lastIndent = indent;

      stack.addLast(new Frame(indent, key));
      String path = buildPath(stack);

      List<String> toInject = commentsByPath.get(path);
      if (toInject != null && !toInject.isEmpty()) {
        if (!alreadyHasCommentBlockAbove(out)) {
          for (String c : toInject) {
            // Keep indentation consistent for injected comment blocks
            out.add(" ".repeat(indent) + c.stripLeading());
          }
        }
      }

      out.add(line);
    }

    return joinLines(out);
  }

  private static boolean isListItemKeyLine(String line) {
    String trimmed = line.stripLeading();
    return trimmed.startsWith("- ");
  }

  private static boolean alreadyHasCommentBlockAbove(List<String> out) {
    for (int j = out.size() - 1; j >= 0; j--) {
      String s = out.get(j);
      if (s.isBlank()) continue;
      return s.stripLeading().startsWith("#");
    }
    return false;
  }

  private static boolean hasHeaderComments(List<String> genLines) {
    for (String line : genLines) {
      if (line.isBlank()) continue;
      String t = line.stripLeading();
      return t.startsWith("#");
    }
    return false;
  }

  private static int firstNonEmptyIndex(List<String> lines) {
    for (int i = 0; i < lines.size(); i++) {
      if (!lines.get(i).isBlank()) return i;
    }
    return lines.size();
  }

  private static String normalizeKey(String rawKey) {
    return rawKey.trim();
  }

  private static String buildPath(Deque<Frame> stack) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Frame f : stack) {
      if (!first) sb.append('.');
      sb.append(f.key);
      first = false;
    }
    return sb.toString();
  }

  private static List<String> splitLinesPreserve(String s) {
    // Normalize line endings, keep empty last line behavior stable
    String normalized = s.replace("\r\n", "\n").replace('\r', '\n');
    return new ArrayList<>(Arrays.asList(normalized.split("\n", -1)));
  }

  private static String joinLines(List<String> lines) {
    return String.join("\n", lines);
  }

  private record Frame(int indent, String key) {}

  private static final class CommentIndex {
    final List<String> headerComments;
    final Map<String, List<String>> commentsByPath;

    private CommentIndex(List<String> headerComments, Map<String, List<String>> commentsByPath) {
      this.headerComments = headerComments;
      this.commentsByPath = commentsByPath;
    }

    static CommentIndex parse(String yaml) {
      List<String> lines = splitLinesPreserve(yaml);

      List<String> header = new ArrayList<>();
      Map<String, List<String>> byPath = new LinkedHashMap<>();

      // Header collection: contiguous comment/blank lines at top, stopping at first key/content.
      int idx = 0;
      while (idx < lines.size()) {
        String line = lines.get(idx);
        String t = line.stripLeading();
        if (t.startsWith("#") || line.isBlank()) {
          if (t.startsWith("#")) {
            header.add(t);
          } else if (!header.isEmpty()) {
            // preserve blank lines inside header block only if header has started
            header.add("");
          }
          idx++;
          continue;
        }
        break;
      }
      // Trim trailing blank lines in header
      while (!header.isEmpty() && header.getLast().isBlank()) {
        header.removeLast();
      }

      Deque<Frame> stack = new ArrayDeque<>();
      int lastIndent = -1;

      List<String> pendingComments = new ArrayList<>();

      for (String line : lines) {
        String t = line.stripLeading();

        if (t.startsWith("#")) {
          pendingComments.add(t);
          continue;
        }

        if (line.isBlank()) {
          if (!pendingComments.isEmpty()) {
            pendingComments.add("");
          }
          continue;
        }

        Matcher m = KEY_LINE.matcher(line);
        if (!m.matches() || isListItemKeyLine(line)) {
          pendingComments.clear();
          continue;
        }

        int indent = m.group(1).length();
        String key = normalizeKey(m.group(2));

        if (indent <= lastIndent) {
          while (!stack.isEmpty() && stack.peekLast().indent >= indent) {
            stack.removeLast();
          }
        }
        lastIndent = indent;

        stack.addLast(new Frame(indent, key));
        String path = buildPath(stack);

        // attach pending comment block to this key
        if (!pendingComments.isEmpty()) {
          // Trim trailing blanks in the pending block
          while (!pendingComments.isEmpty() && pendingComments.getLast().isBlank()) {
            pendingComments.removeLast();
          }
          if (!pendingComments.isEmpty()) {
            byPath.put(path, List.copyOf(pendingComments));
          }
          pendingComments.clear();
        }
      }

      return new CommentIndex(List.copyOf(header), byPath);
    }
  }
}