package nf_core.nf.test;

import org.yaml.snakeyaml.Yaml;
import java.util.Map;

public class Methods {

  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public static CharSequence removeNextflowVersion(CharSequence softwareVersionsFile) {
    final Map<String, Object> softwareVersionsMap = new Yaml().load(softwareVersionsFile.toString());
    softwareVersionsMap.remove("Workflow", "Nextflow");

    return softwareVersionsMap.toString();
  }

}
