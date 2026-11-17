package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NfCoreUtilsTest {

  @TempDir
  Path dir;

  @Test
  void shouldThrowWhenLinkingMissingLibrary() {
    assertThrows(RuntimeException.class, () -> Methods.nfcoreLink(
      dir.resolve("missing").toString(), dir.toString()));
  }

  @Test
  void shouldThrowWhenLinkingToMissingModulesDir() {
    Methods.nfcoreInitialise(dir.resolve("library").toString());
    assertThrows(RuntimeException.class, () -> Methods.nfcoreLink(
      dir.resolve("library").toString(), dir.resolve("missing").toString()));
  }

  @Test
  void shouldIgnoreMissingLibraryOnDelete() {
    Methods.nfcoreDeleteLibrary(dir.resolve("missing").toString());
    assertFalse(Files.exists(dir.resolve("missing")));
  }
}
