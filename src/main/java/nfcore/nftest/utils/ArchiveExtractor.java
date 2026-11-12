package nfcore.nftest.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility methods for extracting archive files from local paths, URLs
 * and cloud storage, and for downloading files from cloud storage.
 */
public final class ArchiveExtractor {

  /**
   * Prevents instantiation of this utility class.
   */
  private ArchiveExtractor() {
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
   * Extract an archive in the given destination directory, with the archive
   * type taken from the file extension.
   *
   * @param source   the local path or URL (http, https, ftp, file, s3) of
   *                 the archive
   * @param destPath directory to extract the archive into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static void extract(
      final String source,
      final String destPath)
      throws IOException {
    String path = Utils.getURLFileName(source);
    String name = path.substring(path.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    if (dot < 0) {
      throw new IllegalArgumentException(
        "Unsupported archive type: " + source
      );
    }
    String extension = name.substring(dot + 1);
    String compression = extension;
    if (name.endsWith(".tar." + extension)) {
      compression = "tar." + extension;
    }
    extract(source, destPath, compression);
  }

  /**
   * Extract an archive in the given destination directory. A local path is
   * extracted in place. An http(s), ftp or file URL is downloaded with
   * {@code curl}, and any other URL, such as s3://, with
   * {@code nextflow fs cp}, to a temporary file in the destination
   * directory, deleted once extracted.
   *
   * @param source      the local path or URL (http, https, ftp, file, s3)
   *                    of the archive
   * @param destPath    directory to extract the archive into
   * @param compression compression type, in any case: zip, tar, or any of
   *                    the following prefixed with "tar." or "t":
   *                    gzip, gz, bzip2, bz2, xz, lz4, lzma, lzop, zst, zstd
   * @throws IOException on failure or if archive type is unsupported
   */
  public static void extract(
      final String source,
      final String destPath,
      final String compression)
      throws IOException {
    List<String> command = extractCommand(compression, destPath);
    Path destDir = Paths.get(destPath);
    Files.createDirectories(destDir);

    int schemeEnd = source.indexOf("://");
    String scheme = "";
    if (schemeEnd >= 0) {
      scheme = source.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
    }

    Path download = null;
    try {
      Path archive;
      if (scheme.isEmpty()) {
        archive = Paths.get(source);
      } else {
        // Download first, so a failed or truncated transfer is never
        // handed to the extractor
        download = Files.createTempFile(destDir, "download", ".tmp");
        archive = download;
        List<String> fetch = switch (scheme) {
          case "http", "https", "ftp", "file" -> List.of(
            "curl", "--fail", "-L", "--retry", "5",
            "-o", download.toString(), source
          );
          default -> List.of(
            "nextflow", "fs", "cp", source, download.toString()
          );
        };
        runOrThrow(new ProcessBuilder(fetch), "Failed to download " + source);
      }
      command.add(archive.toString());
      runOrThrow(
        new ProcessBuilder(command),
        "Failed to extract " + source
      );
      System.out.println("Successfully extracted: " + source);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(
        "Interrupted while extracting " + source, e
      );
    } finally {
      if (download != null) {
        try {
          Files.deleteIfExists(download);
        } catch (IOException e) {
          System.err.println(
            "Warning: failed to delete downloaded file "
            + download + ": " + e.getMessage()
          );
        }
      }
    }
  }

  /**
   * Build the extraction command for a compression type, without the
   * archive path, which the caller appends last.
   *
   * @param compression compression type, see
   *                    {@link #extract(String, String, String)}
   * @param destPath    directory to extract the archive into
   * @return the command, ending with the option that takes the archive path
   * @throws IllegalArgumentException if the compression type is unsupported
   */
  private static List<String> extractCommand(
      final String compression,
      final String destPath) {
    if (compression == null || compression.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'compression' parameter is required."
      );
    }
    String lower = compression.toLowerCase(Locale.ROOT);
    if (lower.equals("zip")) {
      return new ArrayList<>(List.of("unzip", "-o", "-d", destPath));
    }

    List<String> command = new ArrayList<>(
      List.of("tar", "-x", "-C", destPath)
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
      command.add(tarFlag);
    }
    command.add("-f");
    return command;
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
