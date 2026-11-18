package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UtilsTest {

  @Test
  void shouldGetFileExtension() {
    Path path = Paths.get("/tmp/example.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldConvertExtensionToLowerCase() {
    Path path = Paths.get("/tmp/example.BAM");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldGetExtensionFromFileWithMultipleDots() {
    Path path = Paths.get("/tmp/sample.test.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringWhenFileHasNoExtension() {
    Path path = Paths.get("/tmp/example");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringForHiddenFile() {
    Path path = Paths.get("/tmp/.example");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringWhenFilenameEndsWithDot() {
    Path path = Paths.get("/tmp/example.");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldIgnoreDotsInParentDirectories() {
    Path path = Paths.get("/tmp/some.directory/example.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldIgnoreGzExtension() {
    Path path = Paths.get("/tmp/some.directory/example.vcf.gz");
    assertEquals("vcf", Utils.getExtension(path, false));
    assertEquals("gz", Utils.getExtension(path, true));
  }

  @Test
  void shouldIgnoreNestedGzExtension() {
    Path path = Paths.get("/tmp/some.directory/example.vcf.gz.gz");
    assertEquals("vcf", Utils.getExtension(path, false));
    assertEquals("gz", Utils.getExtension(path, true));
  }

  @Test
  void shouldExcludeLinesByNumber(@TempDir final Path dir) throws IOException {
    Path file = Files.write(dir.resolve("in.txt"), List.of("a", "b", "c", "d"));
    assertEquals(
      List.of("b", "d"),
      Utils.excludeLines(file.toString(), List.of(3, 1), List.of())
    );
    assertEquals(
      List.of("a", "b", "c", "d"),
      Utils.excludeLines(file.toString(), List.of(), List.of())
    );
    assertEquals(
      List.of("a"),
      Utils.excludeLines(file.toString(), List.of(4), List.of("b", "c"))
    );
  }

  @Test
  void shouldRejectOutOfRangeLineNumbers(@TempDir final Path dir)
      throws IOException {
    Path file = Files.write(dir.resolve("in.txt"), List.of("a", "b"));
    assertThrows(
      IllegalArgumentException.class,
      () -> Utils.excludeLines(file.toString(), List.of(0), List.of())
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> Utils.excludeLines(file.toString(), List.of(3), List.of())
    );
  }
}
