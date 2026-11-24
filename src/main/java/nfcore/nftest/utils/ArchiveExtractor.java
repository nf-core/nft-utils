package nfcore.nftest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.tukaani.xz.XZInputStream;

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
   * Decompresses a stream.
   */
  @FunctionalInterface
  private interface Decompressor {
    /**
     * Wraps a compressed stream.
     *
     * @param in the compressed stream
     * @return the decompressed stream
     * @throws IOException if the stream header is invalid
     */
    InputStream open(InputStream in) throws IOException;
  }

  /**
   * The decompressor of each compression type of a single compressed file,
   * not a tar archive.
   */
  private static final Map<String, Decompressor> SINGLE_FILE = Map.of(
    "gz", GZIPInputStream::new,
    "gzip", GZIPInputStream::new,
    "xz", XZInputStream::new
  );

  /**
   * The suffix of a single compressed file, in any case.
   */
  private static final Pattern SINGLE_FILE_SUFFIX = Pattern.compile(
    "(?i)\\.(" + String.join("|", SINGLE_FILE.keySet()) + ")$"
  );

  /**
   * Extracts a local archive.
   */
  @FunctionalInterface
  private interface Extraction {
    /**
     * Extracts the archive.
     *
     * @param archive the local archive
     * @throws IOException if the extraction fails
     */
    void from(Path archive) throws IOException;
  }

  /**
   * Extract an archive in a directory. A local path or file:// URL is
   * extracted in place. Any other URL, such as http(s)://, ftp:// or s3://,
   * is downloaded with {@code nextflow fs cp} to a temporary file in the
   * destination directory, deleted once extracted.
   *
   * @param source      the local path, file:// URL or other URL Nextflow
   *                    can read (http(s)://, ftp://, s3://, ...)
   * @param destPath    directory to extract the archive into, or
   *                    {@code null} for a new temporary directory that is
   *                    deleted when the JVM exits
   * @param compression compression type, in any case: zip, tar, or any of
   *                    the following prefixed with "tar." or "t":
   *                    gzip, gz, bzip2, bz2, xz, lz4, lzma, lzop, zst, zstd;
   *                    or gz, gzip or xz for a single compressed file,
   *                    written to the directory without that suffix;
   *                    or {@code null} to take it from the file extension
   * @return the directory the archive was extracted into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static Path extract(
      final String source,
      final String destPath,
      final String compression)
      throws IOException {
    String type = compression;
    if (type == null) {
      type = compressionFromName(source);
    }
    Path extractDir;
    if (destPath == null) {
      extractDir = temporaryDirectory();
    } else {
      extractDir = Paths.get(destPath);
    }
    Extraction extraction = extraction(source, type, extractDir);
    Files.createDirectories(extractDir);

    Path download = null;
    try {
      Path archive;
      if (!source.contains("://")) {
        archive = Paths.get(source);
      } else if (source.toLowerCase(Locale.ROOT).startsWith("file://")) {
        archive = Paths.get(URI.create(source));
      } else {
        // Download first, so a failed or truncated transfer is never
        // handed to the extractor
        download = Files.createTempFile(extractDir, "download", ".tmp");
        archive = download;
        copy(source, download);
      }
      extraction.from(archive);
      System.out.println("Successfully extracted: " + source);
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
    return extractDir;
  }

  /**
   * Extract an archive and return a file or folder in it.
   *
   * @param source      the local path or URL of the archive, see
   *                    {@link #extract(String, String, String)}
   * @param destPath    directory to extract the archive into, or
   *                    {@code null} for a new temporary directory
   * @param compression compression type, or {@code null}
   * @param path        file or folder to return, relative to the extract
   *                    directory
   * @return {@code path} inside the extract directory
   * @throws IOException on failure, if archive type is unsupported or if
   *     {@code path} is not in the archive
   * @throws IllegalArgumentException if {@code path} is not inside the
   *     extract directory
   */
  public static Path extractPath(
      final String source,
      final String destPath,
      final String compression,
      final String path)
      throws IOException {
    Path entry = Paths.get(path).normalize();
    if (entry.isAbsolute() || entry.toString().isEmpty()
        || entry.startsWith("..")) {
      throw new IllegalArgumentException(
        "path must be inside the extract directory: " + path
      );
    }
    Path target = extract(source, destPath, compression).resolve(entry);
    if (!Files.exists(target)) {
      throw new IOException(path + " not found in " + source);
    }
    return target;
  }

  /**
   * Create a temporary directory that is deleted when the JVM exits.
   *
   * @return the directory
   * @throws IOException if the directory cannot be created
   */
  private static Path temporaryDirectory() throws IOException {
    Path dir = Files.createTempDirectory("nft-utils-extract");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        Utils.deleteDirectory(dir.toFile());
      } catch (IOException e) {
        System.err.println(
          "Warning: failed to delete " + dir + ": " + e.getMessage()
        );
      }
    }));
    return dir;
  }

  /**
   * The file name of a local path or URL, in its own case, without the
   * URL query or fragment.
   *
   * @param source the local path or URL
   * @return the file name
   */
  private static String fileName(final String source) {
    String path = uriPath(source);
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /**
   * The path of a URL, or a local path as is, so '#' and '?' stay in its
   * name.
   *
   * @param source the local path or URL
   * @return the path, or {@code source} if it has none or is not a valid URI
   */
  private static String uriPath(final String source) {
    if (!source.contains("://")) {
      return source;
    }
    try {
      String path = new URI(source).getPath();
      return path == null || path.isEmpty() ? source : path;
    } catch (URISyntaxException e) {
      return source;
    }
  }

  /**
   * Take the compression type from the file extension of an archive.
   *
   * @param source the local path or URL of the archive
   * @return the compression type
   * @throws IllegalArgumentException if the name has no extension
   */
  private static String compressionFromName(final String source) {
    String name = fileName(source).toLowerCase(Locale.ROOT);
    int dot = name.lastIndexOf('.');
    if (dot < 0) {
      throw new IllegalArgumentException(
        "Unsupported archive type: " + source
      );
    }
    String extension = name.substring(dot + 1);
    if (name.endsWith(".tar." + extension)) {
      return "tar." + extension;
    }
    return extension;
  }

  /**
   * Choose how to extract an archive of a compression type.
   *
   * @param source      the local path or URL of the archive
   * @param compression compression type, see
   *                    {@link #extract(String, String, String)}
   * @param extractDir  directory to extract the archive into
   * @return the extraction
   * @throws IllegalArgumentException if the compression type is unsupported
   */
  private static Extraction extraction(
      final String source,
      final String compression,
      final Path extractDir) {
    Decompressor decompressor =
      SINGLE_FILE.get(compression.toLowerCase(Locale.ROOT));
    if (decompressor != null) {
      Path output = extractDir.resolve(decompressedName(source));
      return archive -> decompress(decompressor, archive, output, source);
    }
    List<String> command = extractCommand(compression, extractDir.toString());
    return archive -> {
      List<String> args = new ArrayList<>(command);
      args.add(archive.toString());
      Utils.runOrThrow(
        new ProcessBuilder(args), "Failed to extract " + source
      );
    };
  }

  /**
   * Decompress a single compressed file. It is written next to the output
   * first, so a failure leaves no partial file.
   *
   * @param decompressor the decompressor of its compression type
   * @param archive      the compressed file
   * @param output       the decompressed file
   * @param source       the local path or URL of the compressed file
   * @throws IOException if the file cannot be read, decompressed or written
   */
  private static void decompress(
      final Decompressor decompressor,
      final Path archive,
      final Path output,
      final String source) throws IOException {
    Path partial = output.resolveSibling(output.getFileName() + ".part");
    try (InputStream raw = Files.newInputStream(archive);
        InputStream in = decompressor.open(raw)) {
      Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
      Files.move(partial, output, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      IOException failure = new IOException("Failed to extract " + source, e);
      try {
        Files.deleteIfExists(partial);
      } catch (IOException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  /**
   * Name of a compressed file once decompressed: its file name, in its own
   * case, without the suffix of a {@link #SINGLE_FILE} type.
   *
   * @param source the local path or URL of the compressed file
   * @return the decompressed file name
   * @throws IllegalArgumentException if the source has no file name, or
   *     one that would resolve to a directory ({@code .} or {@code ..})
   */
  private static String decompressedName(final String source) {
    return checkedName(
      SINGLE_FILE_SUFFIX.matcher(fileName(source)).replaceFirst(""), source
    );
  }

  /**
   * Checks that a file name stays inside the directory it is resolved in.
   *
   * @param name   the file name
   * @param source the local path or URL it comes from, for the error
   * @return the file name
   * @throws IllegalArgumentException if the name is empty, {@code .} or
   *     {@code ..}
   */
  private static String checkedName(final String name, final String source) {
    if (name.isEmpty() || name.equals(".") || name.equals("..")) {
      throw new IllegalArgumentException("No file name in " + source);
    }
    return name;
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
      return new ArrayList<>(List.of("unzip", "-q", "-o", "-d", destPath));
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
   * Downloads a single file from a cloud URI into a new temporary directory,
   * deleted when nf-test exits, and returns the local {@link Path}.
   *
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails
   * @throws IllegalArgumentException if the URI has no file name
   */
  public static Path downloadFromS3(
      final String cloudUri)
      throws IOException {
    if (cloudUri == null || cloudUri.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'cloudUri' parameter is required."
      );
    }

    String name = checkedName(fileName(cloudUri), cloudUri);
    Path destFile = temporaryDirectory().resolve(name);
    copy(cloudUri, destFile);
    return destFile;
  }

  /**
   * Download a URL with {@code nextflow fs cp}.
   *
   * @param uri    the URL to download
   * @param target the file to write
   * @throws IOException if the download fails
   */
  private static void copy(final String uri, final Path target)
      throws IOException {
    // nextflow fs cp reads ? and * as glob wildcards unless escaped
    String escaped = uri.replaceAll("[?*]", "\\\\$0");
    Utils.runOrThrow(
      new ProcessBuilder("nextflow", "fs", "cp", escaped, target.toString()),
      "Failed to download " + uri
    );
  }
}
