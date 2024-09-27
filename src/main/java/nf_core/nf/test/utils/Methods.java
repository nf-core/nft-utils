package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

public class Methods {

  // Load YAML and return MAP
  public Map<String, Object> LoadYAML(String yamlFile) {
    Yaml yaml = new Yaml();
    InputStream inputStream = this.getClass()
        .getClassLoader()
        .getResourceAsStream(yamlFile.toString());
    Map<String, Object> mapYaml = yaml.load(inputStream);
    return mapYaml;
  }

  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public Map<String, Object> yaml(CharSequence versionFile)
      throws URISyntaxException, MalformedURLException, IOException {

    final Map<String, Object> softwareVersionsMap = LoadYAML(versionFile.toString());
    softwareVersionsMap.remove("Workflow", "Nextflow");

    return softwareVersionsMap;
  }
}
