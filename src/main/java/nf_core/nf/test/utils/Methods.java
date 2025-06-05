package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

  // Helper method to resolve wildcard patterns to actual file paths
  private static String resolveWildcardPath(String pathPattern) throws IOException {
    // If no wildcard, return as is
    if (!pathPattern.contains("*") && !pathPattern.contains("?")) {
      return pathPattern;
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
      List<Path> matchingFiles = Files.list(parent)
          .filter(Files::isRegularFile)
          .filter(path -> matcher.matches(path.getFileName()))
          .sorted()
          .collect(Collectors.toList());

      if (matchingFiles.isEmpty()) {
        throw new IOException("No files found matching pattern: " + pathPattern);
      }

      // Return the first matching file (absolute path)
      return matchingFiles.get(0).toAbsolutePath().toString();
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
  public static Map<String, Map<String, Object>> removeFromYamlMap(CharSequence versionFile, String Key1, String Key2) {
    String yamlFilePath = versionFile.toString();

    try {
      // Resolve wildcard patterns if present
      yamlFilePath = resolveWildcardPath(yamlFilePath);
    } catch (IOException e) {
      System.err.println("Error resolving file path pattern: " + e.getMessage());
      return null;
    }

    Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

    if (yamlData != null) {
      // Access and use the YAML data
      if (yamlData.containsKey(Key1)) {
        if (Key2 == null || Key2.isEmpty()) {
          // Remove the entire Key1 entry
          yamlData.remove(Key1);
        } else {
          // Remove only the specific Key2 from Key1
          yamlData.get(Key1).remove(Key2);
        }
      }
    }
    return yamlData;
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
  public static List<File> getAllFilesFromDir(String outdir, boolean includeDir, List<String> ignoreGlobs,
      String ignoreFilePath, List<String> includeGlobs)
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

    List<String> allIncludeGlobs = new ArrayList<>();
    if (includeGlobs != null) {
      allIncludeGlobs.addAll(includeGlobs);
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : allIncludeGlobs) {
      includeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
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
        if (includeDir && (isIncluded(dir) && !isExcluded(dir) && !dir.getFileName().toString().equals("output"))) {
          output.add(dir.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      private boolean isExcluded(Path path) {
        return excludeMatchers.stream().anyMatch(matcher -> matcher.matches(directory.relativize(path)));
      }

      private boolean isIncluded(Path path) {
        return includeMatchers.stream().anyMatch(matcher -> matcher.matches(directory.relativize(path)));
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
}
