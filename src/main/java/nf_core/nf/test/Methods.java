package nf_core.nf.test;

import org.yaml.snakeyaml.Yaml;
import java.util.Map;

public class Methods {

  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public static String removeNextflowVersion(String softwareVersionsFile) {
    final Map<String, Object> softwareVersionsMap = new Yaml().load(softwareVersionsFile);
    softwareVersionsMap.remove("Workflow", "Nextflow");

    return softwareVersionsMap.toString();
  }

}
