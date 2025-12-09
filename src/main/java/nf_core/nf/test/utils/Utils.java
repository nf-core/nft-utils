package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class Utils {

  /**
   * Result of running a process started from a {@link ProcessBuilder}.
   */
  public static class ProcessResult {
    public final int exitCode;
    public final String stderr;

    public ProcessResult(int exitCode, String stderr) {
      this.exitCode = exitCode;
      this.stderr = stderr;
    }
  }

  /**
   * Starts the given {@link ProcessBuilder}, captures stderr, waits for exit,
   * and returns a {@link ProcessResult}.
   */
  public static ProcessResult runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    Process process = pb.start();
    BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    StringBuilder stderr = new StringBuilder();
    String line;
    while ((line = stderrReader.readLine()) != null) {
      stderr.append(line).append("\n");
    }
    int exitCode = process.waitFor();
    return new ProcessResult(exitCode, stderr.toString());
  }

  // Helper to single-quote a string for safe shell usage: '...'
  public static String shellEscape(String s) {
    if (s == null) return "''";
    return "'" + s.replace("'", "'" + "\"'\"" + "'") + "'";
  }

  public static String getURLFileName(String urlString) {
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
}
