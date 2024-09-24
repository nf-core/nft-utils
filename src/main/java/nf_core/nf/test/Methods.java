package nf_core.nf.test.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Methods {

  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public static removeNextflowVersion(Path softwareVersions) {
    final Yaml softwareVersionsYaml = path(softwareVersions);
    if (softwareVersionsYaml.containsKey("Workflow")) {
      softwareVersionsYaml.Workflow.remove("Nextflow");
    }
    return removeNextflowVersion(softwareVersionsYaml);
  }

}
