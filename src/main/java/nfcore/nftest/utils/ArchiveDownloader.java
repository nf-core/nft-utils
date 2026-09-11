package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility methods for downloading and extracting archive files from URLs
 * and cloud storage.
 */
public final class ArchiveDownloader {

  /**
   * Prevents instantiation of this utility class.
   */
  private ArchiveDownloader() {
  }

  /**
   * Number of fields expected in an AWS S3 CLI listing line.
   */
  private static final int AWS_S3_LIST_FIELDS = 4;

  /**
   * Index of the object key in an AWS S3 CLI listing line.
   */
  private static final int AWS_S3_KEY_INDEX = 3;

  /**
   * Length of the URI scheme separator.
   */
  private static final int SCHEME_SEPARATOR_LENGTH = 3;

  /**
   * Download a tar archive and extract it in the given destination directory.
   * The file is streamed directly with {@code curl} into {@code tar} via a
   * pipe. The compression type must be provided if applicable.
   * Uses safe single-quoting for the URL and destination path.
   *
   * @param urlString   the URL to fetch
   * @param destPath    directory to extract the tarball into
   * @param compression compression type: tar, gzip, gz, bzip2, bz2, xz, lz4,
   *                    lzma, lzop, zstd
   * @throws IOException on failure
   */
  static void curlAndUntar(
      final String urlString,
      final String destPath,
      final String compression)
      throws IOException {
    Path destDir = Paths.get(destPath);
    Files.createDirectories(destDir);

    String escUrl = Utils.shellEscape(urlString);
    String escDest = Utils.shellEscape(destPath);
    String cmd = "curl -L --retry 5 " + escUrl
      + " | tar xaf - -C " + escDest;
    String tarExt = compression;

    if (tarExt != null && !tarExt.equals("tar")) {
      if (tarExt.startsWith("tar.")) {
        tarExt = tarExt.substring("tar.".length());
      } else if (tarExt.startsWith("t")) {
        tarExt = tarExt.substring("t".length());
      }

      if (tarExt.equals("gzip") || tarExt.equals("gz")) {
        tarExt = "gzip";
      } else if (tarExt.equals("bzip2") || tarExt.equals("bz2")) {
        tarExt = "bzip2";
      } else if (tarExt.equals("xz")) {
        tarExt = "xz";
      } else if (tarExt.equals("lz4")) {
        tarExt = "lz4";
      } else if (tarExt.equals("lzma")) {
        tarExt = "lzma";
      } else if (tarExt.equals("lzop")) {
        tarExt = "lzop";
      } else if (tarExt.equals("zstd") || tarExt.equals("zst")) {
        tarExt = "zstd";
      } else {
        throw new IllegalArgumentException(
          "Unsupported compression type: " + tarExt
        );
      }
      cmd += " --" + tarExt;
    }

    ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
    Utils.ProcessResult result;
    try {
      result = Utils.runProcess(pb);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(
        "Interrupted while downloading " + urlString, e
      );
    }
    if (result.getExitCode() != 0) {
      throw new IOException(
        "Failed to download and extract " + urlString
        + ": exit code " + result.getExitCode() + "\n"
        + result.getStderr()
      );
    }
    System.out.println(
      "Successfully downloaded and extracted file: "
      + urlString
    );
  }

  /**
   * Download a zip file and extract it in the given destination directory.
   * The file is first downloaded with {@code curl} to a temporary file, then
   * {@code unzip} is called and the temporary file is deleted.
   *
   * @param urlString the URL to fetch
   * @param destPath  directory to extract the zip into
   * @throws IOException on failure
   */
  static void curlAndUnzip(
      final String urlString,
      final String destPath)
      throws IOException {
    Path destDir = Paths.get(destPath);
    Files.createDirectories(destDir);

    Path tempFile = Files.createTempFile(destDir, "download", ".zip");

    ProcessBuilder pb = new ProcessBuilder(
        "curl",
        "-L",
        "--retry",
        "5",
        "-o",
        tempFile.toString(),
        urlString);

    try {
      Utils.ProcessResult result = Utils.runProcess(pb);
      if (result.getExitCode() != 0) {
        throw new IOException(
          "Failed to download " + urlString
          + ": exit code " + result.getExitCode() + "\n"
          + result.getStderr()
        );
      }
      pb = new ProcessBuilder(
          "unzip",
          "-o",
          tempFile.toString(),
          "-d",
          destPath);
      result = Utils.runProcess(pb);
      if (result.getExitCode() != 0) {
        throw new IOException(
          "Failed to extract zip " + tempFile
          + ": exit code " + result.getExitCode() + "\n"
          + result.getStderr()
        );
      }
      System.out.println(
        "Successfully downloaded and extracted file: "
        + urlString
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(
        "Interrupted while downloading " + urlString, e
      );
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        System.err.println(
          "Warning: failed to delete temporary file "
          + tempFile + ": " + e.getMessage()
        );
      }
    }
  }

  /**
   * Download an archive and extract it in the given destination directory.
   * Dispatches to {@code curlAndUnzip} for ZIP files and to
   * {@code curlAndUntar} for tar archives based on the URL's file extension.
   *
   * @param urlString the URL to fetch
   * @param destPath  directory to extract the archive into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static void curlAndExtract(
      final String urlString,
      final String destPath)
      throws IOException {
    String lower = Utils.getURLFileName(urlString);

    if (lower.endsWith(".zip")) {
      curlAndUnzip(urlString, destPath);
      return;
    }

    for (String suffix : new String[] {
        "gz", "bz2", "xz", "lz4", "lzma", "lzop", "zst", "zstd"
      }) {
      if (lower.endsWith(".tar." + suffix) || lower.endsWith(".t" + suffix)) {
        curlAndUntar(urlString, destPath, suffix);
        return;
      }
    }

    if (lower.endsWith(".tar")) {
      curlAndUntar(urlString, destPath, null);
      return;
    }

    throw new IllegalArgumentException(
      "Unsupported archive type in URL: " + urlString
    );
  }

  /**
   * Download an archive and extract it in the given destination directory.
   * Dispatches to {@code curlAndUnzip} for ZIP files and to
   * {@code curlAndUntar} for tar archives based on the {@code compression}
   * parameter.
   *
   * @param urlString   the URL to fetch
   * @param destPath    directory to extract the archive into
   * @param compression compression type: zip, tar, or any of the following
   *                    prefixed with "tar." or "t":
   *                    gzip, gz, bzip2, bz2, xz, lz4, lzma, lzop, zstd
   * @throws IOException on failure or if archive type is unsupported
   */
  public static void curlAndExtract(
      final String urlString,
      final String destPath,
      final String compression)
      throws IOException {
    if (compression == null || compression.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'compression' parameter is required."
      );
    }
    String lower = compression.toLowerCase(Locale.ROOT);
    if (lower.equals("zip")) {
      curlAndUnzip(urlString, destPath);
    } else if (lower.equals("tar")) {
      curlAndUntar(urlString, destPath, null);
    } else if (lower.startsWith("tar.")) {
      curlAndUntar(urlString, destPath, compression);
    } else if (lower.startsWith("t")) {
      curlAndUntar(urlString, destPath, compression);
    } else {
      throw new IllegalArgumentException(
        "Unsupported compression type: "
        + compression
      );
    }
  }

  /**
   * Lists files under an S3 prefix using the AWS CLI
   * ({@code aws s3 ls --recursive}).
   *
   * @param s3Path The S3 path or prefix to list.
   * @param includeMatchers Path matchers for files to include.
   * @param excludeMatchers Path matchers for files to exclude.
   * @param includeDir Whether directory markers should be included.
   * @param noSignRequest Whether to use {@code --no-sign-request}.
   * @return A sorted list of S3 object keys matching the filters.
   * @throws IOException If an I/O error occurs.
   * @throws InterruptedException If the thread is interrupted.
   */
  static List<String> getAllFilesFromS3ViaCli(
      final String s3Path,
      final List<PathMatcher> includeMatchers,
      final List<PathMatcher> excludeMatchers,
      final boolean includeDir,
      final boolean noSignRequest)
      throws IOException, InterruptedException {

    String normalizedPath = s3Path;
    if (!s3Path.endsWith("/")) {
      normalizedPath += "/";
    }
    String bucketAndPrefix = normalizedPath.substring("s3://".length());
    int firstSlash = bucketAndPrefix.indexOf('/');
    String prefix;
    if (firstSlash >= 0) {
      prefix = bucketAndPrefix.substring(firstSlash + 1);
    } else {
      prefix = "";
    }

    List<String> cmd = new ArrayList<>(
      Arrays.asList("aws", "s3", "ls", "--recursive")
    );
    if (noSignRequest) {
      cmd.add("--no-sign-request");
    }
    cmd.add(normalizedPath);

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(false);
    Process process = pb.start();

    List<String> files = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
          process.getInputStream(),
          StandardCharsets.UTF_8
        )
    )) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.trim().split("\\s+", AWS_S3_LIST_FIELDS);
        if (parts.length < AWS_S3_LIST_FIELDS) {
          continue;
        }

        String fullKey = parts[AWS_S3_KEY_INDEX];
        String relativePath;
        if (!prefix.isEmpty() && fullKey.startsWith(prefix)) {
          relativePath = fullKey.substring(prefix.length());
        } else {
          relativePath = fullKey;
        }

        if (relativePath.isEmpty()) {
          continue;
        }

        boolean isDir = relativePath.endsWith("/");
        if (isDir && !includeDir) {
          continue;
        }

        final String pathString;
        if (isDir) {
          pathString = relativePath.substring(0, relativePath.length() - 1);
        } else {
          pathString = relativePath;
        }
        Path relPath = Paths.get(pathString);
        boolean included = includeMatchers.isEmpty()
            || includeMatchers.stream().anyMatch(m -> m.matches(relPath));
        boolean excluded = excludeMatchers
          .stream()
          .anyMatch(m -> m.matches(relPath));
        if (included && !excluded) {
          files.add(relativePath);
        }
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException(
          "AWS CLI returned exit code " + exitCode
          + " when listing: " + normalizedPath);
    }

    return files.stream().sorted().collect(Collectors.toList());
  }

  /**
   * Downloads a single file from a cloud URI to a temporary local directory
   * and returns the local {@link Path}.
   *
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails
   * @throws InterruptedException if the process is interrupted
   */
  public static Path downloadFromS3(
      final String cloudUri)
      throws IOException, InterruptedException {
    return downloadFromS3(
      new LinkedHashMap<String, Object>(),
      cloudUri);
  }

  /**
   * Downloads a single file from a cloud URI to a temporary local directory
   * and returns the local {@link Path}.
   *
   * @param options Reserved for future use (currently unused)
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails
   * @throws InterruptedException if the process is interrupted
   */
  public static Path downloadFromS3(
      final LinkedHashMap<String, Object> options,
      final String cloudUri)
      throws IOException, InterruptedException {
    if (cloudUri == null || cloudUri.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'cloudUri' parameter is required."
      );
    }

    int schemeEnd = cloudUri.indexOf("://");
    String withoutScheme;
    if (schemeEnd >= 0) {
      withoutScheme = cloudUri.substring(
        schemeEnd + SCHEME_SEPARATOR_LENGTH
      );
    } else {
      withoutScheme = cloudUri;
    }

    int firstSlash = withoutScheme.indexOf('/');
    String relativeKey;
    if (firstSlash >= 0) {
      relativeKey = withoutScheme.substring(firstSlash + 1);
    } else {
      relativeKey = withoutScheme;
    }

    Path destFile = Paths.get(
      System.getProperty("java.io.tmpdir"),
      "nft-utils-cloud", relativeKey
    );
    Path destParent = destFile.getParent();
    if (destParent != null) {
      Files.createDirectories(destParent);
    }

    List<String> cmd = Arrays.asList(
      "nextflow", "fs", "cp",
      cloudUri, destFile.toString()
    );
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(false);
    Process process = pb.start();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException(
          "nextflow fs cp returned exit code " + exitCode
          + " when downloading: " + cloudUri);
    }

    return destFile;
  }
}
