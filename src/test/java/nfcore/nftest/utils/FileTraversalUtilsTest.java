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
  void shouldRejectFileOrMissingPath() throws IOException {
    Path file = Files.writeString(dir.resolve("a.txt"), "a");
    assertThrows(
      IllegalArgumentException.class,
      () -> relativePaths(file.toString()));
    assertThrows(
      IllegalArgumentException.class,
      () -> relativePaths(dir.resolve("missing").toString()));
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
  @SuppressWarnings("deprecation")
  void shouldListRootDirectoriesAndLinksFromDir() throws Exception {
    Path target = Files.createDirectories(dir.resolve("target"));
    Files.writeString(target.resolve("d.txt"), "d");
    Path results = Files.createDirectories(dir.resolve("results/sub"))
      .getParent();
    Files.createDirectories(results.resolve("output"));
    Files.writeString(results.resolve("a.txt"), "a");
    Files.writeString(results.resolve("sub/b.txt"), "b");
    Files.writeString(results.resolve("output/c.txt"), "c");
    Files.createSymbolicLink(results.resolve("link"), target);

    assertEquals(
      List.of(results, results.resolve("a.txt"), results.resolve("link"),
        results.resolve("link/d.txt"), results.resolve("output/c.txt"),
        results.resolve("sub"), results.resolve("sub/b.txt"))
        .stream().map(Path::toFile).toList(),
      FileTraversalUtils.getAllFilesFromDir(
        results.toString(), true, List.of(), null, List.of("**")));
    assertEquals(
      List.of("a.txt", "link/d.txt", "output/c.txt", "sub/b.txt"),
      FileTraversalUtils.getAllFilesFromDir(
        results.toString(), false, List.of(), null, List.of("**"), true));
    assertEquals(
      List.of(results.resolve("a.txt").toFile()),
      FileTraversalUtils.getAllFilesFromDir(
        results.resolve("a.txt").toString(), true, List.of(), null,
        List.of("**")));
  }

  @Test
  void shouldSkipSymlinkLoops() throws Exception {
    Path sub = Files.createDirectories(dir.resolve("sub"));
    Files.writeString(sub.resolve("a.txt"), "a");
    Files.createSymbolicLink(sub.resolve("loop"), dir);

    assertEquals(
      List.of("sub", "sub/a.txt"),
      FileTraversalUtils.getAllFilesFromPath(
        dir.toString(), List.of(), List.of(), true, null, false, true));
  }

  @Test
  void shouldIncludeEmptyAndSymlinkedDirectories() throws Exception {
    Path target = Files.createDirectories(dir.resolve("target"));
    Files.writeString(target.resolve("a.txt"), "a");
    Path out = Files.createDirectories(dir.resolve("out/empty"));
    Files.createSymbolicLink(out.resolveSibling("linked"), target);

    assertEquals(
      List.of("empty", "linked", "linked/a.txt"),
      FileTraversalUtils.getAllFilesFromPath(
        out.getParent().toString(), List.of(), List.of(), true, null, false,
        true));
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
  void shouldDeriveDirectoriesFromKeys() {
    FileTraversalUtils.PathFilter all = new FileTraversalUtils.PathFilter(
      Utils.buildMatchers(List.of("**")), List.of());

    assertEquals(
      List.of("qc", "qc/a.txt", "stable", "stable/sub", "stable/sub/b.txt"),
      FileTraversalUtils.filterRelativePaths(
        List.of("stable/sub/b.txt", "qc/", "qc/a.txt"), all, true));
    assertEquals(
      List.of("qc/a.txt", "stable/sub/b.txt"),
      FileTraversalUtils.filterRelativePaths(
        List.of("stable/sub/b.txt", "qc/", "qc/a.txt"), all, false));
  }

  @Test
  void shouldReadMapsEmittedAloneButNotInTuples() {
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
    assertEquals(
      List.of("/abs/ref.fa"),
      FileTraversalUtils.getAllFilesFromChannel(
        List.of(Map.of("ref", "/abs/ref.fa"))));
    assertEquals(
      List.of(),
      FileTraversalUtils.getAllFilesFromChannel(Map.of("out", records)));
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
