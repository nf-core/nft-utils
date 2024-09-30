package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
  // matching or not matching supplied regexes
  public static List<File> getAllFilesFromDir(String outdir, boolean includeDir, List<String> excludeRegexes) {
    List<File> output = new ArrayList<>();
    File directory = new File(outdir);

    getAllFilesRecursively(directory, includeDir, excludeRegexes, output);

    Collections.sort(output);
    return output;
  }

  // Recursively list all files in a directory and its sub-directories
  // matching or not matching supplied regexes
  private static void getAllFilesRecursively(File directory, boolean includeDir, List<String> excludeRegexes,
      List<File> output) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        boolean matchesInclusion = includeDir || file.isFile();
        boolean matchesExclusion = false;

        if (excludeRegexes != null) {
          for (String regex : excludeRegexes) {
            if (Pattern.matches(regex, file.getName())) {
              matchesExclusion = true;
              break;
            }
          }
        }

        if (matchesInclusion && !matchesExclusion) {
          output.add(file);
        }

        if (file.isDirectory()) {
          getAllFilesRecursively(file, includeDir, excludeRegexes, output);
        }
      }
    }
  }
}
