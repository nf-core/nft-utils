package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
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
  void shouldReadFilesAndPathsAsStrings() {
    Options opts = new Options(
      "f",
      Map.of("extractDir", Path.of("/data/dbs"),
        "ignore", List.of(new File("/a.txt"), Path.of("/b.txt"))),
      "extractDir", "ignore");

    assertEquals("/data/dbs", opts.string("extractDir", null));
    assertEquals(List.of("/a.txt", "/b.txt"), opts.strings("ignore", List.of()));
  }

  @Test
  void shouldFindGroovyStringKeys() {
    Options opts = new Options(
      "f", Map.of(new StringBuilder("ignoreFile"), "x"), "ignoreFile");

    assertEquals("x", opts.string("ignoreFile", null));
  }

  @Test
  void shouldReadWholeNumbersAsIntegers() {
    Options opts = new Options(
      "f", Map.of("a", 3L, "b", 3.5, "c", "3"), "a", "b", "c");

    assertEquals(3, opts.integer("a", 0));
    assertThrows(IllegalArgumentException.class, () -> opts.integer("b", 0));
    assertThrows(IllegalArgumentException.class, () -> opts.integer("c", 0));
  }

  @Test
  void shouldUseDefaultsForNullValues() {
    Map<String, Object> options = new HashMap<>();
    options.put("a", null);
    Options opts = new Options("f", options, "a");

    assertEquals("x", opts.string("a", "x"));
    assertEquals(List.of("y"), opts.strings("a", List.of("y")));
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

    Options wrong = new Options(
      "f", Map.of("ignoreFile", true, "ignore", List.of("a", 1)),
      "ignoreFile", "ignore");
    assertThrows(
      IllegalArgumentException.class, () -> wrong.string("ignoreFile", null));
    assertThrows(
      IllegalArgumentException.class, () -> wrong.strings("ignore", List.of()));
  }

  @Test
  void shouldNameTheKindOfKeyInErrors() {
    IllegalArgumentException unknown = assertThrows(
      IllegalArgumentException.class,
      () -> new Options(
        "nfcoreInstall", "module key", Map.of("nmae", "x"), "name"));
    assertEquals(
      "Unknown nfcoreInstall() module keys: [nmae]; valid module keys: [name]",
      unknown.getMessage());
  }
}
