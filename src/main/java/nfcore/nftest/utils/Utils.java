package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility methods for process execution, shell escaping, and file path
 * manipulation.
 */
public final class Utils {

  /**
   * Prevents instantiation of this utility class.
   */
  private Utils() {
  }

  /**
   * Compiles glob patterns into path matchers, skipping null or empty globs.
   *
   * @param globs The glob patterns, or {@code null}.
   * @return The path matchers.
   */
  static List<PathMatcher> buildMatchers(final List<String> globs) {
    List<PathMatcher> matchers = new ArrayList<>();
    if (globs == null) {
      return matchers;
    }
    for (String glob : globs) {
      if (glob != null && !glob.isEmpty()) {
        matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
      }
    }
    return matchers;
  }

  /**
   * Runs a process with no input and throws if it exits with a non-zero
   * code.
   *
   * @param pb      the process to run
   * @param failure the message prefix of the exception
   * @throws IOException if the process cannot start, is interrupted or exits
   *     with a non-zero code; the message includes its standard output and
   *     error
   */
  static void runOrThrow(
      final ProcessBuilder pb,
      final String failure)
      throws IOException {
    // Prompts go to stdout, so keep it to explain a failure
    pb.redirectErrorStream(true);
    Process process = pb.start();
    // No input: a prompt fails at once instead of waiting forever
    process.getOutputStream().close();

    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
          process.getInputStream(),
          StandardCharsets.UTF_8
        )
    )) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(failure + ": interrupted", e);
    }
    if (exitCode != 0) {
      throw new IOException(
        failure + ": exit code " + exitCode + "\n" + output
      );
    }
  }

  /**
   * Recursively delete a directory and all its contents, without following
   * symbolic links.
   *
   * @param directory The directory to delete
   * @throws IOException if a file or directory cannot be deleted
   */
  static void deleteDirectory(final File directory) throws IOException {
    Path root = directory.toPath();
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    List<Path> paths;
    try (Stream<Path> walk = Files.walk(root)) {
      paths = walk.sorted(Comparator.reverseOrder()).toList();
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    for (Path path : paths) {
      Files.delete(path);
    }
  }

  /**
   * Extract the last extension from a file path.
   * @param path File path to get extension from
   * @return The extension of the path
   */
  public static String getExtension(final Path path) {
    return getExtension(path, true);
  }

  /**
   * Extract the last extension from a file path.
   * @param path File path to get extension from
   * @param keepGz Should `.gz` compression extension be kept
   * @return The extension of the path
   */
  public static String getExtension(final Path path, final boolean keepGz) {
    Path fileNamePath = path.getFileName();
    if (fileNamePath == null) {
      return "";
    }
    String fileName = fileNamePath.toString();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot <= 0 || lastDot == fileName.length() - 1) {
      return "";
    }
    String extension = fileName.substring(lastDot + 1).toLowerCase();
    if (extension.equals("gz") && !keepGz) {
      return getExtension(
        Paths.get(fileName.substring(0, lastDot)),
        false
      );
    }
    return extension;
  }
}
