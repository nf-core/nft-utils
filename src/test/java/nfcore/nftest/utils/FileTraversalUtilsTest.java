package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTraversalUtilsTest {

  @TempDir
  Path dir;

  private List<?> relativePaths(final String path)
      throws IOException, InterruptedException {
    return FileTraversalUtils.getAllFilesFromPath(
      path, List.of("*.log"), List.of(), false, null, false, true);
  }

  private List<?> files(final String path, final boolean includeDir)
      throws IOException, InterruptedException {
    return FileTraversalUtils.getAllFilesFromPath(
      path, List.of("*.log"), List.of(), includeDir, null, false, false);
  }

  @Test
  void shouldReturnSortedPathsOrFiles() throws Exception {
    Files.createDirectories(dir.resolve("sub"));
    Files.writeString(dir.resolve("sub/b.txt"), "b");
    Files.writeString(dir.resolve("a.txt"), "a");
    Files.writeString(dir.resolve("run.log"), "log");

    assertEquals(
      List.of("a.txt", "sub/b.txt"),
      relativePaths(dir.toString()));
    assertEquals(
      List.of(dir.resolve("a.txt").toFile(), dir.resolve("sub/b.txt").toFile()),
      files(dir.toString(), false));
  }

  @Test
  void shouldRejectFilesWithIncludeDirOrS3() {
    assertThrows(
      IllegalArgumentException.class,
      () -> files(dir.toString(), true));
    assertThrows(
      IllegalArgumentException.class,
      () -> files("s3://bucket/prefix/", false));
  }

  @Test
  void shouldFollowSymlinkedDirectories() throws Exception {
    Path target = Files.createDirectories(dir.resolve("published/fastqc"));
    Files.writeString(target.resolve("a.txt"), "a");
    Files.writeString(target.resolve("run.log"), "log");
    Path out = Files.createDirectories(dir.resolve("out"));
    Files.createSymbolicLink(out.resolve("fastqc"), target);
    Path linkedRoot = Files.createSymbolicLink(dir.resolve("root"), out);

    for (Path root : List.of(out, linkedRoot)) {
      assertEquals(
        List.of("fastqc/a.txt"),
        FileTraversalUtils.getAllFilesFromPath(
          root.toString(), List.of("fastqc/*.log"), List.of(), false, null,
          false, true));
    }
  }

  @Test
  void shouldMatchDoubleStarSlashGlobsAtTopLevel() throws Exception {
    Files.createDirectories(dir.resolve("sub"));
    Files.writeString(dir.resolve("a.txt"), "a");
    Files.writeString(dir.resolve("top.html"), "t");
    Files.writeString(dir.resolve("sub/deep.html"), "d");

    assertEquals(
      List.of("a.txt"),
      FileTraversalUtils.getAllFilesFromPath(
        dir.toString(), List.of("**/*.html"), List.of(), false, null, false,
        true));
  }

  @Test
  void shouldDeriveS3DirectoriesFromKeys() {
    FileTraversalUtils.PathFilter all = new FileTraversalUtils.PathFilter(
      Utils.buildMatchers(List.of("**")), List.of());

    assertEquals(
      List.of("qc", "qc/a.txt", "stable", "stable/sub", "stable/sub/b.txt"),
      FileTraversalUtils.s3Paths(
        List.of("stable/sub/b.txt", "qc/", "qc/a.txt"), all, true));
    assertEquals(
      List.of("qc/a.txt", "stable/sub/b.txt"),
      FileTraversalUtils.s3Paths(
        List.of("stable/sub/b.txt", "qc/", "qc/a.txt"), all, false));
  }

  @Test
  void shouldReadFilesOfTypedRecordsButNotOfMeta() {
    List<Object> records = List.of(
      Map.of("id", "s1", "file", "/abs/f.txt"));
    List<Object> tuples = List.of(
      List.of(Map.of("id", "s1", "ref", "/abs/ref.fa"), "/abs/g.txt"));

    assertEquals(
      List.of("/abs/f.txt"),
      FileTraversalUtils.getAllFilesFromChannel(records));
    assertEquals(
      List.of("/abs/g.txt"),
      FileTraversalUtils.getAllFilesFromChannel(tuples));
  }

  @Test
  void shouldGetRelativePathOfFilesAndStrings() {
    assertEquals(
      List.of("a/b.txt", "c.txt"),
      FileTraversalUtils.getRelativePath(
        List.of(dir.resolve("a/b.txt").toFile(), dir.resolve("c.txt").toString()),
        dir.toString()));
  }
}
