package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NextflowOutputFilterTest {

  @Test
  void shouldRemoveAdditionalPatternMatches() {
    List<String> result = Methods.filterNextflowOutput(
      "keep me\nsecret 1234 here\n.*drop line.*",
      List.of("\\d{4} ", ".*drop line.*"));
    assertEquals(List.of("keep me", "secret here"), result);
  }

  @Test
  void shouldSkipInvalidAdditionalPattern() {
    List<String> result = Methods.filterNextflowOutput(
      "keep me", List.of("("));
    assertEquals(List.of("keep me"), result);
  }

  @Test
  void shouldReplaceWorkDirHash() {
    List<String> result = Methods.filterNextflowOutput(
      "file(/data/work/7b/b7421f705229baeb721b87806b97ed/test_1.fastq.gz)");
    assertEquals(List.of("file(/data/work/[NFT_HASH]/test_1.fastq.gz)"), result);
  }

  @Test
  void shouldDropUnstableLines() {
    List<String> result = Methods.filterNextflowOutput(
      "Nextflow 25.10.0 is available\n"
      + "Please consider updating your version\n"
      + "  containerEngine : docker\n"
      + "Downloading plugin nf-schema@2.5.1\n"
      + "WARN: Singularity cache directory has not been defined -- "
      + "Remote image will be stored in the path: /data/work/singularity\n"
      + "WARN: Apptainer cache directory has not been defined -- "
      + "Remote image will be stored in the path: /data/work/apptainer\n"
      + "keep me");
    assertEquals(List.of("keep me"), result);
  }

  @Test
  void shouldAcceptOptionsMapAfterOutput() {
    List<String> result = Methods.filterNextflowOutput(
      "b line\na line", Map.<String, Object>of("ignore", List.of("b ")));
    assertEquals(List.of("a line"), result);
  }

  @Test
  void filterLinePatternDoesNotDropLines() {
    assertEquals("Downloading plugin nf-schema@2.5.1",
      Methods.filterLinePattern("Downloading plugin nf-schema@2.5.1", null));
  }

  @Test
  void shouldMatchIgnoreAgainstLineWithoutAnsi() {
    List<String> result = Methods.filterNextflowOutput(
      "\u001B[1mb\u001B[0m line\na line",
      Map.<String, Object>of("ignore", List.of("b line")));
    assertEquals(List.of("a line"), result);
  }
}
