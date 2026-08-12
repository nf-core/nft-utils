package nf_core.nf.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void shouldGetFileExtension() {
    Path path = Paths.get("/tmp/example.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldConvertExtensionToLowerCase() {
    Path path = Paths.get("/tmp/example.BAM");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldGetExtensionFromFileWithMultipleDots() {
    Path path = Paths.get("/tmp/sample.test.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringWhenFileHasNoExtension() {
    Path path = Paths.get("/tmp/example");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringForHiddenFile() {
    Path path = Paths.get("/tmp/.example");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldReturnEmptyStringWhenFilenameEndsWithDot() {
    Path path = Paths.get("/tmp/example.");
    assertEquals("", Utils.getExtension(path));
  }

  @Test
  void shouldIgnoreDotsInParentDirectories() {
    Path path = Paths.get("/tmp/some.directory/example.bam");
    assertEquals("bam", Utils.getExtension(path));
  }

  @Test
  void shouldIgnoreGzExtension() {
    Path path = Paths.get("/tmp/some.directory/example.vcf.gz");
    assertEquals("vcf", Utils.getExtension(path, false));
    assertEquals("gz", Utils.getExtension(path, true));
  }

  @Test
  void shouldIgnoreNestedGzExtension() {
    Path path = Paths.get("/tmp/some.directory/example.vcf.gz.gz");
    assertEquals("vcf", Utils.getExtension(path, false));
    assertEquals("gz", Utils.getExtension(path, true));
  }

  @Test
  void downloadFile() throws IOException {
    Path destinationFolder = Files.createTempDirectory(
      "nft-utils-test-"
    );

    Path result = Utils.downloadFile(
      "https://raw.githubusercontent.com/nf-core/test-datasets/modules/data/genomics/homo_sapiens/genome/genome.fasta",
      destinationFolder
    );

    assertTrue(Files.exists(result));
    assertEquals(
      "genome.fasta",
      result.getFileName().toString()
    );
  }

  @Test
  void downloadFileFail() throws IOException {
    Path destinationFolder = Files.createTempDirectory(
      "nft-utils-test-"
    );

    RuntimeException exception = assertThrows(
      RuntimeException.class,
      () -> Utils.downloadFile(
        "https://raw.githubusercontent.com/nf-core/test-datasets/modules/data/genomics/homo_sapiens/genome/genome.fastaX",
        destinationFolder
      )
    );

    assertTrue(
      exception.getMessage().contains("Failed to download file")
    );
  }
}
