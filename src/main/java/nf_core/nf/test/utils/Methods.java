package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  // Return all files in a directory and its sub-directories
  // matching or not matching supplied glob
  public static List<File> getAllFilesFromDir(String outdir, boolean includeDir, List<String> ignoreGlobs,
      String ignoreGlobFile) throws IOException {
    List<File> output = new ArrayList<>();
    Path directory = Paths.get(outdir).toAbsolutePath();

    List<PathMatcher> excludeMatchers = getExcludeMatchers(ignoreGlobs, ignoreGlobFile);

    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (dir.getFileName().toString().equals("output")) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        if (includeDir && !isExcluded(dir)) {
          output.add(dir.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!isExcluded(file)) {
          output.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      private boolean isExcluded(Path path) {
        return excludeMatchers.stream().anyMatch(matcher -> matcher.matches(path));
      }
    });

    return output.stream()
        .sorted(Comparator.comparing(File::getPath))
        .collect(Collectors.toList());
  }

  private static List<PathMatcher> getExcludeMatchers(List<String> ignoreGlobs, String ignoreGlobFile)
      throws IOException {
    List<String> allIgnoreGlobs = new ArrayList<>();

    // Add globs from the list
    if (ignoreGlobs != null) {
      allIgnoreGlobs.addAll(ignoreGlobs);
    }

    // Add globs from the file
    if (ignoreGlobFile != null && !ignoreGlobFile.isEmpty()) {
      Path path = Paths.get(ignoreGlobFile);
      if (Files.exists(path) && Files.isRegularFile(path)) {
        allIgnoreGlobs.addAll(Files.readAllLines(path));
      } else {
        throw new IllegalArgumentException(
            "Specified exclude glob file does not exist or is not a regular file: " + ignoreGlobFile);
      }
    }

    return allIgnoreGlobs.stream()
        .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob))
        .collect(Collectors.toList());
  }
}
