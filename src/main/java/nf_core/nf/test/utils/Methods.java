package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.FileReader;
import java.io.IOException;
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
}
