package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
   * Result of running a process started from a {@link ProcessBuilder}.
   */
  public static final class ProcessResult {
    /** The exit code returned by the process. */
    private final int exitCode;
    /** The standard error output captured from the process. */
    private final String stderr;

    /**
     * Creates a process result containing the exit code and captured stderr.
     *
     * @param processExitCode the exit code returned by the process
     * @param processStderr the captured standard error output
     */
    public ProcessResult(
        final int processExitCode,
        final String processStderr) {
      this.exitCode = processExitCode;
      this.stderr = processStderr;
    }

    /**
     * Returns the exit code of the process.
     *
     * @return the process exit code
     */
    public int getExitCode() {
      return exitCode;
    }

    /**
     * Returns the captured standard error output.
     *
     * @return the captured stderr
     */
    public String getStderr() {
      return stderr;
    }
  }

  /**
   * Starts the given {@link ProcessBuilder}, discards standard output, captures
   * standard error, waits for the process to exit, and returns a
   * {@link ProcessResult} containing the exit code and captured error output.
   *
   * @param pb The {@link ProcessBuilder} used to start the process.
   * @return A {@link ProcessResult} containing the process exit code and
   * stderr.
   * @throws IOException If an I/O error occurs while starting or reading from
   *     the process.
   * @throws InterruptedException If the current thread is interrupted while
   *     waiting for the process to exit.
   */
  public static ProcessResult runProcess(
      final ProcessBuilder pb)
      throws IOException, InterruptedException {
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    Process process = pb.start();

    try (BufferedReader stderrReader = new BufferedReader(
        new InputStreamReader(
          process.getErrorStream(),
          StandardCharsets.UTF_8
        )
    )) {
      StringBuilder stderr = new StringBuilder();
      String line;
      while ((line = stderrReader.readLine()) != null) {
        stderr.append(line).append("\n");
      }

      int exitCode = process.waitFor();
      return new ProcessResult(exitCode, stderr.toString());
    }
  }

  /**
   * Shell escape a string by wrapping in single quotes and escaping existing
   * single quotes.
   *
   * @param s The string to escape for safe use in a shell command.
   * @return The shell-escaped string
   */
  public static String shellEscape(final String s) {
    if (s == null) {
      return "''";
    }
    return "'" + s.replace("'", "'" + "\"'\"" + "'") + "'";
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
