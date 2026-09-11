package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility methods for reading and manipulating YAML files.
 */
public final class YamlUtils {

  /**
   * Prevents instantiation of this utility class.
   */
  private YamlUtils() {
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
    try (BufferedReader reader = Files.newBufferedReader(
        Paths.get(filePath),
        StandardCharsets.UTF_8)) {
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
  static List<String> resolveWildcardPaths(
      final String pathPattern)
      throws IOException {
    if (!pathPattern.contains("*") && !pathPattern.contains("?")) {
      return Arrays.asList(pathPattern);
    }

    Path pattern = Paths.get(pathPattern);
    Path parent = pattern.getParent();
    Path fileNamePath = pattern.getFileName();
    if (fileNamePath == null) {
      throw new IOException(
        "Invalid path pattern: " + pathPattern
      );
    }
    String fileName = fileNamePath.toString();

    if (parent == null) {
      parent = Paths.get(".");
    }

    if (!Files.exists(parent) || !Files.isDirectory(parent)) {
      throw new IOException(
        "Parent directory does not exist: "
        + parent
      );
    }

    PathMatcher matcher = FileSystems
      .getDefault()
      .getPathMatcher("glob:" + fileName);

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
      List<String> yamlFilePaths = resolveWildcardPaths(yamlFilePattern);

      for (String yamlFilePath : yamlFilePaths) {
        Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

        if (yamlData != null) {
          if (yamlData.containsKey(key1)) {
            if (key2 == null || key2.isEmpty()) {
              yamlData.remove(key1);
            } else {
              yamlData.get(key1).remove(key2);
            }
          }

          for (
              Map.Entry<String, Map<String, Object>> entry
              : yamlData.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> value = entry.getValue();

            if (mergedResult.containsKey(key)) {
              mergedResult.get(key).putAll(value);
            } else {
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
}
