package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NfCoreUtilsTest {

  @TempDir
  Path dir;

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
}
