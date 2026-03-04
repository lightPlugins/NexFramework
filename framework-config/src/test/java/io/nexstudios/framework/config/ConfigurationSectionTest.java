package io.nexstudios.framework.config;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ConfigurationSectionTest {

  @Test
  void contains_get_set_scalar_and_defaults() {
    CommentedConfigurationNode root = CommentedConfigurationNode.root();
    ConfigurationSection s = new ConfigurationSection(root);

    assertFalse(s.contains("a"));
    assertEquals("x", s.getString("a", "x"));

    s.set("a", "b");
    assertTrue(s.contains("a"));
    assertEquals("b", s.getString("a", "x"));
    assertEquals(7, s.getInt("missing", 7));
  }

  @Test
  void getKeys_and_getValues_deep() {
    CommentedConfigurationNode root = CommentedConfigurationNode.root();
    ConfigurationSection s = new ConfigurationSection(root);

    s.set("a.b.c", 1);
    s.set("a.b.d", 2);
    s.set("x", true);

    Set<String> shallow = s.getKeys(false);
    assertTrue(shallow.contains("a"));
    assertTrue(shallow.contains("x"));
    assertFalse(shallow.contains("a.b.c"));

    Set<String> deep = s.getKeys(true);
    assertTrue(deep.contains("a"));
    assertTrue(deep.contains("a.b"));
    assertTrue(deep.contains("a.b.c"));
    assertTrue(deep.contains("a.b.d"));
    assertTrue(deep.contains("x"));

    Map<String, Object> vals = s.getValues(true);
    assertEquals(1, vals.get("a.b.c"));
    assertEquals(2, vals.get("a.b.d"));
    assertEquals(true, vals.get("x"));
  }

  @Test
  void getStringList_returnsOnlyStrings_andEmptyOnMissingOrNonList() {
    CommentedConfigurationNode root = CommentedConfigurationNode.root();
    ConfigurationSection s = new ConfigurationSection(root);

    assertEquals(List.of(), s.getStringList("missing"));

    s.set("notList", "abc");
    assertEquals(List.of(), s.getStringList("notList"));

    s.node().node("list").appendListNode().raw("a");
    s.node().node("list").appendListNode().raw(123);
    s.node().node("list").appendListNode().raw("b");

    assertEquals(List.of("a", "b"), s.getStringList("list"));
  }

  @Test
  void comments_roundtrip_on_nodes() {
    CommentedConfigurationNode root = CommentedConfigurationNode.root();
    ConfigurationSection s = new ConfigurationSection(root);

    s.set("a.b", 1);
    s.setComment("a.b", "hello");

    assertEquals("hello", s.getComment("a.b"));
  }
}