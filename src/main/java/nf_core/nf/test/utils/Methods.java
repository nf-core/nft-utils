package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

public class Methods {

  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public static CharSequence removeNextflowVersion(CharSequence softwareVersionsFile)
      throws URISyntaxException, MalformedURLException, IOException {
    final Map<String, Object> softwareVersionsMap = new Yaml().load(softwareVersionsFile.toString());
    softwareVersionsMap.remove("Workflow", "Nextflow");

    return removeNextflowVersion(softwareVersionsMap.toString());
  }
}
