package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Methods {

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

  // Recursively list all files in a directory and its sub-directories, matching
  // or not matching supplied regexes
  public static getAllFilesFromDir(dir, List<String> includeRegexes = null, List<String> excludeRegexes = null) {
        def output = []
        new File(dir).eachFileRecurse() { file ->
            boolean matchesInclusion = (includeRegexes == null || includeRegexes.any { regex -> file.name.toString() ==~ regex })
            boolean matchesExclusion = (excludeRegexes == null || !excludeRegexes.any { regex -> file.name.toString() ==~ regex })

            if (matchesInclusion && matchesExclusion) {
                output.add(file)
            }
        }return output.sort

  { it.name }}

  // Static (global) things useful for supplying to getAllFilesFromDir()
  static List<String> unstableFilenamesRegex = [/.*\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}.*/]  // e.g. date strings
  static List<String> plainTextFormatsRegex = [/.*\.(txt|json|tsv)$/]                       // Common plain text formats
}
