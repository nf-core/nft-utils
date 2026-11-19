package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTraversalUtilsTest {

  @TempDir
  Path dir;

  private List<?> listFiles(
      final String path,
      final boolean includeDir,
      final boolean relative)
      throws IOException, InterruptedException {
    return FileTraversalUtils.getAllFilesFromPath(
      path, List.of("*.log"), List.of(), includeDir, null, false, relative);
  }

  @Test
  void shouldReturnSortedFilesWhenNotRelative() throws Exception {
    Files.createDirectories(dir.resolve("sub"));
    Files.writeString(dir.resolve("sub/b.txt"), "b");
    Files.writeString(dir.resolve("a.txt"), "a");
    Files.writeString(dir.resolve("run.log"), "log");

    assertEquals(
      List.of("a.txt", "sub/b.txt"),
      listFiles(dir.toString(), false, true));
    assertEquals(
      List.of(dir.resolve("a.txt").toFile(), dir.resolve("sub/b.txt").toFile()),
      listFiles(dir.toString(), false, false));
  }

  @Test
  void shouldRejectFilesWithIncludeDirOrS3() {
    assertThrows(
      IllegalArgumentException.class,
      () -> listFiles(dir.toString(), true, false));
    assertThrows(
      IllegalArgumentException.class,
      () -> listFiles("s3://bucket/prefix/", false, false));
  }
}
