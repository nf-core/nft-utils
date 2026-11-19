package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.Assumptions;
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
  void shouldNotFollowSymlinksWhenDeletingDirectory(@TempDir final Path dir)
      throws IOException {
    Path outside = Files.createDirectories(dir.resolve("outside"));
    Files.writeString(outside.resolve("keep.txt"), "keep");
    Path inside = Files.createDirectories(dir.resolve("inside"));
    Files.createSymbolicLink(inside.resolve("link"), outside);

    Utils.deleteDirectory(inside.toFile());

    assertFalse(Files.exists(inside));
    assertTrue(Files.exists(outside.resolve("keep.txt")));
  }

  @Test
  void shouldThrowIOExceptionWhenDirectoryCannotBeListed(
      @TempDir final Path dir) throws IOException {
    Path locked = Files.createDirectories(dir.resolve("inside/locked"));
    Files.writeString(locked.resolve("file.txt"), "x");
    locked.toFile().setReadable(false);
    try {
      // Root, or a filesystem without permissions, can still read it
      Assumptions.assumeFalse(Files.isReadable(locked));
      assertThrows(IOException.class,
        () -> Utils.deleteDirectory(dir.resolve("inside").toFile()));
    } finally {
      locked.toFile().setReadable(true);
    }
  }

  @Test
  void shouldNotWaitForInput() {
    IOException e = assertTimeoutPreemptively(
      Duration.ofSeconds(10),
      () -> assertThrows(IOException.class, () -> Utils.runOrThrow(
        new ProcessBuilder("sh", "-c", "read answer"), "Failed")));
    assertEquals("Failed: exit code 1\n", e.getMessage());
  }

  @Test
  void shouldThrowWithOutputOnFailure() {
    IOException e = assertThrows(IOException.class, () -> Utils.runOrThrow(
      new ProcessBuilder("sh", "-c", "echo ask; echo boom >&2; exit 3"),
      "Failed"));
    assertEquals("Failed: exit code 3\nask\nboom\n", e.getMessage());
  }

  @Test
  void shouldIgnoreNestedGzExtension() {
    Path path = Paths.get("/tmp/some.directory/example.vcf.gz.gz");
    assertEquals("vcf", Utils.getExtension(path, false));
    assertEquals("gz", Utils.getExtension(path, true));
  }
}
