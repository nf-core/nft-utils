package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OptionsTest {

  // StringBuilder stands in for a Groovy GString: a CharSequence, not a String
  private static final CharSequence GSTRING = new StringBuilder("tests/.nftignore");

  @Test
  void shouldReadGroovyStringsAsStrings() {
    Options opts = new Options(
      "f",
      Map.of("ignoreFile", GSTRING, "ignore", List.of(GSTRING)),
      "ignoreFile", "ignore");

    assertEquals("tests/.nftignore", opts.string("ignoreFile", null));
    assertEquals(List.of("tests/.nftignore"), opts.strings("ignore", List.of()));
  }

  @Test
  void shouldUseDefaultsForMissingOptions() {
    Options opts = new Options("f", null, "a");

    assertEquals("x", opts.string("a", "x"));
    assertEquals(true, opts.bool("a", true));
    assertEquals(3, opts.integer("a", 3));
    assertEquals(List.of("y"), opts.strings("a", List.of("y")));
    assertEquals(List.of(1), opts.list("a", Integer.class, List.of(1)));
  }

  @Test
  void shouldReadAnyValueAsString() {
    Options opts = new Options(
      "f", Map.of("extractDir", Path.of("/data/dbs")), "extractDir");

    assertEquals("/data/dbs", opts.string("extractDir", null));
  }

  @Test
  void shouldRejectUnknownOptionsAndWrongTypes() {
    IllegalArgumentException unknown = assertThrows(
      IllegalArgumentException.class,
      () -> new Options("f", Map.of("ignorefile", "x"), "ignoreFile"));
    assertEquals(
      "Unknown f() options: [ignorefile]; valid options: [ignoreFile]",
      unknown.getMessage());

    Options opts = new Options(
      "f", Map.of("relative", "yes", "ignore", "x"), "relative", "ignore");
    assertThrows(IllegalArgumentException.class, () -> opts.bool("relative", true));
    assertThrows(
      IllegalArgumentException.class, () -> opts.strings("ignore", List.of()));
  }
}
