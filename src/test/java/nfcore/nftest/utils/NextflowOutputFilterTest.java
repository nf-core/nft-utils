package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

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
  void shouldRejectInvalidAdditionalPattern() {
    assertThrows(PatternSyntaxException.class,
      () -> Methods.filterNextflowOutput("keep me", List.of("(")));
  }

  @Test
  void shouldKeepRepeatedLinesButNotRepeatedSortedLines() {
    List<String> result = Methods.filterNextflowOutput(
      "Done\nDone\nWARN: b\nWARN: a\nWARN: b");
    assertEquals(List.of("Done", "Done", "WARN: a", "WARN: b"), result);
  }

  @Test
  void shouldAcceptNullLists() {
    assertEquals(List.of("keep me"), Methods.filterNextflowOutput(
      "keep me", null, true, false, null, null));
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
  void shouldMatchIgnoreAgainstLineWithoutAnsi() {
    List<String> result = Methods.filterNextflowOutput(
      "\u001B[1mb\u001B[0m line\na line",
      Map.<String, Object>of("ignore", List.of("b line")));
    assertEquals(List.of("a line"), result);
  }

  @Test
  void shouldReplaceRunNameOfIgnoredLaunchingLine() {
    List<String> result = Methods.filterNextflowOutput(
      "Launching `main.nf` [goofy_salas] - revision: 83be6aadf5\n"
      + "Run goofy_salas done",
      Map.<String, Object>of("ignore", List.of("Launching")));
    assertEquals(List.of("Run [RUN_NAME] done"), result);
  }

  @Test
  void shouldReadRunNameAndIgnoreWithoutAnsiWhenKeepingAnsi() {
    List<String> result = Methods.filterNextflowOutput(
      "Launching `main.nf` [\u001B[1mgoofy_salas\u001B[0m]\n"
      + "\u001B[1mb\u001B[0m line\n"
      + "Run goofy_salas done",
      Map.<String, Object>of("keepAnsi", true, "ignore", List.of("b line")));
    assertEquals(
      List.of(
        "Launching `main.nf` [\u001B[1m[RUN_NAME]\u001B[0m]",
        "Run [RUN_NAME] done"),
      result);
  }
}
