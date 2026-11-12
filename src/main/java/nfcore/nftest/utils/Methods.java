package nfcore.nftest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
   * @return A nested map containing the YAML data, or {@code null} if the file
   *     cannot be read.
   */
  public static Map<String, Map<String, Object>> readYamlFile(
      final String filePath) {
    return YamlUtils.readYamlFile(filePath);
  }

  /**
   * Removes the Nextflow version entry from the Workflow entry in the
   * specified Version YAML file.
   *
   * @param versionFile The YAML file path or wildcard pattern to process.
   * @return A map containing the YAML data with the Nextflow version removed.
   */
  public static Map<String, Map<String, Object>> removeNextflowVersion(
      final CharSequence versionFile) {
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
   */
  public static Map<String, Map<String, Object>> removeFromYamlMap(
      final CharSequence versionFile,
      final String key1,
      final String key2) {
    return YamlUtils.removeFromYamlMap(versionFile, key1, key2);
  }

  /**
   * Removes the specified key from a YAML map using default options.
   *
   * @param versionFile The YAML content containing the map to modify.
   * @param key1 The key to remove from the YAML map.
   * @return A map containing the updated YAML data.
   */
  public static Map<String, Map<String, Object>> removeFromYamlMap(
      final CharSequence versionFile,
      final String key1) {
    return YamlUtils.removeFromYamlMap(versionFile, key1, null);
  }

  // ── FILE TRAVERSAL ────────────────────────────────────────────────────

  /**
   * Retrieves all files from the specified directory using default options.
   *
   * @param path The path to the directory to traverse.
   * @return A list of files found in the directory.
   * @throws IOException If an error occurs while traversing the directory.
   */
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
   */
  @SuppressWarnings("unchecked")
  public static List<?> getAllFilesFromDir(
      final LinkedHashMap<String, Object> options,
      final String outdir)
      throws IOException {
    return FileTraversalUtils.getAllFilesFromDir(
      outdir,
      (Boolean) options.getOrDefault("includeDir", false),
      (List<String>) options.getOrDefault("ignore", List.of()),
      (String) options.get("ignoreFile"),
      (List<String>) options.getOrDefault("include", List.of("*", "**/*")),
      (Boolean) options.getOrDefault("relative", false));
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
   */
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
   * @param filePaths The file paths to convert.
   * @param baseDir The base directory used to calculate relative paths.
   * @return A list of relative paths.
   */
  public static List<String> getRelativePath(
      final List<File> filePaths,
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
  public static List<String> getAllFilesFromPath(
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
   * @return A sorted list of relative file paths
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  @SuppressWarnings("unchecked")
  public static List<String> getAllFilesFromPath(
      final LinkedHashMap<String, Object> options,
      final String path)
      throws IOException, InterruptedException {
    return FileTraversalUtils.getAllFilesFromPath(
      path,
      (List<String>) options.getOrDefault("ignore", List.of()),
      (List<String>) options.getOrDefault("include", List.of("**", "*")),
      (Boolean) options.getOrDefault("includeDir", false),
      (String) options.get("ignoreFile"),
      (Boolean) options.getOrDefault("noSignRequest", false));
  }

  // ── HASHING ───────────────────────────────────────────────────────────

  /**
   * Computes an MD5 hash from the string representation of each element in
   * a list.
   *
   * @param input The list of objects to include in the MD5 calculation.
   * @return The MD5 digest as a hexadecimal string.
   */
  public static String listToMD5(final List<?> input) {
    return HashUtils.listToMD5(input);
  }

  // ── nf-core MODULE MANAGEMENT ─────────────────────────────────────────

  /**
   * Creates the modules directory and .nf-core.yml configuration file.
   *
   * @param libDir The directory path to initialise an nf-core library at
   */
  public static void nfcoreInitialise(final String libDir) {
    NfCoreUtils.nfcoreInitialise(libDir);
  }

  /**
   * Installs nf-core modules from a list.
   *
   * @param libDir  An nf-core library initialised by nfcoreInitialise()
   * @param modules List of module names (strings) or module maps
   */
  public static void nfcoreInstall(final String libDir, final List<?> modules) {
    NfCoreUtils.nfcoreInstall(libDir, modules);
  }

  /**
   * Creates a symbolic link from the installed nf-core modules to the base
   * directory.
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreLink(final String libDir, final String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "link");
  }

  /**
   * Remove all linked modules from a modules directory.
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreUnlink(
      final String libDir,
      final String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "unlink");
  }

  /**
   * Delete the temporary nf-core library.
   *
   * @param libDir The library directory path to delete
   */
  public static void nfcoreDeleteLibrary(final String libDir) {
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
    return filterNextflowOutput(output, null, true, false, null, null);
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
    return filterNextflowOutput(output, null, sorted, false, null, null);
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
    return filterNextflowOutput(output, null, sorted, keepAnsi, null, null);
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
    return NextflowOutputFilter.filterNextflowOutput(
      output, additionalPatterns, sorted, keepAnsi, ignore, include);
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
    return filterNextflowOutput(
      output, additionalPatterns, true, false, null, null);
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
   * Filters Nextflow output with the options of either map overload.
   *
   * @param options The filtering options, or {@code null} to use defaults
   * @param output  The output to filter
   * @return The filtered output as a List&lt;String&gt;
   */
  @SuppressWarnings("unchecked")
  private static List<String> filterNextflowOutputWithOptions(
      final Map<String, Object> options,
      final Object output) {
    Map<String, Object> opts = Map.of();
    if (options != null) {
      opts = options;
    }
    return filterNextflowOutput(
      output,
      (List<String>) opts.get("additionalPatterns"),
      !Boolean.FALSE.equals(opts.get("sorted")),
      Boolean.TRUE.equals(opts.get("keepAnsi")),
      (List<String>) opts.get("ignore"),
      (List<String>) opts.get("include"));
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
   *                archive type, instead of the file extension
   * @param source  the local path or URL of the archive
   * @return the directory the archive was extracted into
   * @throws IOException on failure or if archive type is unsupported
   */
  public static Path extract(
      final LinkedHashMap<String, Object> options,
      final String source)
      throws IOException {
    return ArchiveExtractor.extract(
      source,
      Objects.toString(options.get("extractDir"), null),
      Objects.toString(options.get("compression"), null)
    );
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
   * @throws InterruptedException if the process is interrupted
   */
  public static Path downloadFromS3(
      final String cloudUri)
      throws IOException, InterruptedException {
    return ArchiveExtractor.downloadFromS3(cloudUri);
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
    return ArchiveExtractor.downloadFromS3(cloudUri);
  }
}
