package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  /**
   * Shell escape a string by wrapping in single quotes and escaping existing single quotes.
   * @param s
   * @return The shell-escaped string
   */
  public static String shellEscape(String s) {
    if (s == null) return "''";
    return "'" + s.replace("'", "'" + "\"'\"" + "'") + "'";
  }

  /**
   * Extract a lower-cased file name portion from a URL string for extension checking.
   * @param urlString The URL string
   * @return The lower-cased file name portion of the URL
   */
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

  /**
   * Extract the last extension from a file path.
   * @param Path path The file path
   * @param boolean keepGz Should `.gz` compression extension be kept
   * @return The extension of the path
   */
  public static String getExtension(Path path) {
    return getExtension(path, true);
  }

  public static String getExtension(Path path, boolean keepGz) {
    String fileName = path.getFileName().toString();
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

  /**
   * Download a file from a URL to a temporary location and return the path.
   * @param String urlString The URL string
   * @return The path to the downloaded file
   */
  static Path downloadFile(String urlString, Path destinationFolder) {
    try {
      URI uri = URI.create(urlString);
      String fileName = Paths.get(uri.getPath()).getFileName().toString();
      if (fileName.isEmpty()) {
        throw new RuntimeException(
          "Could not determine filename from URL: " + urlString
        );
      }
      Files.createDirectories(destinationFolder);
      Path destination = destinationFolder.resolve(fileName);

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .GET()
        .build();

      HttpResponse<Path> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofFile(destination)
      );

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        Files.deleteIfExists(destination);

        throw new RuntimeException(
          "Failed to download file: HTTP " +
          response.statusCode()
        );
      }
      return destination;

    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to download file: " + urlString,
        e
      );
    }
  }
}
