package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class HashUtilsTest {

  @Test
  void shouldHashOneValueAsItsText() {
    // md5 of "abc"
    assertEquals(
      "900150983cd24fb0d6963f7d28e17f72", HashUtils.md5Hex("abc"));
  }

  @Test
  void shouldJoinItemsWithNewlines() {
    // md5 of "a\nb"
    assertEquals(
      "8cdeb44417f3c26826595d5820cf5700",
      HashUtils.listToMD5(List.of("a", "b")));
    assertNotEquals(
      HashUtils.listToMD5(List.of("ab")),
      HashUtils.listToMD5(List.of("a", "b")));
  }
}
