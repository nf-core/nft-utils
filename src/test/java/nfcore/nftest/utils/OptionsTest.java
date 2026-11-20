package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class OptionsTest {

  // StringBuilder stands in for a Groovy GString: a CharSequence, not a String
  private static final CharSequence GSTRING = new StringBuilder("tests/.nftignore");

  @Test
  void shouldReadGroovyStringsAsStrings() {
    Options opts = new Options(
      "f",
      Map.of("ignoreFile", GSTRING, "ignore", List.of(GSTRING)),
      Set.of("ignoreFile", "ignore"));

    assertEquals("tests/.nftignore", opts.string("ignoreFile", null));
    assertEquals(List.of("tests/.nftignore"), opts.strings("ignore", List.of()));
  }

  @Test
  void shouldUseDefaultsForMissingOptions() {
    Options opts = new Options("f", null, Set.of("a"));

    assertEquals("x", opts.string("a", "x"));
    assertEquals(true, opts.bool("a", true));
    assertEquals(3, opts.integer("a", 3));
    assertEquals(List.of("y"), opts.strings("a", List.of("y")));
  }

  @Test
  void shouldRejectUnknownOptionsAndWrongTypes() {
    IllegalArgumentException unknown = assertThrows(
      IllegalArgumentException.class,
      () -> new Options("f", Map.of("ignorefile", "x"), Set.of("ignoreFile")));
    assertEquals(
      "Unknown f() options: [ignorefile]; valid options: [ignoreFile]",
      unknown.getMessage());

    Options opts = new Options(
      "f", Map.of("relative", "yes", "ignore", "x"),
      Set.of("relative", "ignore"));
    assertThrows(IllegalArgumentException.class, () -> opts.bool("relative", true));
    assertThrows(
      IllegalArgumentException.class, () -> opts.strings("ignore", List.of()));
  }
}
