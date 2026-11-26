package nfcore.nftest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Public API for the nft-utils nf-test plugin.
 *
 * <p>All methods in this class are registered as Groovy extension methods
 * via {@code META-INF/nf-test-plugin}. Each method delegates to a
 * specialized utility class.
 */
public final class Methods {

  /**
   * Prevents instantiation of this utility class.
   */
  private Methods() {
  }

  // ── YAML ──────────────────────────────────────────────────────────────

  /**
   * Reads a Version YAML file and returns its contents as a nested map.
   *
   * @param filePath The path to the YAML file to read.
   * @return A nested map containing the YAML data, empty for an empty file.
   * @throws IOException If the file cannot be read, or a top-level value is
   *     not a map.
   */
  public static Map<String, Map<String, Object>> readYamlFile(
      final String filePath)
      throws IOException {
    return YamlUtils.readYamlFile(filePath);
  }

  /**
   * Removes the Nextflow version entry from the Workflow entry in the
   * specified Version YAML file.
   *
   * @param versionFile The YAML file path or wildcard pattern to process.
   * @return A map containing the YAML data with the Nextflow version removed.
   * @throws IOException If no file matches, a file cannot be read, or a
   *     top-level value is not a map.
   */
  public static Map<String, Map<String, Object>> removeNextflowVersion(
      final CharSequence versionFile)
      throws IOException {
    return YamlUtils.removeFromYamlMap(versionFile, "Workflow", "Nextflow");
  }

  /**
   * Removes an entry from a YAML map and merges the processed results from all
   * files matching the specified path or wildcard pattern.
   *
   * @param versionFile The YAML file path or wildcard pattern to process.
   * @param key1 The top-level key from which to remove an entry.
   * @param key2 The nested key to remove, or {@code null} or empty to remove
   *     the entire {@code key1} entry.
   * @return A merged map containing the processed YAML data.
   * @throws IOException If no file matches, a file cannot be read, or a
   *     top-level value is not a map.
   */
  public static Map<String, Map<String, Object>> removeFromYamlMap(
      final CharSequence versionFile,
      final String key1,
      final String key2)
      throws IOException {
    return YamlUtils.removeFromYamlMap(versionFile, key1, key2);
  }

  /**
   * Removes the specified key from a YAML map using default options.
   *
   * @param versionFile The YAML content containing the map to modify.
   * @param key1 The key to remove from the YAML map.
   * @return A map containing the updated YAML data.
   * @throws IOException If no file matches, a file cannot be read, or a
   *     top-level value is not a map.
   */
  public static Map<String, Map<String, Object>> removeFromYamlMap(
      final CharSequence versionFile,
      final String key1)
      throws IOException {
    return YamlUtils.removeFromYamlMap(versionFile, key1, null);
  }

  // ── FILE TRAVERSAL ────────────────────────────────────────────────────

  /**
   * Retrieves all files from the specified directory using default options.
   *
   * @param path The path to the directory to traverse.
   * @return A list of files found in the directory.
   * @throws IOException If an error occurs while traversing the directory.
   * @deprecated use {@link #getAllFilesFromPath(LinkedHashMap, String)}
   */
  @Deprecated
  public static List<?> getAllFilesFromDir(
      final String path)
      throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  /**
   * Retrieves files from an output directory using options provided in a map.
   *
   * @param options Options controlling directory traversal and filtering.
   * @param outdir The root output directory to traverse.
   * @return A list of matching files or relative paths.
   * @throws IOException If an error occurs while traversing the directory.
   * @throws IllegalArgumentException If {@code outdir} is invalid.
   * @deprecated use {@link #getAllFilesFromPath(LinkedHashMap, String)}
   */
  @Deprecated
  public static List<?> getAllFilesFromDir(
      final LinkedHashMap<String, Object> options,
      final String outdir)
      throws IOException {
    Options opts = new Options("getAllFilesFromDir", options,
      "ignore", "include", "includeDir", "ignoreFile", "relative");
    return FileTraversalUtils.getAllFilesFromDir(
      outdir,
      opts.bool("includeDir", false),
      opts.strings("ignore", List.of()),
      opts.string("ignoreFile", null),
      opts.strings("include", List.of("**/*")),
      opts.bool("relative", false));
  }

  /**
   * Recursively retrieves files and optionally directories from an output
   * directory, applying include and exclude glob patterns.
   *
   * @param outdir The root output directory to traverse.
   * @param includeDir Whether directories should be included in the result.
   * @param ignoreGlobs Glob patterns to exclude.
   * @param ignoreFilePath Path to a file containing additional ignore patterns.
   * @param includeGlobs Glob patterns to include.
   * @return A sorted list of matching files and directories.
   * @throws IOException If an error occurs while traversing the directory.
   * @deprecated use {@link #getAllFilesFromPath(LinkedHashMap, String)}
   */
  @Deprecated
  public static List<File> getAllFilesFromDir(
      final String outdir,
      final boolean includeDir,
      final List<String> ignoreGlobs,
      final String ignoreFilePath,
      final List<String> includeGlobs)
      throws IOException {
    return FileTraversalUtils.getAllFilesFromDir(
      outdir, includeDir, ignoreGlobs, ignoreFilePath, includeGlobs);
  }

  /**
   * Get all file paths from a Nextflow channel output.
   *
   * @param channel the channel output to process
   * @return a flattened list containing only absolute file paths
   */
  public static List<String> getAllFilesFromChannel(
      final Object channel) {
    return FileTraversalUtils.getAllFilesFromChannel(channel);
  }

  /**
   * Converts a list of file paths to paths relative to the specified base
   * directory.
   *
   * @param filePaths The files or path strings to convert.
   * @param baseDir The base directory used to calculate relative paths.
   * @return A list of relative paths.
   */
  public static List<String> getRelativePath(
      final List<?> filePaths,
      final String baseDir) {
    return FileTraversalUtils.getRelativePath(filePaths, baseDir);
  }

  /**
   * Lists all files at the given path, returning sorted relative paths.
   *
   * @param path The path to list - a local directory or an S3 URI
   * @return A sorted list of relative file paths
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<?> getAllFilesFromPath(
      final String path)
      throws IOException, InterruptedException {
    return getAllFilesFromPath(new LinkedHashMap<String, Object>(), path);
  }

  /**
   * Lists all files at the given path (local or cloud), returning sorted
   * relative paths, with filtering options.
   *
   * @param options Named options map (Groovy named params)
   * @param path    The path to list
   * @return A sorted list of relative file paths, or of files with
   *     {@code relative: false}
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<?> getAllFilesFromPath(
      final LinkedHashMap<String, Object> options,
      final String path)
      throws IOException, InterruptedException {
    Options opts = new Options("getAllFilesFromPath", options,
      "ignore", "include", "includeDir", "ignoreFile", "noSignRequest",
      "relative");
    return FileTraversalUtils.getAllFilesFromPath(
      path,
      opts.strings("ignore", List.of()),
      opts.strings("include", List.of("**")),
      opts.bool("includeDir", false),
      opts.string("ignoreFile", null),
      opts.bool("noSignRequest", false),
      opts.bool("relative", true));
  }

  // ── HASHING ───────────────────────────────────────────────────────────

  /**
   * Computes an MD5 hash from the string representation of each element in
   * a list, {@code null} as {@code null}.
   *
   * @param input The list of objects to include in the MD5 calculation.
   * @return The MD5 digest as a hexadecimal string.
   */
  public static String listToMD5(final List<?> input) {
    return HashUtils.listToMD5(input);
  }

  /**
   * Reads a file and returns its lines, minus the excluded ones, for example
   * to hash a file with {@code listToMD5()} without its unstable lines.
   *
   * @param options {@code lines}: 1-based numbers of the lines to remove;
   *                {@code ignore}: strings whose lines are removed
   * @param path    The file to read.
   * @return The remaining lines, in order.
   * @throws IOException If the file cannot be read.
   * @throws IllegalArgumentException on an unknown option, a wrong option
   *     type, a line number out of range or an empty ignore string
   */
  public static List<String> excludeLines(
      final LinkedHashMap<String, Object> options,
      final String path) throws IOException {
    Options opts = new Options("excludeLines", options, "lines", "ignore");
    return Utils.excludeLines(
      path,
      opts.integers("lines", List.of()),
      opts.strings("ignore", List.of()));
  }

  // ── nf-core MODULE MANAGEMENT ─────────────────────────────────────────

  /**
   * Creates the modules directory and .nf-core.yml configuration file.
   *
   * @param libDir The directory path to initialise an nf-core library at
   * @throws IOException if the library cannot be created
   */
  public static void nfcoreInitialise(final String libDir)
      throws IOException {
    NfCoreUtils.nfcoreInitialise(libDir);
  }

  /**
   * Installs nf-core modules from a list.
   *
   * @param libDir  An nf-core library initialised by nfcoreInitialise()
   * @param modules List of module names (strings) or module maps
   * @throws IOException if a module fails to install
   */
  public static void nfcoreInstall(final String libDir, final List<?> modules)
      throws IOException {
    NfCoreUtils.nfcoreInstall(libDir, modules);
  }

  /**
   * Creates a symbolic link from the installed nf-core modules to the base
   * directory.
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   * @throws IOException if either directory does not exist or a link cannot
   *     be created
   */
  public static void nfcoreLink(final String libDir, final String modulesDir)
      throws IOException {
    NfCoreUtils.nfcoreLink(libDir, modulesDir);
  }

  /**
   * Remove all linked modules from a modules directory.
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   * @throws IOException if either directory does not exist or a link cannot
   *     be read or removed
   */
  public static void nfcoreUnlink(
      final String libDir,
      final String modulesDir)
      throws IOException {
    NfCoreUtils.nfcoreUnlink(libDir, modulesDir);
  }

  /**
   * Delete the temporary nf-core library.
   *
   * @param libDir The library directory path to delete
   * @throws IOException if a file in the library cannot be deleted
   */
  public static void nfcoreDeleteLibrary(final String libDir)
      throws IOException {
    NfCoreUtils.nfcoreDeleteLibrary(libDir);
  }

  // ── OUTPUT FILTERING ──────────────────────────────────────────────────

  /**
   * Filters Nextflow stdout/stderr output to remove variable content that makes
   * snapshots unstable.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(final Object output) {
    return filterNextflowOutputWithOptions(null, output);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @param sorted Whether to sort the output lines alphabetically
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final boolean sorted) {
    return filterNextflowOutputWithOptions(Map.of("sorted", sorted), output);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting and ANSI code
   * handling.
   *
   * @param output   The stdout or stderr output (String or List) to filter
   * @param sorted   Whether to sort the output lines alphabetically
   * @param keepAnsi Whether to keep ANSI escape codes
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final boolean sorted,
      final boolean keepAnsi) {
    return filterNextflowOutputWithOptions(
      Map.of("sorted", sorted, "keepAnsi", keepAnsi), output);
  }

  /**
   * Sanitizes a Nextflow output line by replacing non-deterministic values with
   * stable placeholders.
   *
   * @param line The output line to sanitize.
   * @param capturedRunName The run name captured from the Nextflow launching
   *     line, or {@code null}.
   * @return The sanitized output line.
   */
  public static String filterLinePattern(
      final String line,
      final String capturedRunName) {
    return NextflowOutputFilter.filterLinePattern(line, capturedRunName);
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns, optional
   * sorting, and ANSI code handling.
   *
   * @param output             The output to filter
   * @param additionalPatterns Additional regex patterns to remove
   * @param sorted             Whether to sort output lines
   * @param keepAnsi           Whether to keep ANSI codes
   * @param ignore             Strings to filter out
   * @param include            Strings to include
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns,
      final boolean sorted,
      final boolean keepAnsi,
      final List<String> ignore,
      final List<String> include) {
    // A HashMap, as a Groovy caller can pass null for any list
    Map<String, Object> options = new HashMap<>();
    options.put("additionalPatterns", additionalPatterns);
    options.put("sorted", sorted);
    options.put("keepAnsi", keepAnsi);
    options.put("ignore", ignore);
    options.put("include", include);
    return filterNextflowOutputWithOptions(options, output);
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns.
   *
   * @param output             The output to filter
   * @param additionalPatterns Additional regex patterns to remove
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns) {
    return filterNextflowOutputWithOptions(
      Collections.singletonMap("additionalPatterns", additionalPatterns),
      output);
  }

  /**
   * Filters Nextflow output using Groovy's named parameter syntax.
   *
   * @param options LinkedHashMap of named options (Groovy named params)
   * @param output  The output to filter
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final LinkedHashMap<String, Object> options,
      final Object output) {
    return filterNextflowOutputWithOptions(options, output);
  }

  /**
   * Filters Nextflow output using an explicit options map passed after the
   * output, e.g. {@code filterNextflowOutput(output, [sorted: false])}.
   *
   * @param output  The output to filter
   * @param options The filtering options, or {@code null} to use defaults
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final Map<String, Object> options) {
    return filterNextflowOutputWithOptions(options, output);
  }

  /**
   * Filters Nextflow output with named options, which hold the defaults of
   * every overload; a missing or {@code null} option takes its default.
   *
   * @param options The filtering options, or {@code null} to use defaults
   * @param output  The output to filter
   * @return The filtered output as a List&lt;String&gt;
   */
  private static List<String> filterNextflowOutputWithOptions(
      final Map<String, Object> options,
      final Object output) {
    Options opts = new Options("filterNextflowOutput", options,
      "additionalPatterns", "sorted", "keepAnsi", "ignore", "include");
    return NextflowOutputFilter.filterNextflowOutput(
      output,
      opts.strings("additionalPatterns", List.of()),
      opts.bool("sorted", true),
      opts.bool("keepAnsi", false),
      opts.strings("ignore", List.of()),
      opts.strings("include", List.of()));
  }

  // ── OUTPUT SANITIZATION ───────────────────────────────────────────────

  /**
   * Sanitizes the output channel using default sanitization options.
   *
   * @param channel The output channel to sanitize.
   * @return A sanitized copy of the output channel.
   */
  public static TreeMap<String, Object> sanitizeOutput(
      final TreeMap<String, Object> channel) {
    return sanitizeOutput(new HashMap<String, Object>(), channel);
  }

  /**
   * Sanitizes the output channel using the provided sanitization options.
   *
   * @param options The options controlling output sanitization.
   * @param channel The output channel to sanitize.
   * @return A sanitized copy of the output channel.
   */
  public static TreeMap<String, Object> sanitizeOutput(
      final HashMap<String, Object> options,
      final TreeMap<String, Object> channel) {
    return OutputSanitizer.sanitizeOutput(options, channel);
  }

  // ── CSV ───────────────────────────────────────────────────────────────

  /**
   * Normalizes a table file and returns its canonical representation.
   *
   * @param path The table file to normalize.
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @param removeComments whether leading comment lines starting with {@code #}
   * should be removed
   * @return The normalized table content.
   */
  public static String normalizeTable(
    final Path path,
    final int digits,
    final boolean removeComments
  ) {
    return TableUtils.normalizeTable(path, digits, removeComments);
  }

  /**
   * Normalizes a table file and returns its canonical representation.
   *
   * @param path The table file to normalize.
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return The normalized table content.
   */
  public static String normalizeTable(
    final Path path,
    final int digits
  ) {
    return normalizeTable(
      path,
      digits,
      TableUtils.DEFAULT_REMOVE_COMMENTS
    );
  }

  /**
   * Normalizes a table file and returns its canonical representation.
   *
   * @param path The table file to normalize.
   * @param removeComments whether leading comment lines starting with {@code #}
   * should be removed
   * @return The normalized table content.
   */
  public static String normalizeTable(
    final Path path,
    final boolean removeComments
  ) {
    return normalizeTable(
      path,
      TableUtils.DEFAULT_DOUBLE_DIGITS,
      removeComments
    );
  }

  /**
   * Normalizes a table file and returns its canonical representation.
   *
   * @param path The table file to normalize.
   * @return The normalized table content.
   */
  public static String normalizeTable(final Path path) {
    return normalizeTable(
      path,
      TableUtils.DEFAULT_DOUBLE_DIGITS,
      TableUtils.DEFAULT_REMOVE_COMMENTS
    );
  }

  // ── ARCHIVE DOWNLOADING ───────────────────────────────────────────────

  /**
   * Extract an archive in a new temporary directory, deleted when the JVM
   * exits, with the archive type taken from the file extension.
   *
   * @param source the local path or URL of the archive
   * @return the directory the archive was extracted into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static Path extract(final String source) throws IOException {
    return ArchiveExtractor.extract(source, null, null);
  }

  /**
   * Extract an archive.
   *
   * @param options {@code extractDir}: directory to extract the archive into,
   *                instead of a new temporary directory; {@code compression}:
   *                archive type, instead of the file extension;
   *                {@code path}: file or folder to return, relative to
   *                the extract directory
   * @param source  the local path or URL of the archive
   * @return the directory the archive was extracted into, or {@code path}
   *     inside it
   * @throws IOException on failure, if archive type is unsupported or if
   *     {@code path} is not in the archive
   * @throws IllegalArgumentException on an unknown option, or a
   *     {@code path} not inside the extract directory
   */
  public static Path extract(
      final LinkedHashMap<String, Object> options,
      final String source)
      throws IOException {
    Options opts = new Options(
      "extract", options, "extractDir", "compression", "path");
    String extractDir = opts.string("extractDir", null);
    String compression = opts.string("compression", null);
    String path = opts.string("path", null);
    if (path == null) {
      return ArchiveExtractor.extract(source, extractDir, compression);
    }
    return ArchiveExtractor.extractPath(source, extractDir, compression, path);
  }

  /**
   * Deprecated: extract an archive in the given destination directory.
   *
   * @param urlString the local path or URL of the archive
   * @param destPath  directory to extract the archive into
   * @throws IOException on failure or if archive type is unsupported
   * @deprecated use {@link #extract(LinkedHashMap, String)}
   */
  @Deprecated
  public static void curlAndExtract(
      final String urlString,
      final String destPath)
      throws IOException {
    System.err.println(
      "Warning: curlAndExtract() is deprecated, use extract() instead"
    );
    ArchiveExtractor.extract(urlString, destPath, null);
  }

  /**
   * Deprecated: extract an archive in the given destination directory.
   *
   * @param urlString   the local path or URL of the archive
   * @param destPath    directory to extract the archive into
   * @param compression compression type
   * @throws IOException on failure or if archive type is unsupported
   * @deprecated use {@link #extract(LinkedHashMap, String)}
   */
  @Deprecated
  public static void curlAndExtract(
      final String urlString,
      final String destPath,
      final String compression)
      throws IOException {
    System.err.println(
      "Warning: curlAndExtract() is deprecated, use extract() instead"
    );
    ArchiveExtractor.extract(urlString, destPath, compression);
  }

  /**
   * Downloads a single file from a cloud URI to a temporary local directory
   * and returns the local {@link Path}.
   *
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails
   */
  public static Path downloadFromS3(
      final String cloudUri)
      throws IOException {
    return ArchiveExtractor.downloadFromS3(cloudUri);
  }

  /**
   * Downloads a single file from a cloud URI to a temporary local directory
   * and returns the local {@link Path}. The named options are ignored.
   *
   * @param options Ignored
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails
   * @deprecated it takes no options; use {@link #downloadFromS3(String)}
   */
  @Deprecated
  public static Path downloadFromS3(
      final LinkedHashMap<String, Object> options,
      final String cloudUri)
      throws IOException {
    System.err.println(
      "Warning: downloadFromS3() takes no options, use downloadFromS3(uri)"
      + " instead"
    );
    return ArchiveExtractor.downloadFromS3(cloudUri);
  }
}
