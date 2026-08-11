package nf_core.nf.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

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
}
