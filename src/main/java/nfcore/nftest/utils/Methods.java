package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Locale;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public final class Methods {

  private Methods() {
  }

  /**
   * Reads a Version YAML file and returns its contents as a nested map.
   *
   * @param filePath The path to the YAML file to read.
   * @return A nested map containing the YAML data, or {@code null} if the file
   *     cannot be read.
   */
  public static Map<String, Map<String, Object>> readYamlFile(
      final String filePath) {
    Yaml yaml = new Yaml();
    try (FileReader reader = new FileReader(filePath)) {
      Map<String, Map<String, Object>> data = yaml.load(reader);
      return data;
    } catch (IOException e) {
      System.err.println("Error reading YAML file: " + e.getMessage());
      return null;
    }
  }

  /**
   * Resolves a file path or wildcard pattern to a list of matching file paths.
   *
   * @param pathPattern The file path or wildcard pattern to resolve.
   * @return A sorted list of matching absolute file paths.
   * @throws IOException If the parent directory does not exist, no files match
   *     the pattern, or an error occurs while accessing the directory.
   */
    private static List<String> resolveWildcardPaths(
      final String pathPattern)
      throws IOException {
    // If no wildcard, return single item list
    if (!pathPattern.contains("*") && !pathPattern.contains("?")) {
      return Arrays.asList(pathPattern);
    }

    Path pattern = Paths.get(pathPattern);
    Path parent = pattern.getParent();
    String fileName = pattern.getFileName().toString();

    // If parent is null, use current directory
    if (parent == null) {
      parent = Paths.get(".");
    }

    // Check if parent directory exists
    if (!Files.exists(parent) || !Files.isDirectory(parent)) {
      throw new IOException(
        "Parent directory does not exist: "
        + parent
      );
    }

    PathMatcher matcher = FileSystems
      .getDefault()
        .getPathMatcher("glob:" + fileName);

    // Find matching files in the parent directory
    try {
      List<String> matchingFiles = Files.list(parent)
          .filter(Files::isRegularFile)
          .filter(path -> matcher.matches(path.getFileName()))
          .sorted()
          .map(path -> path.toAbsolutePath().toString())
          .collect(Collectors.toList());

      if (matchingFiles.isEmpty()) {
        throw new IOException(
          "No files found matching pattern: "
          + pathPattern
        );
      }

      return matchingFiles;
    } catch (IOException e) {
      throw new IOException(
        "Error resolving wildcard pattern "
        + pathPattern + ": " + e.getMessage()
      );
    }
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
    return removeFromYamlMap(versionFile, "Workflow", "Nextflow");
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
    String yamlFilePattern = versionFile.toString();
    Map<String, Map<String, Object>> mergedResult = new TreeMap<>();

    try {
      // Resolve wildcard patterns if present - now returns all matching files
      List<String> yamlFilePaths = resolveWildcardPaths(yamlFilePattern);

      for (String yamlFilePath : yamlFilePaths) {
        Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

        if (yamlData != null) {
          // Process each file's data
          if (yamlData.containsKey(key1)) {
            if (key2 == null || key2.isEmpty()) {
              // Remove the entire key1 entry
              yamlData.remove(key1);
            } else {
              // Remove only the specific key2 from key1
              yamlData.get(key1).remove(key2);
            }
          }

          // Merge the processed data into the result
          for (
              Map.Entry<String, Map<String, Object>> entry
              : yamlData.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> value = entry.getValue();

            if (mergedResult.containsKey(key)) {
              // If key already exists, merge the inner maps
              mergedResult.get(key).putAll(value);
            } else {
              // If key doesn't exist, add it (also sorted)
              mergedResult.put(key, new TreeMap<>(value));
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println(
        "Error resolving file path pattern: " + e.getMessage()
      );
      return null;
    }

    return mergedResult;
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
    return removeFromYamlMap(versionFile, key1, null);
  }

  /**
   * Retrieves all files from the specified directory using default options.
   *
   * @param path The path to the directory to traverse.
   * @return A list of files found in the directory.
   * @throws IOException If an error occurs while traversing the directory.
   */
    public static List getAllFilesFromDir(
      final String path)
      throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  /**
   * Retrieves files from an output directory using options provided in a map.
   *
   * @param options Options controlling directory traversal and filtering.
   * @param outdir The root output directory to traverse.
   * @return A list of matching files or relative paths when {@code relative}
   *     is enabled.
   * @throws IOException If an error occurs while traversing the directory or
   *     reading the ignore patterns file.
   * @throws IllegalArgumentException If {@code outdir} is null, empty, does
   *     not exist, or is not a directory.
   */
  public static List getAllFilesFromDir(
      final LinkedHashMap<String, Object> options,
      final String outdir)
      throws IOException {
    if (outdir == null || outdir.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'outdir' parameter is required."
      );
    }
    // Check if path exists
    Path dirPath = Paths.get(outdir);
    if (!Files.exists(dirPath)) {
      throw new IllegalArgumentException(
        "The specified path does not exist: " + outdir
      );
    }

    // Check if it's a directory
    if (!Files.isDirectory(dirPath)) {
      throw new IllegalArgumentException(
        "The specified path is not a directory: " + outdir
      );
    }

    // Extract optional parameters from the map (use defaults if not provided)
    Boolean includeDir = (Boolean) options
      .getOrDefault("includeDir", false);
    List<String> ignoreGlobs = (List<String>) options
      .getOrDefault("ignore", new ArrayList<String>());
    String ignoreFilePath = (String) options
      .get("ignoreFile");
    Boolean relative = (Boolean) options
      .getOrDefault("relative", false);
    List<String> includeGlobs = (List<String>) options
      .getOrDefault("include", Arrays.asList("*", "**/*"));

    List<File> files = getAllFilesFromDir(
      outdir, includeDir, ignoreGlobs,
      ignoreFilePath, includeGlobs);

    if (relative) {
      return getRelativePath(files, outdir);
    } else {
      return files;
    }
  }

  /**
   * Recursively retrieves files and optionally directories from an output
   * directory, applying include and exclude glob patterns.
   *
   * @param outdir The root output directory to traverse.
   * @param includeDir Whether directories should be included in the result.
   * @param ignoreGlobs Glob patterns identifying files or directories to
   *     exclude.
   * @param ignoreFilePath Path to a file containing additional ignore glob
   *     patterns.
   * @param includeGlobs Glob patterns identifying files or directories to
   *     include.
   * @return A sorted list of matching files and, if enabled, directories.
   * @throws IOException If an error occurs while traversing the directory or
   *     reading the ignore patterns file.
   */
  public static List<File> getAllFilesFromDir(
      final String outdir,
      final boolean includeDir,
      final List<String> ignoreGlobs,
      final String ignoreFilePath,
      final List<String> includeGlobs)
      throws IOException {
    List<File> output = new ArrayList<>();
    Path directory = Paths.get(outdir);

    List<String> allIgnoreGlobs = new ArrayList<>();
    if (ignoreGlobs != null) {
      allIgnoreGlobs.addAll(ignoreGlobs);
    }
    if (ignoreFilePath != null && !ignoreFilePath.isEmpty()) {
      allIgnoreGlobs.addAll(readGlobsFromFile(ignoreFilePath));
    }

    List<PathMatcher> excludeMatchers = new ArrayList<>();
    for (String glob : allIgnoreGlobs) {
      excludeMatchers.add(
        FileSystems.getDefault().getPathMatcher("glob:" + glob)
      );
    }

    List<String> allIncludeGlobs = new ArrayList<>();
    if (includeGlobs != null) {
      allIncludeGlobs.addAll(includeGlobs);
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : allIncludeGlobs) {
      includeMatchers.add(
        FileSystems.getDefault().getPathMatcher("glob:" + glob)
      );
    }

    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(
              final Path file,
              final BasicFileAttributes attrs) {
            if (isIncluded(file) && !isExcluded(file)) {
              output.add(file.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir,
              final BasicFileAttributes attrs) {
            // Exclude output which is the root output folder from nf-test
            if (
                includeDir
                && (isIncluded(dir)
                && !isExcluded(dir)
                && !dir.getFileName().toString().equals("output"))) {
              output.add(dir.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          private boolean isExcluded(final Path path) {
            return excludeMatchers
              .stream()
              .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }

          private boolean isIncluded(final Path path) {
            return includeMatchers
              .stream()
              .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }
        });

    return output
      .stream()
      .sorted(Comparator.comparing(File::getPath))
      .collect(Collectors.toList());
  }

  /**
   * Reads glob patterns from a file, ignoring empty lines and
   * surrounding whitespace.
   *
   * @param filePath The path to the file containing glob patterns.
   * @return A list of glob patterns read from the file.
   * @throws IOException If an error occurs while reading the file.
   */
  private static List<String> readGlobsFromFile(
      final String filePath)
      throws IOException {
    List<String> globs = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) {
          globs.add(line);
        }
      }
    }
    return globs;
  }

  /**
   * Converts a list of file paths to paths relative to the specified base
   * directory.
   *
   * @param filePaths The file paths to convert.
   * @param baseDir The base directory used to calculate relative paths.
   * @return A list of paths relative to the specified base directory.
   */
  public static List<String> getRelativePath(
      final List<File> filePaths,
      final String baseDir) {
    Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();

    return filePaths
        .stream()
        .map(filePath -> {
          Path path = Paths.get(filePath.toURI()).toAbsolutePath().normalize();
          return basePath.relativize(path).toString();
        })
        .collect(Collectors.toList());
  }

  /**
   * Bit mask used to convert a signed byte to its unsigned representation.
   */
  private static final int BYTE_MASK = 0xff;

  /**
   * Computes an MD5 hash from the string representation of each element in
   * a list.
   *
   * @param input The list of objects to include in the MD5 calculation.
   * @return The MD5 digest as a hexadecimal string.
   * @throws UnsupportedEncodingException If UTF-8 encoding is not supported.
   */
  public static String listToMD5(
      final ArrayList<Object> input)
      throws UnsupportedEncodingException {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      Iterator<Object> inputIterator = input.iterator();
      while (inputIterator.hasNext()) {
        md5.update(inputIterator.next().toString().getBytes("UTF-8"));
      }
      byte[] digest = md5.digest();

      // Convert byte array to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : digest) {
        String hex = Integer.toHexString(BYTE_MASK & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

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
   * @param modules List of module names (strings) or module maps with keys:
   *  name (required), sha (optional), remote (optional)
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

  /**
   * Filters Nextflow stdout/stderr output to remove variable content that makes
   * snapshots unstable.
   * This method removes common patterns like timestamps, execution IDs, memory
   * usage, and other
   * runtime-specific information to make test snapshots reproducible.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @return The filtered output as a List<String> with unstable patterns
   * removed
   */
  public static List<String> filterNextflowOutput(final Object output) {
    return filterNextflowOutput(output, null, true, false, null, null);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @param sorted Whether to sort the output lines alphabetically
   * @return The filtered output as a List<String> with unstable patterns
   * removed
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
   * @param keepAnsi Whether to keep ANSI escape codes (colors, formatting)
   * @return The filtered output as a List<String> with unstable patterns
   * removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final boolean sorted,
      final boolean keepAnsi) {
    return filterNextflowOutput(output, null, sorted, keepAnsi, null, null);
  }

  /**
   * Sanitizes a Nextflow output line by replacing non-deterministic values with
   * stable placeholders, including usernames, timestamps, hashes, paths, run
   * names, container engines, and software versions.
   *
   * @param line The output line to sanitize.
   * @param capturedRunName The run name captured from the Nextflow launching
   *     line, or {@code null} if no run name was captured.
   * @return The sanitized output line with non-deterministic values replaced by
   *     stable placeholders.
   */
  public static String filterLinePattern(
      final String line,
      final String capturedRunName) {
    String filtered = line;

    // Replace username value in patterns like "userName : max"
    String userName = System.getenv("USER");
    if (userName != null && !userName.isEmpty()) {
      filtered = filtered.replaceAll(
        "(userName\\s*:\\s*)" + java.util.regex.Pattern.quote(userName),
        "$1[USER]");
    }

    // Remove timestamp patterns

    // ISO 8601 related formats:
    // YYY-MM-DDTHH:mm:ss
    // YYY-MM-DD HH:mm:ss
    // YYY-MM-DD_HH-mm-ss
    filtered = filtered.replaceAll(
        "\\d{4}-\\d{2}-\\d{2}[T\\s_]\\d{2}[:-]\\d{2}[:-]\\d{2}"
        + "(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?",
        "[TIMESTAMP]");
    // US date format: MM/DD/YYY HH:mm:ss
    filtered = filtered.replaceAll(
      "\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}",
      "[TIMESTAMP]");

    // Remove Nextflow process execution hashes (format: [xx/yyyyyy])
    filtered = filtered.replaceAll(
      "\\[[0-9a-f]{2}/[0-9a-f]{6}\\]",
      "[NXF_HASH]");

    // Remove NFT_HASH work dir (format: [xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx])
    filtered = filtered.replaceAll(
      "\\b[0-9a-f]{30,32}\\b",
      "[NFT_HASH]");

    // Remove revision hashes (format: revision: abc1234)
    filtered = filtered.replaceAll(
      "revision: [0-9a-f]{10}",
      "revision: [REVISION]");

    // Remove Nextflow version update notifications
    filtered = filtered.replaceAll(
      ".*Nextflow\\s+\\d+\\.\\d+\\.\\d+.*is available.*",
      "");
    filtered = filtered.replaceAll(
      ".*Please consider updating your version.*",
      "");

    // Replace absolute paths with [PATH] placeholder using a more
    // general approach
    filtered = filterAbsolutePaths(filtered);

    // Remove run name using captured run name from launching line
    if (capturedRunName != null) {
      // Replace bracketed run name: [run_name]
      filtered = filtered.replace(
        "[" + capturedRunName + "]",
        "[RUN_NAME]");
      // Replace unbracketed run name: run_name
      filtered = filtered.replace(
        capturedRunName,
        "[RUN_NAME]");
    }

    // Remove containerEngine messages
    // as it's docker and singularity specific, but not conda
    filtered = filtered.replaceAll(".*containerEngine.*", "");

    // Replace common reproducibility solutions (ie virtualenv or container)
    // by [CONTAINER] - All of theses are profiles in nf-core TEMPLATE
    // I know that none all of these are actually containers, but it's a
    // good quick approximation
    filtered = filtered.replaceAll("apptainer", "[CONTAINER]");
    filtered = filtered.replaceAll("charliecloud", "[CONTAINER]");
    filtered = filtered.replaceAll("conda", "[CONTAINER]");
    filtered = filtered.replaceAll("docker", "[CONTAINER]");
    filtered = filtered.replaceAll("mamba", "[CONTAINER]");
    filtered = filtered.replaceAll("podman", "[CONTAINER]");
    filtered = filtered.replaceAll("shifter", "[CONTAINER]");
    filtered = filtered.replaceAll("singularity", "[CONTAINER]");
    filtered = filtered.replaceAll("wave", "[CONTAINER]");

    // Replace nf-core pipeline versions (e.g., "nf-core/xxx yyyy")
    filtered = filtered.replaceAll(
      "(nf-core/[^\\s]+\\s+)\\d+\\.\\d+(?:\\.\\d+)?[a-zA-Z]*",
      "$1[VERSION]");

    // Replace NEXTFLOW versions
    filtered = filtered.replaceAll(
      "N E X T F L O W  ~  version \\d+\\.\\d+\\.\\d+(-edge)?",
      "N E X T F L O W  ~  version [VERSION]");

    return filtered;
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns, optional
   * sorting, and ANSI code handling.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove from
   *                           the output
   * @param sorted             Whether to sort the output lines alphabetically
   * @param keepAnsi           Whether to keep ANSI escape codes (colors,
   *                           formatting)
   * @param ignore             List of strings to filter out from the output
   *                           (lines containing any of these strings will be
   *                           removed)
   * @param include            List of strings to include in the output (only
   *                           lines containing at least one of these strings
   *                           will be kept). If null or empty, all lines are
   *                           considered for inclusion.
   * @return The filtered output as a List<String> with unstable patterns
   * removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns,
      final boolean sorted,
      final boolean keepAnsi,
      final List<String> ignore,
      final List<String> include) {
    if (output == null) {
      return new ArrayList<>();
    }

    List<String> outputLines;
    if (output instanceof List) {
      // Handle workflow.stdout and workflow.stderr which are Lists
      List<?> outputList = (List<?>) output;
      if (outputList.isEmpty()) {
        return new ArrayList<>();
      }
      outputLines = outputList.stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    } else if (output instanceof String) {
      String outputString = (String) output;
      if (outputString.isEmpty()) {
        return new ArrayList<>();
      }
      // Split string into lines
      outputLines = Arrays.asList(outputString.split("\n"));
    } else {
      // Convert any other type to string and split into lines
      String outputString = output.toString();
      outputLines = Arrays.asList(outputString.split("\n"));
    }

    // Filter each line
    List<String> filteredLines = new ArrayList<>();
    String capturedRunName = null;

    for (String line : outputLines) {
      String filtered = line;

      // Filter out lines containing any of the ignore strings
      if (ignore != null && !ignore.isEmpty()) {
        boolean shouldIgnore = false;
        for (String ignoreString : ignore) {
          if (filtered.contains(ignoreString)) {
            shouldIgnore = true;
            break;
          }
        }
        if (shouldIgnore) {
          continue; // Skip this line
        }
      }

      // Filter to include only lines containing any of the include strings
      if (include != null && !include.isEmpty()) {
        boolean shouldInclude = false;
        for (String includeString : include) {
          if (filtered.contains(includeString)) {
            shouldInclude = true;
            break;
          }
        }
        if (!shouldInclude) {
          continue; // Skip this line
        }
      }

      // Strip ANSI escape codes unless keepAnsi is true
      // (colors, formatting, etc.)
      if (!keepAnsi) {
        filtered = filtered.replaceAll(
          "\\x1B\\[[0-9;]*[A-Za-z]", "");
      }

      // Capture run name from launching line
      if (
          capturedRunName == null
          && filtered.contains("Launching")
          && filtered.contains("[")
          && filtered.contains("]")) {
        java.util.regex.Pattern runNamePattern =
          java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = runNamePattern.matcher(filtered);
        if (matcher.find()) {
          capturedRunName = matcher.group(1);
        }
      }

      filtered = filterLinePattern(filtered, capturedRunName);

      // Only add non-empty lines (filter out empty lines)
      if (!filtered.trim().isEmpty()) {
        filteredLines.add(filtered);
      }
    }

    // Sort and remove duplicates if requested
    if (sorted) {
      // Separate lines that should be sorted from those that
      // should preserve order
      List<String> sortableLines = new ArrayList<>();
      List<String> preserveOrderLines = new ArrayList<>();

      for (String line : filteredLines) {
        if (line.contains("Staging foreign file")
            || line.contains("Submitted process")
            || line.startsWith("Creating env using conda:")
            || line.startsWith("Pulling Singularity image")
            || line.startsWith("ERROR ~")
            || line.startsWith("WARN:")
            || (
              line.contains("Check ")
              && line.contains(" file for details")
            )) {
          sortableLines.add(line);
        } else {
          preserveOrderLines.add(line);
        }
      }

      // Sort only the sortable lines
      Collections.sort(sortableLines);

      // Combine lists: preserve-order lines first, then sorted lines
      List<String> combinedLines = new ArrayList<>();
      combinedLines.addAll(preserveOrderLines);
      combinedLines.addAll(sortableLines);

      // Remove duplicates while preserving the new order
      List<String> uniqueLines = new ArrayList<>();
      String lastLine = null;
      for (String line : combinedLines) {
        if (!line.equals(lastLine)) {
          uniqueLines.add(line);
          lastLine = line;
        }
      }
      filteredLines = uniqueLines;
    }

    return filteredLines;
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns.
   * This overloaded method allows specifying additional patterns to filter.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove from
   *                           the output
   * @return The filtered output as a List<String> with unstable patterns
   * removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns) {
    return filterNextflowOutput(
      output, additionalPatterns,
      true, false, null, null
    );
  }

  /**
   * Filters Nextflow stdout/stderr output using Groovy's named parameter
   * syntax.
   *
   * This allows calling: filterNextflowOutput(output, sorted: false, keepAnsi:
   * true, ignore: ["Staging foreign file"], include: ["ERROR", "WARN"])
   *
   * @param output  The stdout or stderr output (String or List) to filter
   * @param options Map containing filtering options (automatically created by
   *                Groovy named params):
   *                - additionalPatterns: List<String> of additional regex
   *                patterns (optional)
   *                - sorted: Boolean whether to sort the output (default: true)
   *                - keepAnsi: Boolean whether to keep ANSI codes (default:
   *                false)
   *                - ignore: List<String> of strings to filter out (lines
   *                containing any of these strings will be removed) (optional)
   *                - include: List<String> of strings to include (only lines
   *                containing at least one of these strings will be kept)
   *                (optional)
   * @return The filtered output as a List<String> with unstable patterns
   * removed
   */

  // Handle Groovy named parameters: filterNextflowOutput(output, keepAnsi:
  // true)
  // Groovy converts this to: filterNextflowOutput([keepAnsi: true], output)
  public static List<String> filterNextflowOutput(
      final LinkedHashMap<String, Object> options,
      final Object output) {
      final Map<String, Object> optionsFixed = options == null
        ? new HashMap<>()
        : options;

    // Extract options with defaults
    List<String> additionalPatterns = (List<String>) optionsFixed
      .get("additionalPatterns");
    Boolean sorted = (Boolean) optionsFixed.get("sorted");
    Boolean keepAnsi = (Boolean) optionsFixed.get("keepAnsi");
    List<String> ignore = (List<String>) optionsFixed.get("ignore");
    List<String> include = (List<String>) optionsFixed.get("include");

    // Apply defaults
    if (sorted == null) {
      sorted = true;
    }
    if (keepAnsi == null) {
      keepAnsi = false;
    }
    return filterNextflowOutput(
      output, additionalPatterns,
      sorted, keepAnsi, ignore, include
    );
  }

  /**
   * Filters Nextflow output using options provided in a map, applying default
   * values for unspecified options.
   *
   * @param output The Nextflow output to filter.
   * @param options The filtering options, or {@code null} to use defaults.
   * @return A list of filtered output lines.
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final Map<String, Object> options) {
    final Map<String, Object> optionsFixed = options == null
      ? new HashMap<>()
      : options;

    // Extract options with defaults
    List<String> additionalPatterns = (List<String>) optionsFixed
      .get("additionalPatterns");
    Boolean sorted = (Boolean) optionsFixed.get("sorted");
    Boolean keepAnsi = (Boolean) optionsFixed.get("keepAnsi");
    List<String> ignore = (List<String>) optionsFixed.get("ignore");
    List<String> include = (List<String>) optionsFixed.get("include");

    // Apply defaults
    if (sorted == null) {
      sorted = true;
    }
    if (keepAnsi == null) {
      keepAnsi = false;
    }
    return filterNextflowOutput(
      output, additionalPatterns,
      sorted, keepAnsi, ignore, include
    );
  }

  /**
   * Filters absolute paths in the given text and replaces them with [PATH]
   * placeholder.
   *
   * @param text The text to filter
   * @return The filtered text with various directory paths replaced with [PATH]
   */
  private static String filterAbsolutePaths(final String text) {
    String filtered = text;

    // Collect all paths to replace, then sort by length (longest first)
    // This ensures more specific paths are replaced before their parent paths
    List<String> pathsToReplace = new ArrayList<>();

    // Get the current working directory
    String workingDir = System.getProperty("user.dir");
    if (workingDir != null) {
      pathsToReplace.add(workingDir);
    }

    // Check for various environment variables
    String[] envVars = {
        "HOME",
        "NFT_WORKDIR",
        "NXF_CACHE_DIR",
        "NXF_CONDA_CACHEDIR",
        "NXF_HOME",
        "NXF_SINGULARITY_CACHEDIR",
        "NXF_SINGULARITY_LIBRARYDIR",
        "NXF_TEMP",
        "NXF_WORK"
    };

    for (String envVar : envVars) {
      String envValue = System.getenv(envVar);
      if (envValue != null && !envValue.isEmpty() && !envValue.equals("~")) {
        pathsToReplace.add(envValue);
      }
    }

    // Handle default NXF_HOME case: if NXF_HOME is null, Nextflow uses
    // $HOME/.nextflow
    String nxfHome = System.getenv("NXF_HOME");
    if (nxfHome == null || nxfHome.isEmpty()) {
      String home = System.getenv("HOME");
      if (home != null && !home.isEmpty() && !home.equals("~")) {
        pathsToReplace.add(home + "/.nextflow");
      }
    }

    // Remove duplicates and sort paths by length to avoid partial replacements
    pathsToReplace = pathsToReplace.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(java.util.stream.Collectors.toList());

    // Replace all paths with [PATH] in order of longest first
    for (String path : pathsToReplace) {
      filtered = filtered.replace(path, "[PATH]");
    }

    return filtered;
  }

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

  /**
   * Download a tar archive and extract it in the given destination directory.
   * The file is streamed directly with `curl` into `tar` via a pipe.
   * The compression type must be provided if applicable.
   * Uses safe single-quoting for the URL and destination path.
   *
   * @param urlString   the URL to fetch
   * @param destPath    directory to extract the tarball into
   * @param compression compression type: tar, gzip, gz, bzip2, bz2, xz, lz4,
   *                    lzma, lzop, zstd
   *                    or any of these prefixed with "tar." or "t"
   * @throws IOException on failure
   */
  private static void curlAndUntar(
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

    // Convert tarExt name to tar option
    if (tarExt != null && !tarExt.equals("tar")) {
      // Remove leading "tar." or "t" if present
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
    try {
      Utils.ProcessResult result = Utils.runProcess(pb);
      if (result.getExitCode() != 0) {
        System.err
            .println(
              "Error downloading and extracting file "
              + urlString + ": exit code "
              + result.getExitCode() + "\n"
            );
        System.out.println("Bash command: \n" + cmd);
        System.err.println("command output: \n");
        System.err.println(result.getStderr());
      } else {
        System.out.println(
          "Successfully downloaded and extracted file: "
          + urlString
        );
      }
    } catch (IOException | InterruptedException e) {
      System.err.println(
        "Error downloading and extracting file "
        + urlString + ": " + e.getMessage()
      );
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Download a zip file and extract it in the given destination directory.
   * The file is first downloaded with `curl` to a temporary file, then we
   * call `unzip` and delete the temporary file.
   *
   * @param urlString the URL to fetch
   * @param destPath  directory to extract the zip into
   * @throws IOException on failure
   */
  private static void curlAndUnzip(
      final String urlString,
      final String destPath)
      throws IOException {
    Path destDir = Paths.get(destPath);
    Files.createDirectories(destDir);

    // Create a temporary file in the destination directory for the
    // downloaded zip
    Path tempFile = Files.createTempFile(destDir, "download", ".zip");

    // Run curl
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
        System.err.println(
          "Error downloading file " + urlString
          + ": exit code " + result.getExitCode() + "\n"
        );
        System.out.println("Command: " + String.join(" ", pb.command()));
        System.err.println("command output: \n");
        System.err.println(result.getStderr());
        return;
      }
      // Run unzip
      pb = new ProcessBuilder(
          "unzip",
          "-o",
          tempFile.toString(),
          "-d",
          destPath);
      result = Utils.runProcess(pb);
      if (result.getExitCode() != 0) {
        System.err.println(
          "Error extracting zip " + tempFile
          + ": exit code " + result.getExitCode() + "\n"
        );
        System.out.println("Command: " + String.join(" ", pb.command()));
        System.err.println("command output: \n");
        System.err.println(result.getStderr());
      } else {
        System.out.println(
          "Successfully downloaded and extracted file: "
          + urlString
        );
      }
    } catch (IOException | InterruptedException e) {
      System.err.println(
        "Error downloading and extracting file "
        + urlString + ": " + e.getMessage()
      );
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        // Do not fail the operation if temp file cleanup fails; just log it
        System.err.println(
          "Warning: failed to delete temporary file "
          + tempFile + ": " + e.getMessage()
        );
      }
    }
  }

  /**
   * Get all file paths from a Nextflow channel output.
   *
   * This method collects, flattens, and filters a channel to return only
   * absolute file paths (strings starting with "/").
   * Maps and non-absolute paths are filtered out.
   *
   * @param channel the channel output to process (typically a Groovy
   * collection)
   * @return a flattened list containing only absolute file paths
   */
  public static List getAllFilesFromChannel(final Object channel) {
    List result = new ArrayList<>();

    if (channel == null) {
      return result;
    }

    // Flatten and filter the channel
    flattenAndFilter(channel, result);

    return result;
  }

  /**
   * Helper method to recursively flatten nested collections and filter items.
   * Keeps only String items that start with "/" (absolute paths), excludes
   * Maps.
   *
   * @param obj The object to recursively flatten and filter.
   * @param result The list to which matching absolute paths are added.
   */
  private static void flattenAndFilter(
      final Object obj,
      final List result) {
    if (obj == null) {
      return;
    }

    // Skip Maps
    if (obj instanceof Map) {
      return;
    }

    // If it's a collection/iterable, recursively process each element
    if (obj instanceof Iterable) {
      for (Object item : (Iterable) obj) {
        flattenAndFilter(item, result);
      }
    } else if (obj instanceof String) { // For strings
      String str = (String) obj;
      if (str.startsWith("/")) {
        result.add(str);
      }
    } else if (obj.getClass().isArray()) { // For arrays
      int length = java.lang.reflect.Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        flattenAndFilter(java.lang.reflect.Array.get(obj, i), result);
      }
    }
  }

  /**
   * Download an archive and extract it in the given destination directory.
   * Dispatches to `curlAndUnzip` for ZIP files and to `curlAndUntar` for
   * tar archives based on the URL's file extension.
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
   * Lists all files at the given path, returning sorted relative paths.
   *
   * <p>Supports local paths and S3 URIs. S3 URIs are listed via the AWS CLI.
   *
   * @param path The path to list – a local directory or an S3 URI
   *             (e.g. {@code "s3://my-bucket/results/"})
   * @return A sorted list of relative file paths under {@code path}
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
   * relative paths, with filtering options. Uses Groovy named-parameter
   * syntax:
   * {@code getAllFilesFromPath(path, ignore: ['*.log'], include: ['**'])}
   *
   * <p>Supported options:
   * <ul>
   *   <li>{@code ignore} – {@code List<String>} of glob patterns to exclude
   *       (matched against the relative path, e.g.
   *       {@code ['pipeline_info/**']})</li>
   *   <li>{@code include} – {@code List<String>} of glob patterns to include
   *       (default: {@code ["**", "*"]})</li>
   *   <li>{@code includeDir} – {@code Boolean} also emit directory entries
   *       (default: {@code false})</li>
   *   <li>{@code ignoreFile} – {@code String} path to a local file whose lines
   *       are treated as additional ignore globs
   *       (e.g. {@code ".nftignore"})</li>
   *   <li>{@code noSignRequest} – {@code Boolean} pass to the AWS CLI
   *       {@code --no-sign-request} when listing a public S3 bucket without
   *       credentials (default: {@code false})</li>
   * </ul>
   *
   * @param options Named options map (automatically created by Groovy named
   * params)
   * @param path    The path to list – a local directory or a cloud URI
   * @return A sorted list of relative file paths under {@code path}
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<String> getAllFilesFromPath(
      final LinkedHashMap<String, Object> options,
      final String path)
      throws IOException, InterruptedException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'path' parameter is required."
      );
    }

    List<String> ignoreGlobs =
        (List<String>) options.getOrDefault(
          "ignore", new ArrayList<String>()
        );
    List<String> includeGlobs =
        (List<String>) options.getOrDefault(
          "include", Arrays.asList("**", "*")
        );
    Boolean includeDir = (Boolean) options
      .getOrDefault("includeDir", false);
    String ignoreFilePath = (String) options.get("ignoreFile");
    Boolean noSignRequest = (Boolean) options
      .getOrDefault("noSignRequest", false);

    List<String> allIgnoreGlobs = new ArrayList<>(ignoreGlobs);
    if (ignoreFilePath != null && !ignoreFilePath.isEmpty()) {
      allIgnoreGlobs.addAll(readGlobsFromFile(ignoreFilePath));
    }

    List<PathMatcher> excludeMatchers = new ArrayList<>();
    for (String glob : allIgnoreGlobs) {
      if (glob != null && !glob.isEmpty()) {
        excludeMatchers.add(FileSystems
          .getDefault()
          .getPathMatcher("glob:" + glob)
        );
      }
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : includeGlobs) {
      if (glob != null && !glob.isEmpty()) {
        includeMatchers.add(FileSystems
          .getDefault()
          .getPathMatcher("glob:" + glob)
        );
      }
    }

    if (path.startsWith("s3://")) {
      return getAllFilesFromS3ViaCli(
        path, includeMatchers, excludeMatchers,
        includeDir, noSignRequest
      );
    }

    Path root = Paths.get(path);
    List<String> files = new ArrayList<>();

    Files.walkFileTree(
        root,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(
              final Path file,
              final BasicFileAttributes attrs) {
            String relative = root.relativize(file).toString();
            if (relative.isEmpty()) {
              return FileVisitResult.CONTINUE;
            }
            Path relLocal = Paths.get(relative);
            boolean included =
                includeMatchers.isEmpty()
                    || includeMatchers
                      .stream()
                      .anyMatch(m -> m.matches(relLocal));
            boolean excluded = excludeMatchers
              .stream()
              .anyMatch(m -> m.matches(relLocal));
            if (included && !excluded) {
              files.add(relative);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir,
              final BasicFileAttributes attrs) {
            if (dir.equals(root)) {
              return FileVisitResult.CONTINUE;
            }
            if (includeDir) {
              String relative = root.relativize(dir).toString();
              if (!relative.isEmpty()) {
                Path relLocal = Paths.get(relative);
                boolean included =
                    includeMatchers.isEmpty()
                        || includeMatchers
                          .stream()
                          .anyMatch(m -> m.matches(relLocal));
                boolean excluded = excludeMatchers
                  .stream()
                  .anyMatch(m -> m.matches(relLocal));
                if (included && !excluded) {
                  files.add(relative);
                }
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });

    return files.stream().sorted().collect(Collectors.toList());
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
   * Lists files under an S3 prefix using the AWS CLI
   * ({@code aws s3 ls --recursive}).
   *
   * It applies the same include/exclude glob filtering used by the local
   * walk. S3 key suffixes ending in {@code /} are treated as directory
   * markers and emitted only when {@code includeDir} is {@code true}.
   *
   * @param s3Path The S3 path or prefix to list.
   * @param includeMatchers Path matchers for files to include.
   * @param excludeMatchers Path matchers for files to exclude.
   * @param includeDir Whether directory markers should be included in the
   * results.
   * @param noSignRequest Whether to use the AWS CLI {@code --no-sign-request}
   * option.
   *
   * @return A sorted list of S3 object keys matching the include and exclude
   * filters.
   */
  private static List<String> getAllFilesFromS3ViaCli(
      final String s3Path,
      final List<PathMatcher> includeMatchers,
      final List<PathMatcher> excludeMatchers,
      final boolean includeDir,
      final boolean noSignRequest)
      throws IOException, InterruptedException {

    String normalizedPath = s3Path.endsWith("/") ? s3Path : s3Path + "/";
    String bucketAndPrefix = normalizedPath.substring("s3://".length());
    int firstSlash = bucketAndPrefix.indexOf('/');
    String prefix = firstSlash >= 0
      ? bucketAndPrefix.substring(firstSlash + 1)
      : "";

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

    BufferedReader reader = new BufferedReader(
      new InputStreamReader(process.getInputStream())
    );
    List<String> files = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      // Output format: "2024-01-01 12:00:00      12345 prefix/path/to/file.txt"
      String[] parts = line.trim().split("\\s+", AWS_S3_LIST_FIELDS);
      if (parts.length < AWS_S3_LIST_FIELDS) {
        continue;
      }

      String fullKey = parts[AWS_S3_KEY_INDEX];
      String relativePath = (!prefix.isEmpty() && fullKey.startsWith(prefix))
          ? fullKey.substring(prefix.length())
          : fullKey;
      if (relativePath.isEmpty()) {
        continue;
      }

      boolean isDir = relativePath.endsWith("/");
      if (isDir && !includeDir) {
        continue;
      }

      Path relPath = Paths.get(
        isDir
        ? relativePath.substring(0, relativePath.length() - 1)
        : relativePath
      );
      boolean included = includeMatchers.isEmpty()
          || includeMatchers.stream().anyMatch(m -> m.matches(relPath));
      boolean excluded = excludeMatchers
        .stream()
        .anyMatch(m -> m.matches(relPath));
      if (included && !excluded) {
        files.add(relativePath);
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
   * The destination path mirrors the key structure under a plugin-specific
   * temp directory so repeated calls for the same URI are idempotent.
   *
   * <p>Uses {@code nextflow fs cp} under the hood, so any cloud provider
   * supported by Nextflow (S3, GCS, Azure) is transparently handled.
   * Authentication is configured via the project's {@code nextflow.config}.
   *
   * <p>Uses Groovy named-parameter syntax:
   * {@code downloadFromS3("s3://my-bucket/path/to/file.vcf.gz")}
   *
   * @param cloudUri The cloud URI of the file to download
   * (e.g., {@code "s3://my-bucket/dir/file.txt"})
   *
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails or is not available
   * @throws InterruptedException if the process is interrupted
   */
  public static Path downloadFromS3(
      final String cloudUri)
      throws IOException, InterruptedException {
    return downloadFromS3(new LinkedHashMap<String, Object>(), cloudUri);
  }

  /**
   * Length of the URI scheme separator.
   */
  private static final int SCHEME_SEPARATOR_LENGTH = 3;

  /**
   * Downloads a single file from a cloud URI to a temporary local directory
   * and returns the local {@link Path}.
   *
   * See {@link #downloadFromS3(String)} for details.
   *
   * @param options Reserved for future use (currently unused)
   * @param cloudUri The cloud URI of the file to download
   * @return A {@link Path} pointing to the downloaded local file
   * @throws IOException if {@code nextflow fs cp} fails or is not available
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

    // Derive a stable local destination mirroring the key path:
    // <tmpdir>/nft-utils-cloud/<key>
    int schemeEnd = cloudUri.indexOf("://");
    String withoutScheme = schemeEnd >= 0
      ? cloudUri.substring(schemeEnd + SCHEME_SEPARATOR_LENGTH)
      : cloudUri;
    int firstSlash = withoutScheme.indexOf('/');
    String relativeKey = firstSlash >= 0
      ? withoutScheme.substring(firstSlash + 1)
      : withoutScheme;

    Path destFile = Paths.get(
      System.getProperty("java.io.tmpdir"),
      "nft-utils-cloud", relativeKey
    );
    Files.createDirectories(destFile.getParent());

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

  /**
   * Download an archive and extract it in the given destination directory.
   * Dispatches to `curlAndUnzip` for ZIP files and to `curlAndUntar` for
   * tar archives based on the `compression` parameter.
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
    // Zip is the only clearly defined archive format.
    // Everything else is assumed to be Tar
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
}
