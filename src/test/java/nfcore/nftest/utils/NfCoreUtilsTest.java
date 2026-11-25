package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NfCoreUtilsTest {

  @TempDir
  Path dir;

  /** Creates a library with an nf-core module, and returns its path. */
  private Path library(final String name, final String module)
      throws IOException {
    Path lib = dir.resolve(name);
    Files.createDirectories(lib.resolve("modules/nf-core/" + module));
    Files.writeString(lib.resolve("modules/nf-core/" + module + "/main.nf"), "");
    return lib;
  }

  @Test
  void shouldThrowWhenLinkingMissingLibrary() {
    assertThrows(IOException.class, () -> Methods.nfcoreLink(
      dir.resolve("missing").toString(), dir.toString()));
  }

  @Test
  void shouldThrowWhenLinkingToMissingModulesDir() throws IOException {
    Methods.nfcoreInitialise(dir.resolve("library").toString());
    assertThrows(IOException.class, () -> Methods.nfcoreLink(
      dir.resolve("library").toString(), dir.resolve("missing").toString()));
  }

  @Test
  void shouldIgnoreMissingLibraryOnDelete() {
    assertDoesNotThrow(() -> Methods.nfcoreDeleteLibrary(
      dir.resolve("missing").toString()));
  }

  @Test
  void shouldLinkLibraryGivenAsRelativePath() throws IOException {
    // Under the working directory, so the relative path does not climb to /
    Path base = Files.createTempDirectory(
      Files.createDirectories(Path.of("target")), "nfcore");
    try {
      Path lib = base.resolve("lib/modules/nf-core/fastqc");
      Files.createDirectories(lib);
      Path mods = Files.createDirectories(base.resolve("mods"));

      Methods.nfcoreLink(base.resolve("lib").toString(), mods.toString());

      assertTrue(Files.isDirectory(mods.resolve("nf-core/fastqc")));
    } finally {
      Methods.nfcoreDeleteLibrary(base.toString());
    }
  }

  @Test
  void shouldLinkInsideExistingOrgDirAndUnlink() throws IOException {
    Path lib = library("lib", "fastqc");
    Path mods = dir.resolve("mods");
    Files.createDirectories(mods.resolve("nf-core/local"));

    Methods.nfcoreLink(lib.toString(), mods.toString());
    assertTrue(Files.isSymbolicLink(mods.resolve("nf-core/fastqc")));

    Methods.nfcoreUnlink(lib.toString(), mods.toString());
    assertFalse(Files.exists(mods.resolve("nf-core/fastqc")));
    assertTrue(Files.isDirectory(mods.resolve("nf-core/local")));
  }

  @Test
  void shouldNotWriteIntoAnotherLinkedLibrary() throws IOException {
    Path lib1 = library("lib1", "fastqc");
    Path lib2 = library("lib2", "multiqc");
    Path mods = Files.createDirectories(dir.resolve("mods"));
    Methods.nfcoreLink(lib1.toString(), mods.toString());

    assertDoesNotThrow(
      () -> Methods.nfcoreLink(lib1.toString(), mods.toString()));
    // The same library, spelled another way
    assertDoesNotThrow(() -> Methods.nfcoreLink(
      lib1.resolve("../lib1").toString(), mods.toString()));
    assertThrows(IOException.class,
      () -> Methods.nfcoreLink(lib2.toString(), mods.toString()));
    try (var modules = Files.list(lib1.resolve("modules/nf-core"))) {
      assertEquals(1, modules.count());
    }
  }

  @Test
  void shouldUnlinkRelativeLinksIntoTheLibrary() throws IOException {
    Path lib = library("lib", "fastqc");
    Path org = Files.createDirectories(dir.resolve("mods/nf-core"));
    Files.createSymbolicLink(
      org.resolve("fastqc"), Path.of("../../lib/modules/nf-core/fastqc"));

    Methods.nfcoreUnlink(lib.toString(), dir.resolve("mods").toString());

    assertFalse(Files.exists(org.resolve("fastqc"), LinkOption.NOFOLLOW_LINKS));
  }
}
