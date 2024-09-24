package nf_core.nf.test.utils;

public class Methods {
  // Remove Nextflow version from pipeline_software_mqc_versions.yml
  public static Object removeNextflowVersion(file pipeline_software_mqc_versions) {
    def softwareVersions = path(pipeline_software_mqc_versions).yaml;
    if (softwareVersions.containsKey("Workflow")) {
      softwareVersions.Workflow.remove("Nextflow");
    }
    return softwareVersions;
  }

}
