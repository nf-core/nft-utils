package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
   * Runs a process with no input and discarded standard output, and throws if
   * it exits with a non-zero code.
   *
   * @param pb      the process to run
   * @param failure the message prefix of the exception
   * @throws IOException if the process cannot start, is interrupted or exits
   *     with a non-zero code; the message includes its standard error
   */
  static void runOrThrow(
      final ProcessBuilder pb,
      final String failure)
      throws IOException {
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    Process process = pb.start();
    // No input: a prompt fails at once instead of waiting forever
    process.getOutputStream().close();

    StringBuilder stderr = new StringBuilder();
    try (BufferedReader stderrReader = new BufferedReader(
        new InputStreamReader(
          process.getErrorStream(),
          StandardCharsets.UTF_8
        )
    )) {
      String line;
      while ((line = stderrReader.readLine()) != null) {
        stderr.append(line).append("\n");
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
        failure + ": exit code " + exitCode + "\n" + stderr
      );
    }
  }

  /**
   * Extract a lower-cased file name portion from a URL string for extension
   * checking.
   *
   * @param urlString The URL string
   * @return The lower-cased file name portion of the URL
   */
  public static String getURLFileName(final String urlString) {
    // Try to extract a path portion from the URL (strip query strings)
    String pathPart = urlString;
    try {
      java.net.URI uri = new java.net.URI(urlString);
      if (uri.getPath() != null && !uri.getPath().isEmpty()) {
        pathPart = uri.getPath();
      }
    } catch (Exception e) {
      // If parsing fails, fall back to raw urlString
      pathPart = urlString;
    }
    return pathPart.toLowerCase(Locale.ROOT);
  }

  /**
   * Recursively delete a directory and all its contents.
   *
   * @param directory The directory to delete
   */
  static void deleteDirectory(final File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            try {
              Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
              throw new RuntimeException(
                "Failed to delete file: " + file, e
              );
            }
          }
        }
      }

      try {
        Files.deleteIfExists(directory.toPath());
      } catch (IOException e) {
        throw new RuntimeException(
          "Failed to delete directory: " + directory, e
        );
      }
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
