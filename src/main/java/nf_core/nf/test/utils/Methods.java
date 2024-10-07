package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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

  // Removed the Nextflow entry from the Workflow entry
  // within the input Version YAML file
  public static Map<String, Map<String, Object>> removeNextflowVersion(CharSequence versionFile) {
    String yamlFilePath = versionFile.toString();
    Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

    if (yamlData != null) {
      // Access and use the YAML data
      if (yamlData.containsKey("Workflow")) {
        yamlData.get("Workflow").remove("Nextflow");
      }
    }
    return yamlData;
  }

  // Removed the Key2 entry from the Key1 entry
  // within the input Version YAML file
  public static Map<String, Map<String, Object>> removeFromYaml(CharSequence versionFile, String Key1, String Key2) {
    String yamlFilePath = versionFile.toString();
    Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

    if (yamlData != null) {
      // Access and use the YAML data
      if (yamlData.containsKey(Key1)) {
        yamlData.get(Key1).remove(Key2);
      }
    }
    return yamlData;
  }

  // wrapper functions for getAllFilesFromDir with default options
  public static List getAllFilesFromDir(String path) throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  // wrapper functions for getAllFilesFromDir with named options
  public static List getAllFilesFromDir(LinkedHashMap<String, Object> options, String path) throws IOException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("The 'path' parameter is required.");
    }
    // TODO: check if path exists

    // Extract optional parameters from the map (use defaults if not provided)
    Boolean includeDir = (Boolean) options.getOrDefault("includeDir", true);
    List<String> ignoreGlobs = (List<String>) options.getOrDefault("ignore", new ArrayList<String>());
    String ignoreFilePath = (String) options.get("ignoreFile");
    Boolean relative = (Boolean) options.getOrDefault("relative", false);

    List<File> files = getAllFilesFromDir(path, includeDir, ignoreGlobs, ignoreFilePath);

    if (relative) {
      return getRelativePath(files, path);
    } else {
      return files;
    }
  }

  // Return all files in a directory and its sub-directories
  // matching or not matching supplied glob
  public static List<File> getAllFilesFromDir(String outdir, boolean includeDir, List<String> ignoreGlobs,
      String ignoreFilePath)
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
      excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (!isExcluded(file)) {
          output.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        // Exclude output which is the root output folder from nf-test
        if (includeDir && (!isExcluded(dir) && !dir.getFileName().toString().equals("output"))) {
          output.add(dir.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      private boolean isExcluded(Path path) {
        return excludeMatchers.stream().anyMatch(matcher -> matcher.matches(directory.relativize(path)));
      }
    });

    return output.stream()
        .sorted(Comparator.comparing(File::getPath))
        .collect(Collectors.toList());
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

    return filePaths.stream()
        .map(filePath -> {
          Path path = Paths.get(filePath.toURI()).toAbsolutePath().normalize();
          return basePath.relativize(path).toString();
        })
        .collect(Collectors.toList());
  }
}
