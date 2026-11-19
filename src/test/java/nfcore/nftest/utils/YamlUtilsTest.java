package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlUtilsTest {

  @TempDir
  Path dir;

  @Test
  void shouldRemoveNextflowVersion() throws IOException {
    Path file = Files.writeString(dir.resolve("versions.yml"),
      "Workflow:\n  Nextflow: 26.10.0\n  nf-core/foo: 1.0\n");
    assertEquals(Map.of("Workflow", Map.of("nf-core/foo", 1.0)),
      Methods.removeNextflowVersion(file.toString()));
  }

  @Test
  void shouldReadEmptyFileAsEmptyMap() throws IOException {
    Path file = Files.writeString(dir.resolve("versions.yml"), "");
    assertEquals(Map.of(), Methods.removeNextflowVersion(file.toString()));
  }

  @Test
  void shouldThrowForMissingFile() {
    assertThrows(IOException.class, () -> Methods.removeNextflowVersion(
      dir.resolve("missing.yml").toString()));
  }

  @Test
  void shouldThrowForUnmatchedWildcard() {
    assertThrows(IOException.class, () -> Methods.removeNextflowVersion(
      dir.resolve("*_versions.yml").toString()));
  }

  @Test
  void shouldThrowForKeyWithoutNestedKeys() throws IOException {
    Path file = Files.writeString(dir.resolve("versions.yml"), "Workflow:\n");
    IOException e = assertThrows(IOException.class,
      () -> Methods.removeNextflowVersion(file.toString()));
    assertEquals(file + ": expected a map under 'Workflow'", e.getMessage());
  }

  @Test
  void shouldThrowForListAtTopLevel() throws IOException {
    Path file = Files.writeString(dir.resolve("versions.yml"), "- a\n");
    IOException e = assertThrows(IOException.class,
      () -> Methods.readYamlFile(file.toString()));
    assertEquals(file + ": expected a map", e.getMessage());
  }
}
