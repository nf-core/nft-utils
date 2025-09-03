package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
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
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public class Methods {

  // Read a Version YAML file and return a Map of Map
  public static Map<String, Map<String, Object>> readYamlFile(String filePath) {
    Yaml yaml = new Yaml();
    try (FileReader reader = new FileReader(filePath)) {
      Map<String, Map<String, Object>> data = yaml.load(reader);
      return data;
    } catch (IOException e) {
      System.err.println("Error reading YAML file: " + e.getMessage());
      return null;
    }
  }

  // Helper method to resolve wildcard patterns to actual file paths
  private static List<String> resolveWildcardPaths(String pathPattern) throws IOException {
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
      throw new IOException("Parent directory does not exist: " + parent);
    }

    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName);

    // Find matching files in the parent directory
    try {
      List<String> matchingFiles = Files.list(parent)
          .filter(Files::isRegularFile)
          .filter(path -> matcher.matches(path.getFileName()))
          .sorted()
          .map(path -> path.toAbsolutePath().toString())
          .collect(Collectors.toList());

      if (matchingFiles.isEmpty()) {
        throw new IOException("No files found matching pattern: " + pathPattern);
      }

      return matchingFiles;
    } catch (IOException e) {
      throw new IOException("Error resolving wildcard pattern " + pathPattern + ": " + e.getMessage());
    }
  }

  // Removed the Nextflow entry from the Workflow entry
  // within the input Version YAML file
  public static Map<String, Map<String, Object>> removeNextflowVersion(CharSequence versionFile) {
    return removeFromYamlMap(versionFile, "Workflow", "Nextflow");
  }

  // Removed the Key2 entry from the Key1 entry
  // within the input Version YAML file
  // If Key2 is null or empty, clears all content from Key1
  // Processes all files matching wildcard patterns and merges results
  public static Map<String, Map<String, Object>> removeFromYamlMap(CharSequence versionFile, String Key1, String Key2) {
    String yamlFilePattern = versionFile.toString();
    Map<String, Map<String, Object>> mergedResult = new TreeMap<>();

    try {
      // Resolve wildcard patterns if present - now returns all matching files
      List<String> yamlFilePaths = resolveWildcardPaths(yamlFilePattern);

      for (String yamlFilePath : yamlFilePaths) {
        Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

        if (yamlData != null) {
          // Process each file's data
          if (yamlData.containsKey(Key1)) {
            if (Key2 == null || Key2.isEmpty()) {
              // Remove the entire Key1 entry
              yamlData.remove(Key1);
            } else {
              // Remove only the specific Key2 from Key1
              yamlData.get(Key1).remove(Key2);
            }
          }

          // Merge the processed data into the result
          for (Map.Entry<String, Map<String, Object>> entry : yamlData.entrySet()) {
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
      System.err.println("Error resolving file path pattern: " + e.getMessage());
      return null;
    }

    return mergedResult;
  }

  // Overloaded method for clearing all content from Key1
  public static Map<String, Map<String, Object>> removeFromYamlMap(CharSequence versionFile, String Key1) {
    return removeFromYamlMap(versionFile, Key1, null);
  }

  // wrapper functions for getAllFilesFromDir with default options
  public static List getAllFilesFromDir(String path) throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  // wrapper functions for getAllFilesFromDir with named options
  public static List getAllFilesFromDir(LinkedHashMap<String, Object> options, String outdir) throws IOException {
    if (outdir == null || outdir.isEmpty()) {
      throw new IllegalArgumentException("The 'outdir' parameter is required.");
    }
    // Check if path exists
    Path dirPath = Paths.get(outdir);
    if (!Files.exists(dirPath)) {
      throw new IllegalArgumentException("The specified path does not exist: " + outdir);
    }

    // Check if it's a directory
    if (!Files.isDirectory(dirPath)) {
      throw new IllegalArgumentException("The specified path is not a directory: " + outdir);
    }

    // Extract optional parameters from the map (use defaults if not provided)
    Boolean includeDir = (Boolean) options.getOrDefault("includeDir", false);
    List<String> ignoreGlobs = (List<String>) options.getOrDefault("ignore", new ArrayList<String>());
    String ignoreFilePath = (String) options.get("ignoreFile");
    Boolean relative = (Boolean) options.getOrDefault("relative", false);
    List<String> includeGlobs = (List<String>) options.getOrDefault("include", Arrays.asList("*", "**/*"));

    List<File> files = getAllFilesFromDir(outdir, includeDir, ignoreGlobs, ignoreFilePath, includeGlobs);

    if (relative) {
      return getRelativePath(files, outdir);
    } else {
      return files;
    }
  }

  // Return all files in a directory and its sub-directories
  // matching or not matching supplied glob
  public static List<File> getAllFilesFromDir(
      String outdir,
      boolean includeDir,
      List<String> ignoreGlobs,
      String ignoreFilePath,
      List<String> includeGlobs) throws IOException {
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
      excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    List<String> allIncludeGlobs = new ArrayList<>();
    if (includeGlobs != null) {
      allIncludeGlobs.addAll(includeGlobs);
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : allIncludeGlobs) {
      includeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isIncluded(file) && !isExcluded(file)) {
              output.add(file.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Exclude output which is the root output folder from nf-test
            if (includeDir && (isIncluded(dir) && !isExcluded(dir)
                && !dir.getFileName().toString().equals("output"))) {
              output.add(dir.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          private boolean isExcluded(Path path) {
            return excludeMatchers.stream()
                .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }

          private boolean isIncluded(Path path) {
            return includeMatchers.stream()
                .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }
        });

    return output.stream().sorted(Comparator.comparing(File::getPath)).collect(Collectors.toList());
  }

  private static List<String> readGlobsFromFile(String filePath) throws IOException {
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

  public static List<String> getRelativePath(List<File> filePaths, String baseDir) {
    Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();

    return filePaths
        .stream()
        .map(filePath -> {
          Path path = Paths.get(filePath.toURI()).toAbsolutePath().normalize();
          return basePath.relativize(path).toString();
        })
        .collect(Collectors.toList());
  }

  public static String listToMD5(ArrayList<Object> input) throws UnsupportedEncodingException {
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
        String hex = Integer.toHexString(0xff & b);
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
   * Creates the modules directory and .nf-core.yml configuration file
   * 
   * @param libDir The directory path to initialise an nf-core library at
   */
  public static void nfcoreInitialise(String libDir) {
    NfCoreUtils.nfcoreInitialise(libDir);
  }

  /**
   * Installs nf-core modules from a list
   * 
   * @param libDir  An nf-core library initialised by nfcoreInitialise()
   * @param modules List of module names (strings) or module maps with keys: name
   *                (required), sha (optional), remote (optional)
   */
  public static void nfcoreInstall(String libDir, List<?> modules) {
    NfCoreUtils.nfcoreInstall(libDir, modules);
  }

  /**
   * Creates a symbolic link from the installed nf-core modules to the base
   * directory
   * 
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreLink(String libDir, String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "link");
  }

  /**
   * Remove all linked modules from a modules directory
   * 
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreUnlink(String libDir, String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "unlink");
  }

  /**
   * Delete the temporary nf-core library
   * 
   * @param libDir The library directory path to delete
   */
  public static void nfcoreDeleteLibrary(String libDir) {
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
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output) {
    return filterNextflowOutput(output, null, true, true);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @param sorted Whether to sort the output lines alphabetically
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, boolean sorted) {
    return filterNextflowOutput(output, null, sorted, true);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting and ANSI code
   * handling.
   *
   * @param output    The stdout or stderr output (String or List) to filter
   * @param sorted    Whether to sort the output lines alphabetically
   * @param stripAnsi Whether to strip ANSI escape codes (colors, formatting)
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, boolean sorted, boolean stripAnsi) {
    return filterNextflowOutput(output, null, sorted, stripAnsi);
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
   * @param stripAnsi          Whether to strip ANSI escape codes (colors,
   *                           formatting)
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, List<String> additionalPatterns, boolean sorted,
      boolean stripAnsi) {
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
    for (String line : outputLines) {
      String filtered = line;

      // Strip ANSI escape codes if requested (colors, formatting, etc.)
      if (stripAnsi) {
        filtered = filtered.replaceAll("\\x1B\\[[0-9;]*[A-Za-z]", "");
      }

      // Remove timestamp patterns (various formats)
      filtered = filtered.replaceAll(
          "\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}[:-]\\d{2}[:-]\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?",
          "[TIMESTAMP]");
      filtered = filtered.replaceAll("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}", "[TIMESTAMP]");

      // Remove Nextflow process execution hashes (format: [xx/yyyyyy])
      filtered = filtered.replaceAll("\\[[0-9a-f]{2}/[0-9a-f]{6}\\]", "[NXF_HASH]");

      // Remove NFT_HASH work dir (format: [xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx])
      filtered = filtered.replaceAll("\\b[0-9a-f]{31,32}\\b", "[NFT_HASH]");

      // Remove Nextflow version update notifications
      filtered = filtered.replaceAll(".*Nextflow\\s+\\d+\\.\\d+\\.\\d+.*is available.*", "");
      filtered = filtered.replaceAll(".*Please consider updating your version.*", "");

      // Replace absolute paths with [PATH] placeholder using a more general approach
      filtered = filterAbsolutePaths(filtered);

      // Remove session and run names
      filtered = filtered.replaceAll("Session id:\\s*[0-9a-f-]+", "Session id: [SESSION_ID]");
      filtered = filtered.replaceAll("Run name:\\s*\\S+", "Run name: [RUN_NAME]");

      // Remove Nextflow run names in square brackets (e.g., [nasty_magritte])
      // Note: This should come after process hash filtering to avoid conflicts
      // More specific pattern to avoid matching process hashes
      filtered = filtered.replaceAll("\\[([a-z]+_[a-z]+)\\]", "[RUN_NAME]");

      // Remove version-specific information that might vary
      filtered = filtered.replaceAll("version\\s+\\d+\\.\\d+\\.\\d+", "version [VERSION]");
      filtered = filtered.replaceAll("build\\s+\\d+", "build [BUILD]");

      // Replace standalone version numbers (e.g., "1.0.0dev", "2.3.5")
      filtered = filtered.replaceAll("\\b\\d+\\.\\d+\\.\\d+[a-zA-Z]*\\b", "[VERSION]");

      // Only add non-empty lines (filter out empty lines)
      if (!filtered.trim().isEmpty()) {
        filteredLines.add(filtered);
      }
    }

    // Apply additional patterns if provided
    if (additionalPatterns != null) {
      List<String> additionallyFilteredLines = new ArrayList<>();
      for (String line : filteredLines) {
        String filtered = line;
        for (String pattern : additionalPatterns) {
          try {
            filtered = filtered.replaceAll(pattern, "[FILTERED]");
          } catch (Exception e) {
            System.err.println("Warning: Invalid regex pattern '" + pattern + "': " + e.getMessage());
          }
        }
        additionallyFilteredLines.add(filtered);
      }
      filteredLines = additionallyFilteredLines;
    }

    // Sort and remove duplicates if requested
    if (sorted) {
      // Separate lines that should be sorted from those that should preserve order
      List<String> sortableLines = new ArrayList<>();
      List<String> preserveOrderLines = new ArrayList<>();

      for (String line : filteredLines) {
        if (line.contains("Staging foreign file") ||
            line.contains("Submitted process") ||
            line.startsWith("Creating env using conda:") ||
            line.startsWith("Pulling Singularity image") ||
            line.startsWith("ERROR ~") ||
            line.startsWith("WARN:") ||
            (line.contains("Check ") && line.contains(" file for details"))) {
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
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, List<String> additionalPatterns) {
    return filterNextflowOutput(output, additionalPatterns, true, true);
  }

  /**
   * Filters Nextflow stdout/stderr output using Groovy's named parameter syntax.
   * This allows calling: filterNextflowOutput(output, sorted: false, stripAnsi:
   * true)
   *
   * @param output  The stdout or stderr output (String or List) to filter
   * @param options Map containing filtering options (automatically created by
   *                Groovy named params):
   *                - additionalPatterns: List<String> of additional regex
   *                patterns (optional)
   *                - sorted: Boolean whether to sort the output (default: true)
   *                - stripAnsi: Boolean whether to strip ANSI codes (default:
   *                true)
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, Map<String, Object> options) {
    if (options == null) {
      options = new HashMap<>();
    }

    // Extract options with defaults
    List<String> additionalPatterns = (List<String>) options.get("additionalPatterns");
    Boolean sorted = (Boolean) options.get("sorted");
    Boolean stripAnsi = (Boolean) options.get("stripAnsi");

    // Apply defaults
    if (sorted == null)
      sorted = true;
    if (stripAnsi == null)
      stripAnsi = true;

    return filterNextflowOutput(output, additionalPatterns, sorted, stripAnsi);
  }

  /**
   * Filters absolute paths in the given text and replaces them with [PATH]
   * placeholder.
   * 
   * @param text The text to filter
   * @return The filtered text with various directory paths replaced with [PATH]
   */
  private static String filterAbsolutePaths(String text) {
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
        "NXF_TEMP",
        "NXF_WORK"
    };

    for (String envVar : envVars) {
      String envValue = System.getenv(envVar);
      if (envValue != null && !envValue.isEmpty()) {
        pathsToReplace.add(envValue);
      }
    }

    // Remove duplicates and sort paths by length (longest first) to avoid partial
    // replacements
    pathsToReplace = pathsToReplace.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(java.util.stream.Collectors.toList());

    // Also extract paths from the text itself (e.g., "NXF_HOME: /path/to/dir")
    // This handles cases where environment variables are printed but not available
    // in our context
    String[] envPrefixes = {
        "HOME:",
        "NFT_WORKDIR:",
        "NXF_CACHE_DIR:",
        "NXF_CONDA_CACHEDIR:",
        "NXF_HOME:",
        "NXF_SINGULARITY_CACHEDIR:",
        "NXF_TEMP:",
        "NXF_WORK:"
    };

    for (String prefix : envPrefixes) {
      if (filtered.contains(prefix)) {
        // Extract the path after the colon
        int start = filtered.indexOf(prefix) + prefix.length();
        int end = filtered.indexOf('\n', start);
        if (end == -1)
          end = filtered.length();

        String extractedPath = filtered.substring(start, end).trim();
        if (!extractedPath.isEmpty() && !extractedPath.equals("null") && extractedPath.startsWith("/")) {
          pathsToReplace.add(extractedPath);
        }
      }
    }

    // Re-sort after adding extracted paths
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
}
