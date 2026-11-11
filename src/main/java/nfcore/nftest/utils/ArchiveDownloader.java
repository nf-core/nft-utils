package nfcore.nftest.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

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
   * Length of the URI scheme separator.
   */
  private static final int SCHEME_SEPARATOR_LENGTH = 3;

  /**
   * The {@code tar} decompression flag of each supported compression suffix.
   */
  private static final Map<String, String> TAR_FLAGS = Map.of(
    "gz", "--gzip",
    "gzip", "--gzip",
    "bz2", "--bzip2",
    "bzip2", "--bzip2",
    "xz", "--xz",
    "lz4", "--lz4",
    "lzma", "--lzma",
    "lzop", "--lzop",
    "zst", "--zstd",
    "zstd", "--zstd"
  );

  /**
   * Download a file to a temporary file in the destination directory, run the
   * extraction command on it, then delete it. Downloading first means a failed
   * or truncated transfer is never handed to the extractor.
   *
   * @param urlString the URL to fetch
   * @param destPath  directory to extract the archive into
   * @param extractor builds the extraction command for the downloaded file
   * @throws IOException on failure
   */
  private static void downloadAndExtract(
      final String urlString,
      final String destPath,
      final Function<Path, ProcessBuilder> extractor)
      throws IOException {
    Path destDir = Paths.get(destPath);
    Files.createDirectories(destDir);

    Path tempFile = Files.createTempFile(destDir, "download", ".tmp");

    try {
      runOrThrow(
        new ProcessBuilder(
          "curl", "--fail", "-L", "--retry", "5",
          "-o", tempFile.toString(), urlString
        ),
        "Failed to download " + urlString
      );
      runOrThrow(
        extractor.apply(tempFile),
        "Failed to extract " + urlString
      );
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
   * Runs a process and throws if it exits with a non-zero code.
   *
   * @param pb      the process to run
   * @param failure the message prefix of the exception
   * @throws IOException if the process fails
   * @throws InterruptedException if the process is interrupted
   */
  private static void runOrThrow(
      final ProcessBuilder pb,
      final String failure)
      throws IOException, InterruptedException {
    Utils.ProcessResult result = Utils.runProcess(pb);
    if (result.getExitCode() != 0) {
      throw new IOException(
        failure + ": exit code " + result.getExitCode() + "\n"
        + result.getStderr()
      );
    }
  }

  /**
   * Download an archive and extract it in the given destination directory,
   * with the archive type taken from the URL's file extension.
   *
   * @param urlString the URL to fetch
   * @param destPath  directory to extract the archive into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static void curlAndExtract(
      final String urlString,
      final String destPath)
      throws IOException {
    String path = Utils.getURLFileName(urlString);
    String name = path.substring(path.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    if (dot < 0) {
      throw new IllegalArgumentException(
        "Unsupported archive type in URL: " + urlString
      );
    }
    String extension = name.substring(dot + 1);
    String compression = extension;
    if (name.endsWith(".tar." + extension)) {
      compression = "tar." + extension;
    }
    curlAndExtract(urlString, destPath, compression);
  }

  /**
   * Download an archive and extract it in the given destination directory.
   *
   * @param urlString   the URL to fetch
   * @param destPath    directory to extract the archive into
   * @param compression compression type, in any case: zip, tar, or any of
   *                    the following prefixed with "tar." or "t":
   *                    gzip, gz, bzip2, bz2, xz, lz4, lzma, lzop, zst, zstd
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
      downloadAndExtract(
        urlString,
        destPath,
        archive -> new ProcessBuilder(
          "unzip", "-o", archive.toString(), "-d", destPath
        )
      );
      return;
    }

    List<String> tarCmd = new ArrayList<>(
      List.of("tar", "xaf", "-", "-C", destPath)
    );
    if (!lower.equals("tar")) {
      String suffix = "";
      if (lower.startsWith("tar.")) {
        suffix = lower.substring("tar.".length());
      } else if (lower.startsWith("t")) {
        suffix = lower.substring("t".length());
      }
      String tarFlag = TAR_FLAGS.get(suffix);
      if (tarFlag == null) {
        throw new IllegalArgumentException(
          "Unsupported compression type: " + compression
        );
      }
      tarCmd.add(tarFlag);
    }
    downloadAndExtract(
      urlString,
      destPath,
      archive -> new ProcessBuilder(tarCmd).redirectInput(archive.toFile())
    );
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

    Utils.ProcessResult result = Utils.runProcess(new ProcessBuilder(
      "nextflow", "fs", "cp", cloudUri, destFile.toString()
    ));
    if (result.getExitCode() != 0) {
      throw new IOException(
          "nextflow fs cp returned exit code " + result.getExitCode()
          + " when downloading: " + cloudUri + "\n" + result.getStderr());
    }

    return destFile;
  }
}
