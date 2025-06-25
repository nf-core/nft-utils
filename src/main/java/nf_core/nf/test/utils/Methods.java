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
import java.util.List;
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
    List<String> includeGlobs
  ) throws IOException {
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
      }
    );

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
   * @param libDir The directory path to initialise an nf-core library at
   */
  public static void nfcoreSetup(String libDir) {
    System.out.println("\n");
    System.out.println("Creating a temporary nf-core library at " + libDir);
    try {
      // Create modules directory
      File modulesDir = new File(libDir + "/modules");
      modulesDir.mkdirs();

      // Create .nf-core.yml file
      File nfcoreYml = new File(libDir + "/.nf-core.yml");
      try (FileWriter writer = new FileWriter(nfcoreYml)) {
        writer.write("repository_type: \"pipeline\"\n");
        writer.write("template:\n");
        writer.write("    name: test\n");
      }
    } catch (IOException e) {
      System.err.println("Error setting up nf-core: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Installs nf-core modules from a list
   * @param libDir An nf-core library initialised by nfcoreSetup()
   * @param modules List of module names to install (e.g., ["minimap2/index", "samtools/view"])
   */
  public static void nfcoreInstall(String libDir, List<String> modules) {
    System.out.println("Installing nf-core modules...");
    for (String module : modules) {
      try {
        ProcessBuilder processBuilder = new ProcessBuilder(
          "bash",
          "-c",
          "cd " + libDir + " && nf-core --verbose modules install " + module
        );
        Process process = processBuilder.start();

        // Capture stderr from nf-core tools
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder stderr = new StringBuilder();
        String line;
        while ((line = stderrReader.readLine()) != null) {
          stderr.append(line).append("\n");
        }
        int exitCode = process.waitFor();

        // Spit out nf-core tools stderr if install fails
        if (exitCode != 0) {
          System.err.println("Error installing module " + module + ": exit code " + exitCode + "\n");
          System.err.println("nf-core tools output: \n");
          System.err.println(stderr.toString());
        } else {
          System.out.println("Successfully installed module: " + module);
        }
      } catch (IOException | InterruptedException e) {
        System.err.println("Error installing module " + module + ": " + e.getMessage());
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Creates a symbolic link from the installed nf-core modules to the base directory
   * @param libDir An nf-core library initialised by nfcoreSetup()
   * @param destPath Location to make the library available at
   */
  public static void nfcoreLink(String libDir, String destPath) {
    try {
      File sourceDir = new File(libDir + "/modules/nf-core");
      File destination = new File(destPath);

      Files.createSymbolicLink(destination.toPath(), sourceDir.toPath());
      System.out.println("Linking temporary nf-core library: " + destPath + " -> " + sourceDir);
    } catch (FileAlreadyExistsException e) {
      System.out.println("Error: Symlink already exists: " + destPath);
    } catch (IOException e) {
      System.err.println("Error creating symlink: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Delete the temorary nf-core library
   * @param libDir The launch directory path
   * @param modulesDir The base directory path
   */
  public static void nfcoreDeleteLibrary(String libDir) {
    System.out.println("Deleting temporary nf-core library: " + libDir);

    try {
      // Delete modules directory
      File modulesLibDir = new File(libDir);
      deleteDirectory(modulesLibDir);
    } catch (Exception e) {
      System.err.println("Error during cleanup: " + e.getMessage());
    }
  }

  /**
   * Cleanup function to remove created directories and files
   * @param linkPath Symlinked location
   */
  public static void nfcoreUnlink(String linkPath) {
    System.out.println("Unlinking temporary nf-core library: " + linkPath);

    try {
      File nfcoreLink = new File(linkPath);
      nfcoreLink.delete();
    } catch (Exception e) {
      System.err.println("Error during cleanup: " + e.getMessage());
    }
  }

  /**
   * Helper method to recursively delete a directory
   * @param directory The directory to delete
   */
  private static void deleteDirectory(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
      directory.delete();
    }
  }
}
